package com.d102.crescendo.global.service;

import com.d102.crescendo.domain.ai.service.AiService;
import com.d102.crescendo.domain.sheet.dto.request.UserSheetCreateRequest;
import com.d102.crescendo.domain.sheet.dto.response.UserSheetCreateResponse;
import com.d102.crescendo.domain.sheet.service.SheetRegistrationService;
import com.d102.crescendo.global.dto.response.SeedDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeedDataService {

    private final S3Service s3Service;
    private final SheetRegistrationService sheetRegistrationService;
    private final AiService aiService;

    /**
     * resources/seed-data/sheets/ 폴더의 모든 XML 파일을 읽어서 피아노/기타 파트별로 분리하여 S3에 업로드하고 DB에 등록
     */
    public SeedDataResponse seedUserSheets(Integer userId, Integer genreId) {
        List<SeedDataResponse.SheetResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try {
            // 리소스 폴더에서 모든 MusicXML 파일 찾기 (.xml, .musicxml, .mxl)
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            List<Resource> allResources = new ArrayList<>();

            // 각 확장자별로 검색하고 모두 모으기
            try {
                Resource[] xmlFiles = resolver.getResources("classpath:seed-data/sheets/*.xml");
                allResources.addAll(List.of(xmlFiles));
                log.info(".xml 파일 발견: {} 개", xmlFiles.length);
            } catch (Exception e) {
                log.debug(".xml 파일 검색 중 예외: {}", e.getMessage());
            }

            try {
                Resource[] musicxmlFiles = resolver.getResources("classpath:seed-data/sheets/*.musicxml");
                allResources.addAll(List.of(musicxmlFiles));
                log.info(".musicxml 파일 발견: {} 개", musicxmlFiles.length);
            } catch (Exception e) {
                log.debug(".musicxml 파일 검색 중 예외: {}", e.getMessage());
            }

            try {
                Resource[] mxlFiles = resolver.getResources("classpath:seed-data/sheets/*.mxl");
                allResources.addAll(List.of(mxlFiles));
                log.info(".mxl 파일 발견: {} 개", mxlFiles.length);
            } catch (Exception e) {
                log.debug(".mxl 파일 검색 중 예외: {}", e.getMessage());
            }

            if (allResources.isEmpty()) {
                log.warn("seed-data/sheets/ 폴더에 MusicXML 파일(.xml, .musicxml, .mxl)이 없습니다.");
                return SeedDataResponse.builder()
                        .totalFiles(0)
                        .successCount(0)
                        .failCount(0)
                        .results(results)
                        .build();
            }

            log.info("총 발견된 MusicXML 파일 개수: {}", allResources.size());

            Resource[] resources = allResources.toArray(new Resource[0]);

            // 각 파일을 처리
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                log.info("처리 중: {}", fileName);

                try {
                    // 1. 메타데이터 추출 (title, composer)
                    MusicXMLMetadata metadata;
                    try (InputStream metadataInputStream = resource.getInputStream()) {
                        metadata = extractMetadataFromMusicXML(metadataInputStream, fileName);
                    }
                    log.info("메타데이터 추출 완료 - 제목: {}, 작곡가: {}", metadata.getTitle(), metadata.getComposer());

                    // 2. Document 파싱 (악기 정보 추출용)
                    Document doc;
                    try (InputStream docInputStream = resource.getInputStream()) {
                        doc = parseDocument(docInputStream, fileName);
                    }

                    // 3. title이나 composer가 없으면 스킵
                    if (metadata.getTitle() == null || metadata.getTitle().isEmpty() ||
                        metadata.getComposer() == null || metadata.getComposer().isEmpty()) {
                        log.warn("파일에 title 또는 composer 정보가 없습니다: {} (title: {}, composer: {})",
                                fileName, metadata.getTitle(), metadata.getComposer());
                        results.add(SeedDataResponse.SheetResult.builder()
                                .fileName(fileName)
                                .success(false)
                                .message("title 또는 composer 정보가 없습니다")
                                .build());
                        failCount++;
                        continue;
                    }

                    // 4. 피아노와 기타 파트 찾기
                    List<PartInfo> parts = findPianoAndGuitarParts(doc);

                    if (parts.isEmpty()) {
                        log.warn("파일에 피아노 또는 기타 파트가 없습니다: {}", fileName);
                        results.add(SeedDataResponse.SheetResult.builder()
                                .fileName(fileName)
                                .success(false)
                                .message("피아노 또는 기타 파트가 없습니다")
                                .build());
                        failCount++;
                        continue;
                    }

                    // 5. 각 파트별로 분리하여 업로드 및 등록
                    for (PartInfo part : parts) {
                        try {
                            // 해당 파트만 포함하는 Document 생성
                            Document filteredDoc = createFilteredDocument(doc, part.getId());

                            // Document를 InputStream으로 변환
                            InputStream filteredInputStream = documentToInputStream(filteredDoc);
                            byte[] xmlBytes = filteredInputStream.readAllBytes();

                            // S3에 업로드
                            String baseFileName = fileName.replaceAll("\\.(xml|musicxml|mxl)$", "");
                            String newFileName = baseFileName + "-" + part.getInstrumentName() + ".xml";
                            String s3Key = "sheets/" + UUID.randomUUID() + "-" + newFileName;

                            InputStream uploadInputStream = new ByteArrayInputStream(xmlBytes);
                            String xmlUrl = s3Service.uploadFile(s3Key, uploadInputStream, xmlBytes.length, "application/xml");
                            log.info("S3 업로드 완료: {} -> {}", newFileName, xmlUrl);

                            // DB에 등록
                            UserSheetCreateRequest request = new UserSheetCreateRequest();
                            request.setTitle(metadata.getTitle());
                            request.setComposer(metadata.getComposer());
                            request.setXmlUrl(xmlUrl);
                            request.setInstrumentId(part.getInstrumentId());
                            request.setGenreId(genreId);

                            UserSheetCreateResponse response = sheetRegistrationService.registerUserSheet(userId, request);

                            // 시드 데이터 임베딩 생성 (비동기) - registerUserSheet 내부에서 이미 호출됨
                            log.info("시드 데이터 임베딩 요청은 registerUserSheet 내부에서 처리됨: sheetId={}", response.getSheetId());

                            // 성공
                            results.add(SeedDataResponse.SheetResult.builder()
                                    .fileName(newFileName)
                                    .success(true)
                                    .message("등록 완료 (제목: " + metadata.getTitle() + ", 악기: " + part.getInstrumentName() + ")")
                                    .sheetId(response.getSheetId())
                                    .userSheetId(response.getUserSheetId())
                                    .build());
                            successCount++;

                            log.info("악보 등록 완료: {} (sheetId: {}, userSheetId: {}, 제목: {}, 악기: {})",
                                    newFileName, response.getSheetId(), response.getUserSheetId(),
                                    metadata.getTitle(), part.getInstrumentName());

                        } catch (Exception e) {
                            log.error("파트 등록 실패: {} - {}", fileName, part.getInstrumentName(), e);
                            results.add(SeedDataResponse.SheetResult.builder()
                                    .fileName(fileName + " (" + part.getInstrumentName() + ")")
                                    .success(false)
                                    .message("실패: " + e.getMessage())
                                    .build());
                            failCount++;
                        }
                    }

                } catch (Exception e) {
                    // 실패
                    log.error("악보 처리 실패: {}", fileName, e);
                    results.add(SeedDataResponse.SheetResult.builder()
                            .fileName(fileName)
                            .success(false)
                            .message("실패: " + e.getMessage())
                            .build());
                    failCount++;
                }
            }

        } catch (Exception e) {
            log.error("시드 데이터 처리 중 예외 발생", e);
            throw new RuntimeException("시드 데이터 처리 실패: " + e.getMessage());
        }

        return SeedDataResponse.builder()
                .totalFiles(results.size())
                .successCount(successCount)
                .failCount(failCount)
                .results(results)
                .build();
    }

    /**
     * 파일명에서 제목 추출 (확장자 제거, 하이픈/언더스코어를 공백으로)
     * 예: "river_flows_in_you.xml" -> "river flows in you"
     *     "canon-in-d.mxl" -> "canon in d"
     */
    private String extractTitleFromFilename(String fileName) {
        String titleWithoutExt = fileName.replaceAll("\\.(xml|musicxml|mxl)$", "");
        return titleWithoutExt.replace("_", " ").replace("-", " ");
    }

    /**
     * MusicXML 파일에서 메타데이터 추출 (title, composer)
     * MXL 파일인 경우 압축 해제 후 파싱
     */
    private MusicXMLMetadata extractMetadataFromMusicXML(InputStream inputStream, String fileName) throws Exception {
        InputStream xmlStream = inputStream;

        // MXL 파일인 경우 압축 해제
        if (fileName.toLowerCase().endsWith(".mxl")) {
            xmlStream = extractXmlFromMxl(inputStream);
        }

        // XML 파싱하여 메타데이터 추출
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlStream);
        doc.getDocumentElement().normalize();

        String title = null;
        String composer = null;

        // credit 태그에서 title과 composer 찾기
        NodeList creditList = doc.getElementsByTagName("credit");
        for (int i = 0; i < creditList.getLength(); i++) {
            Element creditElement = (Element) creditList.item(i);

            // credit-type 찾기
            NodeList creditTypeList = creditElement.getElementsByTagName("credit-type");
            if (creditTypeList.getLength() > 0) {
                String creditType = creditTypeList.item(0).getTextContent().trim();

                // credit-words 찾기
                NodeList creditWordsList = creditElement.getElementsByTagName("credit-words");
                if (creditWordsList.getLength() > 0) {
                    String creditWords = creditWordsList.item(0).getTextContent().trim();

                    // title인 경우
                    if ("title".equalsIgnoreCase(creditType)) {
                        // 의미 없는 제목("#", "Untitled" 등) 제외
                        if (!creditWords.equals("#") &&
                            !creditWords.equalsIgnoreCase("untitled") &&
                            !creditWords.isEmpty() &&
                            creditWords.length() > 1) {
                            // 이미 title이 있으면 더 긴 것을 선택 (더 구체적인 제목일 가능성)
                            if (title == null || creditWords.length() > title.length()) {
                                title = creditWords;
                            }
                        }
                    }
                    // composer인 경우
                    else if ("composer".equalsIgnoreCase(creditType)) {
                        composer = creditWords;
                    }
                }
            }
        }

        // credit에서 못 찾았으면 work-title 찾기 (fallback 1)
        if (title == null || title.isEmpty()) {
            NodeList workTitleList = doc.getElementsByTagName("work-title");
            if (workTitleList.getLength() > 0) {
                title = workTitleList.item(0).getTextContent().trim();
            }
        }

        // work-title도 없으면 movement-title 찾기 (fallback 2)
        if (title == null || title.isEmpty()) {
            NodeList movementTitleList = doc.getElementsByTagName("movement-title");
            if (movementTitleList.getLength() > 0) {
                title = movementTitleList.item(0).getTextContent().trim();
            }
        }

        // credit에서 composer 못 찾았으면 creator 태그에서 찾기 (fallback)
        if (composer == null || composer.isEmpty()) {
            NodeList creatorList = doc.getElementsByTagName("creator");
            for (int i = 0; i < creatorList.getLength(); i++) {
                Element creatorElement = (Element) creatorList.item(i);
                String type = creatorElement.getAttribute("type");
                if ("composer".equalsIgnoreCase(type)) {
                    composer = creatorElement.getTextContent().trim();
                    break;
                }
            }
        }

        // title이 여전히 없으면 파일명에서 추출 (최종 fallback)
        if (title == null || title.isEmpty()) {
            title = extractTitleFromFilename(fileName);
        }

        return new MusicXMLMetadata(title, composer);
    }

    /**
     * MXL 파일(압축된 MusicXML)에서 XML 추출
     */
    private InputStream extractXmlFromMxl(InputStream mxlInputStream) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(mxlInputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                // META-INF/container.xml에서 rootfile 찾기 또는 직접 .xml 파일 찾기
                String entryName = entry.getName();
                if (entryName.endsWith(".xml") && !entryName.startsWith("META-INF/")) {
                    // XML 내용을 ByteArrayOutputStream으로 읽기
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zipInputStream.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    return new ByteArrayInputStream(baos.toByteArray());
                }
                zipInputStream.closeEntry();
            }
        }
        throw new IOException("MXL 파일에서 XML을 찾을 수 없습니다.");
    }

    /**
     * InputStream에서 Document 파싱 (악기 정보 추출용)
     */
    private Document parseDocument(InputStream inputStream, String fileName) throws Exception {
        InputStream xmlStream = inputStream;

        // MXL 파일인 경우 압축 해제
        if (fileName.toLowerCase().endsWith(".mxl")) {
            xmlStream = extractXmlFromMxl(inputStream);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlStream);
        doc.getDocumentElement().normalize();
        return doc;
    }

    /**
     * 피아노와 기타 파트 찾기
     */
    private List<PartInfo> findPianoAndGuitarParts(Document doc) {
        List<PartInfo> parts = new ArrayList<>();

        NodeList scorePartList = doc.getElementsByTagName("score-part");
        for (int i = 0; i < scorePartList.getLength(); i++) {
            Element scorePart = (Element) scorePartList.item(i);
            String id = scorePart.getAttribute("id");

            // part-name 추출
            String partName = null;
            NodeList partNameList = scorePart.getElementsByTagName("part-name");
            if (partNameList.getLength() > 0) {
                partName = partNameList.item(0).getTextContent().trim();
            }

            if (partName != null && !partName.isEmpty()) {
                String lowerName = partName.toLowerCase();

                // 피아노 판별 (instrumentId = 1)
                if (lowerName.contains("piano") || lowerName.contains("pno") ||
                    lowerName.contains("피아노") || lowerName.contains("keyboard")) {
                    parts.add(new PartInfo(id, "piano", 1));
                    log.info("피아노 파트 발견: {} (ID: {})", partName, id);
                }
                // 기타 판별 (instrumentId = 2)
                if (lowerName.contains("guitar") || lowerName.contains("gtr") ||
                         lowerName.contains("기타") || lowerName.contains("guitare")) {
                    parts.add(new PartInfo(id, "guitar", 2));
                    log.info("기타 파트 발견: {} (ID: {})", partName, id);
                }
//                parts.add(new PartInfo(id, "piano", 1));
            }
        }

        return parts;
    }

    /**
     * 특정 파트만 포함하는 Document 생성
     */
    private Document createFilteredDocument(Document originalDoc, String partId) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document newDoc = builder.newDocument();

        // 루트 요소 복사
        Element originalRoot = originalDoc.getDocumentElement();
        Element newRoot = (Element) newDoc.importNode(originalRoot, false);
        newDoc.appendChild(newRoot);

        // 루트의 모든 자식 노드를 순회하며 복사
        NodeList children = originalRoot.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            // part-list 노드인 경우 필터링하여 복사
            if (child.getNodeType() == Node.ELEMENT_NODE && "part-list".equals(child.getNodeName())) {
                Element filteredPartList = filterPartList((Element) child, partId, newDoc);
                newRoot.appendChild(filteredPartList);
            }
            // part 노드인 경우 해당 파트만 복사
            else if (child.getNodeType() == Node.ELEMENT_NODE && "part".equals(child.getNodeName())) {
                Element partElement = (Element) child;
                String id = partElement.getAttribute("id");
                if (partId.equals(id)) {
                    Node importedPart = newDoc.importNode(partElement, true);
                    newRoot.appendChild(importedPart);
                }
            }
            // 다른 노드들은 그대로 복사
            else {
                Node importedNode = newDoc.importNode(child, true);
                newRoot.appendChild(importedNode);
            }
        }

        return newDoc;
    }

    /**
     * 특정 파트만 포함하는 part-list 생성
     */
    private Element filterPartList(Element originalPartList, String partId, Document newDoc) {
        Element newPartList = newDoc.createElement("part-list");

        NodeList children = originalPartList.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE && "score-part".equals(child.getNodeName())) {
                Element scorePart = (Element) child;
                String id = scorePart.getAttribute("id");
                if (partId.equals(id)) {
                    Node importedScorePart = newDoc.importNode(scorePart, true);
                    newPartList.appendChild(importedScorePart);
                }
            } else {
                // 텍스트 노드 등도 복사
                Node importedNode = newDoc.importNode(child, true);
                newPartList.appendChild(importedNode);
            }
        }

        return newPartList;
    }

    /**
     * Document를 InputStream으로 변환
     */
    private InputStream documentToInputStream(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(outputStream));
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * MusicXML 메타데이터 (title, composer)
     */
    private static class MusicXMLMetadata {
        private final String title;
        private final String composer;

        public MusicXMLMetadata(String title, String composer) {
            this.title = title;
            this.composer = composer;
        }

        public String getTitle() {
            return title;
        }

        public String getComposer() {
            return composer;
        }
    }

    /**
     * 파트 정보 (id, instrumentName, instrumentId)
     */
    private static class PartInfo {
        private final String id;
        private final String instrumentName;
        private final Integer instrumentId;

        public PartInfo(String id, String instrumentName, Integer instrumentId) {
            this.id = id;
            this.instrumentName = instrumentName;
            this.instrumentId = instrumentId;
        }

        public String getId() {
            return id;
        }

        public String getInstrumentName() {
            return instrumentName;
        }

        public Integer getInstrumentId() {
            return instrumentId;
        }
    }
}
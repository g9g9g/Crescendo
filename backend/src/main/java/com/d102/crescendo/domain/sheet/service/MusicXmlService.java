package com.d102.crescendo.domain.sheet.service;

import com.d102.crescendo.domain.sheet.dto.response.MusicXmlParseResponse;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import com.d102.crescendo.global.service.S3Service;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.ScoreTimewise;
import org.audiveris.proxymusic.TypedText;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class MusicXmlService {

    private static final String MUSICXML_MIME = "application/vnd.recordare.musicxml+xml";

    private final S3Client s3Client;
    private final S3Service s3Service;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${file.musicxml.preview.measures:16}")
    private int previewMeasures;

    /**
     * XML(.xml)과 압축 MusicXML(.mxl)을 모두 지원.
     */
    public MusicXmlParseResponse parseAndBuildPreview(String xmlUrl) {
        File tempXmlFile = null;
        try {
            // 1) 다운로드
            byte[] rawBytes = downloadBytes(xmlUrl);
            if (rawBytes == null || rawBytes.length == 0) {
                throw new BusinessException(BusinessError.MUSICXML_EMPTY_FILE);
            }

            // 2) 포맷 판별
            boolean isMxl = looksLikeMxlUrl(xmlUrl) || looksLikeMxlBytes(rawBytes);
            log.info("MusicXML 다운로드 완료 - size={} bytes, isMxl={}", rawBytes.length, isMxl);

            byte[] xmlBytes;
            if (isMxl) {
                // 3) .mxl 해제 → root XML 추출
                xmlBytes = extractXmlFromMxl(rawBytes);
                if (xmlBytes == null || xmlBytes.length == 0) {
                    throw new BusinessException(BusinessError.MUSICXML_INVALID_FORMAT);
                }

                log.info("MXL에서 XML 추출 완료 - 크기: {} bytes", xmlBytes.length);
                validateXmlHeader(xmlBytes);

                String xmlPreview = new String(xmlBytes, 0, Math.min(xmlBytes.length, 500), StandardCharsets.UTF_8);
                log.debug("추출된 XML 미리보기 (첫 500자): {}", xmlPreview);

                String xmlContent = new String(xmlBytes, StandardCharsets.UTF_8);
                if (!(xmlContent.contains("<score-partwise") || xmlContent.contains("<score-timewise"))) {
                    log.error("유효한 MusicXML 루트 엘리먼트를 찾을 수 없음. <score-partwise> 또는 <score-timewise> 필요");
                    throw new BusinessException(BusinessError.MUSICXML_PARSE_ERROR);
                }
            } else {
                // 평문 XML
                validateXmlHeader(rawBytes);
                xmlBytes = rawBytes;
            }

            // 4) 파일로 저장
            tempXmlFile = toTempXmlFile(xmlBytes);

            // 5) JAXB 파싱 (패키지 컨텍스트 + 언랩 + 네임스페이스/DTD 보정)
            Object root = unmarshalRoot(tempXmlFile);
            ScorePartwise score;
            if (root instanceof ScorePartwise spw) {
                score = spw;
            } else if (root instanceof ScoreTimewise) {
                log.error("score-timewise 형식은 현재 지원하지 않습니다. score-partwise 형식만 지원됩니다.");
                throw new BusinessException(BusinessError.MUSICXML_PARSE_ERROR);
            } else {
                log.error("지원하지 않는 루트 타입: {}", root.getClass());
                throw new BusinessException(BusinessError.MUSICXML_PARSE_ERROR);
            }

            // 6) 메타
            int maxMeasures = calculateMaxMeasures(score);
            int partCount = score.getPart() != null ? score.getPart().size() : 0;
            String title = extractTitle(score);
            String composer = extractComposer(score);

            // 7) 전체 파일 URL은 클라 원본
            log.info("원본 XML URL 사용: {}", xmlUrl);

            // 8) 미리보기 생성 → S3
            ScorePartwise previewScore = createPreviewScore(score, previewMeasures);
            String previewS3Key = "musicxml/preview/" + UUID.randomUUID() + ".xml";
            uploadScoreToS3(previewScore, previewS3Key);
            String previewUrl = s3Service.buildFileUrl(previewS3Key);

            log.info("MusicXML 파싱 완료 - 제목: {}, 작곡가: {}, 마디수: {}, 파트수: {}",
                    title, composer, maxMeasures, partCount);

            return MusicXmlParseResponse.builder()
                    .fullUrl(xmlUrl)
                    .previewUrl(previewUrl)
                    .maxMeasureCount((short) maxMeasures)
                    .partCount((short) partCount)
                    .title(title)
                    .composer(composer)
                    .build();

        } catch (JAXBException e) {
            log.error("MusicXML 파싱 오류 - 타입: {}, 메시지: {}", e.getClass().getName(), e.getMessage(), e);
            if (e.getLinkedException() != null) {
                log.error("연결된 예외: {}", e.getLinkedException().getMessage(), e.getLinkedException());
            }
            throw new BusinessException(BusinessError.MUSICXML_PARSE_ERROR);
        } catch (IOException e) {
            log.error("파일 처리 IO 오류 - 타입: {}, 메시지: {}, URL: {}", e.getClass().getName(), e.getMessage(), xmlUrl, e);
            throw new BusinessException(BusinessError.FILE_PROCESSING_ERROR);
        } catch (BusinessException e) {
            log.error("비즈니스 예외 - {}: {}", e.getBusinessError(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("예상치 못한 오류 - 타입: {}, 메시지: {}, 원인: {}, URL: {}",
                    e.getClass().getName(),
                    e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "없음",
                    xmlUrl,
                    e);
            throw new BusinessException(BusinessError.MUSICXML_PROCESSING_ERROR);
        } finally {
            if (tempXmlFile != null && tempXmlFile.exists() && !tempXmlFile.delete()) {
                log.warn("임시 XML 파일 삭제 실패: {}", tempXmlFile.getAbsolutePath());
            }
        }
    }

    /* =========================
       다운로드/판별/추출 유틸
       ========================= */

    private byte[] downloadBytes(String url) throws IOException {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new BusinessException(BusinessError.INVALID_FILE_URL);
        }
        log.info("XML 파일 다운로드 시작 - URL: {}", url);

        // S3 URL인 경우 AWS SDK 사용 (credentials 포함)
        if (isS3Url(url)) {
            return downloadFromS3(url);
        }

        // 일반 URL인 경우 기존 방식
        try (InputStream in = new URL(url).openStream();
             ByteArrayOutputStream bos = new ByteArrayOutputStream(64 * 1024)) {
            in.transferTo(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("XML 파일 다운로드 실패 - URL: {}, 오류: {}", url, e.getMessage());
            throw e;
        }
    }

    private boolean isS3Url(String url) {
        return url.contains(".s3.") && url.contains(".amazonaws.com");
    }

    private byte[] downloadFromS3(String url) {
        try {
            // URL에서 bucket과 key 추출
            // 예: https://bucket-name.s3.region.amazonaws.com/key/path/file.xml?X-Amz-Algorithm=...
            String[] parts = url.replace("https://", "").split("/", 2);
            String bucketPart = parts[0];
            String key = parts[1];

            // 쿼리 파라미터 제거 (Signed URL 대응)
            int queryIndex = key.indexOf('?');
            if (queryIndex != -1) {
                key = key.substring(0, queryIndex);
            }

            log.info("S3에서 다운로드 - bucket: {}, key: {}", bucketName, key);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            log.info("S3 다운로드 완료 - 크기: {} bytes", objectBytes.asByteArray().length);
            return objectBytes.asByteArray();

        } catch (Exception e) {
            log.error("S3 파일 다운로드 실패 - URL: {}", url, e);
            throw new BusinessException(BusinessError.FILE_PROCESSING_ERROR);
        }
    }

    private boolean looksLikeMxlUrl(String url) {
        return url != null && url.toLowerCase(Locale.ROOT).endsWith(".mxl");
    }

    private boolean looksLikeMxlBytes(byte[] bytes) {
        // zip 시그니처: 50 4B 03 04 (PK..)
        return bytes.length >= 4
                && bytes[0] == 0x50
                && bytes[1] == 0x4B
                && bytes[2] == 0x03
                && bytes[3] == 0x04;
    }

    private void validateXmlHeader(byte[] bytes) {
        if (bytes.length < 5) {
            throw new BusinessException(BusinessError.MUSICXML_EMPTY_FILE);
        }
        int offset = 0;
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        if (bytes[offset] != '<') {
            String head = new String(bytes, offset, Math.min(bytes.length - offset, 60), StandardCharsets.UTF_8);
            log.error("유효한 XML 헤더가 아님 - 일부내용: {}", head.replaceAll("\\s+", " "));
            throw new BusinessException(BusinessError.MUSICXML_INVALID_FORMAT);
        }
    }

    private byte[] extractXmlFromMxl(byte[] mxlBytes) {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(mxlBytes);
             ZipInputStream zin = new ZipInputStream(bin)) {

            Map<String, byte[]> entries = new HashMap<>();
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                byte[] data = readAll(zin);
                entries.put(ze.getName(), data);
            }

            log.info("MXL ZIP 내부 파일 목록 ({}개): {}", entries.size(), entries.keySet());

            // META-INF/container.xml에서 rootfile 경로 찾기
            byte[] container = findZipEntryIgnoreCase(entries, "META-INF/container.xml");
            String rootPath = null;
            if (container != null) {
                log.info("container.xml 발견, rootfile 경로 파싱 시도");
                rootPath = parseRootfilePathFromContainerXml(container);
                log.info("파싱된 rootPath: {}", rootPath);
            } else {
                log.warn("container.xml을 찾을 수 없음");
            }

            // rootPath가 없으면 첫 번째 .xml 선택
            if (rootPath == null) {
                log.warn("rootPath가 null, 첫 번째 .xml 파일 검색");
                for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                    if (e.getKey().toLowerCase(Locale.ROOT).endsWith(".xml")
                            && !e.getKey().toLowerCase(Locale.ROOT).contains("container")) {
                        log.info("첫 번째 XML 파일 사용: {}", e.getKey());
                        return e.getValue();
                    }
                }
                log.error("ZIP 내부에 .xml 파일을 찾을 수 없음");
                throw new BusinessException(BusinessError.MUSICXML_INVALID_FORMAT);
            }

            // 경로 정규화 후 조회
            byte[] xml = entries.get(rootPath);
            if (xml == null) {
                String alt = rootPath.replace("\\", "/");
                xml = entries.get(alt);
            }
            if (xml == null) {
                String lower = rootPath.toLowerCase(Locale.ROOT);
                for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                    if (e.getKey().toLowerCase(Locale.ROOT).equals(lower)) {
                        xml = e.getValue();
                        break;
                    }
                }
            }
            if (xml == null) {
                throw new BusinessException(BusinessError.MUSICXML_INVALID_FORMAT);
            }

            return xml;

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(BusinessError.MUSICXML_INVALID_FORMAT);
        }
    }

    private byte[] findZipEntryIgnoreCase(Map<String, byte[]> entries, String path) {
        String target = path.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            if (e.getKey().toLowerCase(Locale.ROOT).equals(target)) {
                return e.getValue();
            }
        }
        return null;
    }

    private String parseRootfilePathFromContainerXml(byte[] containerXml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        // 보안 옵션
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setNamespaceAware(true);

        DocumentBuilder b = f.newDocumentBuilder();
        Document doc;
        try (InputStream in = new ByteArrayInputStream(containerXml)) {
            doc = b.parse(in);
        }

        XPath xp = XPathFactory.newInstance().newXPath();
        Node attr = (Node) xp.evaluate("//rootfile/@full-path", doc, XPathConstants.NODE);
        return (attr != null) ? attr.getNodeValue() : null;
    }

    private byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64 * 1024);
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);
        return bos.toByteArray();
    }

    private File toTempXmlFile(byte[] xmlBytes) throws IOException {
        File f = File.createTempFile("musicxml_", ".xml");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(xmlBytes);
        }
        return f;
    }

    /* =========================
       기존 파싱/업로드 로직
       ========================= */

    private void uploadFileToS3(File file, String s3Key) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(MUSICXML_MIME)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromFile(file));
        log.info("S3 업로드 완료: {}", s3Key);
    }

    private void uploadScoreToS3(ScorePartwise score, String s3Key) throws JAXBException, IOException {
        File tempFile = File.createTempFile("preview_", ".xml");
        try {
            // ✅ 패키지 기반 컨텍스트 (ObjectFactory 로드) — jakarta 빌드 필요
            JAXBContext jaxbContext = JAXBContext.newInstance(ScorePartwise.class, ScoreTimewise.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.marshal(score, tempFile);

            uploadFileToS3(tempFile, s3Key);
        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                log.warn("미리보기 임시 파일 삭제 실패: {}", tempFile.getAbsolutePath());
            }
        }
    }

    /**
     * 루트 언마샬 + 네임스페이스/DTD 보정.
     * - DTD 제거
     * - score-(partwise|timewise) 루트에 기본 네임스페이스 없으면 주입
     * - JAXBContext는 패키지명으로 생성 (ObjectFactory 로딩)
     */
    private Object unmarshalRoot(File xmlFile) throws Exception {
        // 1) 원문 읽기
        byte[] original = java.nio.file.Files.readAllBytes(xmlFile.toPath());

        // 2) 네임스페이스/DTD 보정
        byte[] normalized = normalizeMusicXml(original);
        if (log.isDebugEnabled()) {
            String preview = new String(normalized, 0, Math.min(normalized.length, 200), StandardCharsets.UTF_8);
            log.debug("정규화된 XML 미리보기(200자): {}", preview.replaceAll("\\s+", " ").trim());
        }

        // 3) 보안 해제 옵션 설정된 StAX 파서
        XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xif.setProperty(javax.xml.stream.XMLInputFactory.IS_NAMESPACE_AWARE, false);
        XMLStreamReader xsr = xif.createXMLStreamReader(new ByteArrayInputStream(normalized));

        // 4) 패키지 기반 JAXB 컨텍스트
        JAXBContext jaxbContext = JAXBContext.newInstance(ScorePartwise.class, ScoreTimewise.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        Object root = unmarshaller.unmarshal(xsr);
        if (root instanceof JAXBElement<?> jbe) {
            root = jbe.getValue(); // 언랩
        }
        return root;
    }

    private byte[] normalizeMusicXml(byte[] xmlBytes) {
        String s = new String(xmlBytes, StandardCharsets.UTF_8);

        // (1) DOCTYPE 제거
        s = s.replaceFirst("(?is)<!DOCTYPE\\s+score-(?:partwise|timewise)[^>]*>", "");

        // (2) 문서 전체의 xmlns 선언(기본/접두) 제거: "..." 와 '...' 모두 대응
        //     예) xmlns="http://..."  또는  xmlns:mxl='http://...'
        s = s.replaceAll("(?is)\\s+xmlns(?:\\:[A-Za-z0-9_.\\-]+)?\\s*=\\s*(\"[^\"]*\"|'[^']*')", "");

        // (3) 접두사 제거: <mxl:score-partwise ...> -> <score-partwise ...>
        //                  </mxl:measure> -> </measure>
        s = s.replaceAll("(?is)<\\s*([A-Za-z0-9_.\\-]+):", "<");
        s = s.replaceAll("(?is)</\\s*([A-Za-z0-9_.\\-]+):", "</");

        // (4) 여분 공백 정리(선택)
        s = s.replaceAll(">\\s+<", "><");

        return s.getBytes(StandardCharsets.UTF_8);
    }




    private int calculateMaxMeasures(ScorePartwise score) {
        if (score.getPart() == null || score.getPart().isEmpty()) {
            return 0;
        }
        return score.getPart().stream()
                .mapToInt(part -> part.getMeasure().size())
                .max()
                .orElse(0);
    }

    private String extractTitle(ScorePartwise score) {
        if (score.getWork() != null && score.getWork().getWorkTitle() != null) {
            return score.getWork().getWorkTitle();
        }
        if (score.getMovementTitle() != null) {
            return score.getMovementTitle();
        }
        return "제목 없음";
    }

    private String extractComposer(ScorePartwise score) {
        if (score.getIdentification() != null &&
                score.getIdentification().getCreator() != null) {
            for (TypedText creator : score.getIdentification().getCreator()) {
                if ("composer".equalsIgnoreCase(creator.getType())) {
                    return creator.getValue();
                }
            }
        }
        return "작곡가 미상";
    }

    private ScorePartwise createPreviewScore(ScorePartwise originalScore, int measureLimit) {
        ScorePartwise previewScore = new ScorePartwise();

        // 메타데이터 복사
        previewScore.setVersion(originalScore.getVersion());
        previewScore.setWork(originalScore.getWork());
        previewScore.setMovementTitle(originalScore.getMovementTitle());
        previewScore.setIdentification(originalScore.getIdentification());
        previewScore.setPartList(originalScore.getPartList());

        if (originalScore.getPart() != null) {
            for (ScorePartwise.Part originalPart : originalScore.getPart()) {
                ScorePartwise.Part previewPart = new ScorePartwise.Part();
                previewPart.setId(originalPart.getId());

                List<ScorePartwise.Part.Measure> measures = originalPart.getMeasure();
                int limit = Math.min(Math.max(measureLimit, 0), measures.size());
                if (limit > 0) {
                    previewPart.getMeasure().addAll(measures.subList(0, limit));
                }
                previewScore.getPart().add(previewPart);
            }
        }
        return previewScore;
    }
}

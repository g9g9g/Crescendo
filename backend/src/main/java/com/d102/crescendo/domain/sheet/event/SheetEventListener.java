package com.d102.crescendo.domain.sheet.event;

import com.d102.crescendo.domain.sheet.document.SheetMusicDocument;
import com.d102.crescendo.domain.sheet.repository.SheetMusicDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class SheetEventListener {

    private final SheetMusicDocumentRepository sheetMusicDocumentRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSheetCreatedEvent(SheetCreatedEvent event) {
        log.info("SheetCreatedEvent 수신: sheetId={}, title={}", event.getSheetId(), event.getTitle());

        try {
            // 이벤트에서 받은 데이터로 SheetMusicDocument 생성 (DB 조회 불필요)
            SheetMusicDocument document = SheetMusicDocument.builder()
                    .sheetId(event.getSheetId())
                    .title(event.getTitle())
                    .composer(event.getComposer())
                    .thumbnailUrl(event.getThumbnailUrl())
                    .genreId(event.getGenreId())
                    .tierCode(event.getTierCode())
                    .tierLevel(event.getTierLevel() != null ? event.getTierLevel().intValue() : null)
                    .instrumentId(event.getInstrumentId())
                    .downloadNumber(event.getDownloadNumber())
                    .updatedAt(event.getUpdatedAt())
                    .sourceType(event.getSourceType())
                    .genre(event.getGenre())
                    .instrument(event.getInstrument())
                    .build();

            // Elasticsearch에 저장
            sheetMusicDocumentRepository.save(document);
            log.info("Elasticsearch에 SheetMusicDocument 저장 완료: sheetId={}", event.getSheetId());
        } catch (Exception e) {
            log.error("Elasticsearch 저장 중 에러 발생: sheetId={}", event.getSheetId(), e);
        }
    }
}
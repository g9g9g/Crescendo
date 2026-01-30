package com.d102.crescendo.domain.ai.controller;

import com.d102.crescendo.domain.ai.dto.request.DifficultyEvaluationRequest;
import com.d102.crescendo.domain.ai.dto.request.EmbeddingRequest;
import com.d102.crescendo.domain.ai.dto.response.BatchDifficultyEvaluationResponse;
import com.d102.crescendo.domain.ai.dto.response.BatchEmbeddingResponse;
import com.d102.crescendo.domain.ai.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/ai")
@Tag(name = "07. AI", description = "AI 임베딩 및 추천 관련 API")
public class AiController {

    private final AiService aiService;

    @PostMapping("/embedding")
    @Operation(
            summary = "악보 임베딩 생성 및 DB 저장",
            description = "여러 악보의 sheetId를 받아 AI 서버에서 임베딩을 생성하고 SheetMusic.embedding에 저장합니다. 성공/실패 ID 리스트를 포함하여 반환합니다."
    )
    public ResponseEntity<BatchEmbeddingResponse> generateEmbedding(@RequestBody EmbeddingRequest request) {
        BatchEmbeddingResponse response = aiService.generateAndSaveEmbeddings(request.getSheetIds());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/difficulty/evaluation")
    @Operation(
            summary = "악보 난이도 평가 및 DB 저장",
            description = "여러 악보의 sheetId를 받아 AI 서버에서 난이도를 평가하고 SheetMusic.tier와 SheetDifficultyMetric을 DB에 저장합니다. 성공/실패 ID 리스트를 포함하여 반환합니다."
    )
    public ResponseEntity<BatchDifficultyEvaluationResponse> evaluateDifficulty(@RequestBody DifficultyEvaluationRequest request) {
        BatchDifficultyEvaluationResponse response = aiService.evaluateAndSaveDifficulties(request.getSheetIds());
        return ResponseEntity.ok(response);
    }

}

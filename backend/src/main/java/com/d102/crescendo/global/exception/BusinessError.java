package com.d102.crescendo.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BusinessError {
    //Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_DELETE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 삭제 실패"),
    INVALID_GOOGLE_TOKEN(HttpStatus.BAD_REQUEST, "구글 서버 검증 과정 중 오류"),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다."),
    TOKEN_EMAIL_MISMATCH(HttpStatus.UNAUTHORIZED, "토큰 정보와 사용자 정보가 일치하지 않습니다."),
    REDIS_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 연결에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    //User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_EMAIL_NOT_FOUND(HttpStatus.BAD_REQUEST, "유저 이메일이 존재하지 않습니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "닉네임 형식이 올바르지 않습니다."),
    NICKNAME_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "닉네임 생성에 실패했습니다."),

    //Genre
    GENRE_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 장르입니다."),

    //Instrument
    INSTRUMENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 악기입니다."),

    //Tier
    TIER_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 티어입니다."),


    // [수정] MusicXML 처리 관련 예외 추가
    INVALID_FILE_URL(HttpStatus.BAD_REQUEST, "유효하지 않은 파일 URL입니다."),
    MUSICXML_PARSE_ERROR(HttpStatus.BAD_REQUEST, "올바른 MusicXML 형식이 아닙니다. XML 파일 형식을 확인해주세요."),
    MUSICXML_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "유효하지 않은 XML 파일입니다. MusicXML 형식의 파일을 업로드해주세요."),
    MUSICXML_EMPTY_FILE(HttpStatus.BAD_REQUEST, "빈 파일입니다. 올바른 MusicXML 파일을 업로드해주세요."),
    FILE_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리 중 오류가 발생했습니다."),
    MUSICXML_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "MusicXML 처리 중 예상치 못한 오류가 발생했습니다."),


    // Sheet
    SHEET_NOT_FOUND(HttpStatus.NOT_FOUND, "악보를 찾을 수 없습니다."),
    SHEET_NOT_VISIBLE(HttpStatus.FORBIDDEN, "비공개 악보입니다."),
    USER_SHEET_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 악보를 찾을 수 없습니다."),
    NOT_SERVICE_SHEET(HttpStatus.BAD_REQUEST, "스토어 악보가 아닙니다."),
    SHEET_ALREADY_ADDED(HttpStatus.CONFLICT, "이미 내 악보에 담은 악보입니다."),

    //AI 서버
    AI_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI 서버 내부 오류"),
    AI_RECOMMENDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "유사 악보 추천 요청 실패"),
    EMBEDDING_NOT_FOUND(HttpStatus.BAD_REQUEST, "악보의 임베딩이 존재하지 않습니다."),

    //FCM
    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FCM 토큰을 찾을 수 없습니다."),
    FCM_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "FCM 토큰이 필요합니다."),
    FCM_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "푸시 알림 전송에 실패했습니다."),

    //Performance
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 연주 기록입니다."),
    PERFORMANCE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 연주 기록에 대한 접근 권한이 없습니다."),

    //UserInstrumentTier
    USER_INSTRUMENT_TIER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자의 악기별 티어 정보를 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;

    private final String message;
}
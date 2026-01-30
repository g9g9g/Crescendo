package com.d102.crescendo.presentation.ui.screen.mysheets

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import com.d102.crescendo.di.ApplicationScope
import com.d102.crescendo.domain.model.sheet.PendingSheetData
import com.d102.crescendo.domain.model.sheet.SheetRegistration
import com.d102.crescendo.domain.usecase.convert.ConvertSheetUseCase
import com.d102.crescendo.domain.usecase.s3.UploadFileToS3UseCase
import com.d102.crescendo.domain.usecase.sheet.RegisterUserSheetUseCase
import com.d102.crescendo.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.source
import javax.inject.Inject

private const val TAG = "MySheetsAddViewModel"

@HiltViewModel
class MySheetsAddViewModel @Inject constructor(
    private val uploadFileToS3UseCase: UploadFileToS3UseCase,
    private val convertSheetUseCase: ConvertSheetUseCase,
    private val registerUserSheetUseCase: RegisterUserSheetUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<MySheetsAddUiState>(MySheetsAddUiState.Idle)
    val uiState: StateFlow<MySheetsAddUiState> = _uiState.asStateFlow()

    private var uploadedFileUrl: String? = null
    private var pendingRegistration: PendingSheetData? = null
    private var uploadJob: Job? = null

    fun onFilePicked(uri: Uri, fileName: String, mimeType: String, uploadPath: String = "sheets") {
        // 이미 진행 중인 업로드 취소
        uploadJob?.cancel()
        uploadedFileUrl = null

        uploadJob = applicationScope.launch {
            _uiState.value = MySheetsAddUiState.FileUploading
            Log.d(TAG, "파일 선택됨 — name=$fileName, mime=$mimeType, uri=$uri")

            runCatching {
                when {
                    mimeType.equals("application/pdf", true) || fileName.endsWith(".pdf", true) -> {
                        // PDF → OMR 변환 (서버에서 직접 S3에 업로드)
                        Log.d(TAG, "PDF 감지 — OMR 변환 시작")
                        val part = uri.toMultipartPart(
                            context = context,
                            partName = "file",
                            displayName = fileName,
                            mime = "application/pdf"
                        )
                        val omrResponse = convertSheetUseCase(part)  // suspend
                        require(omrResponse.success && omrResponse.s3Url.isNotBlank()) {
                            "OMR 변환 실패 또는 S3 URL 없음"
                        }
                        Log.d(TAG, "OMR 변환 성공 — ${omrResponse.pagesProcessed}페이지, S3 URL=${omrResponse.s3Url}")

                        // 서버에서 반환한 S3 URL을 그대로 사용
                        omrResponse.s3Url
                    }
                    else -> {
                        // MusicXML 류는 기존 흐름 유지: Uri → S3 업로드
                        Log.d(TAG, "MusicXML/유사 확장자 감지 — 직접 S3 업로드")
                        uploadFileToS3UseCase(uri, uploadPath).getOrThrow()
                    }
                }
            }.onSuccess { fileUrl ->
                Log.d(TAG, "파일 처리 성공 — url=$fileUrl")
                uploadedFileUrl = fileUrl
                _uiState.value = MySheetsAddUiState.FileUploadSuccess(fileUrl)

                pendingRegistration?.let { data ->
                    Log.d(TAG, "대기 중 등록 실행")
                    registerSheetImmediately(data)
                } ?: Log.d(TAG, "등록 대기")
            }.onFailure { e ->
                Log.e(TAG, "파일 처리 실패 — ${e.message}", e)
                _uiState.value = MySheetsAddUiState.FileUploadError(e.message ?: "파일 처리 실패")
            }
        }
    }


    fun uploadFileInBackground(uri: Uri, uploadPath: String = "sheets") {
        Log.d(TAG, "========== 파일 업로드 시작 ==========")
        Log.d(TAG, "URI: $uri")
        Log.d(TAG, "Upload Path: $uploadPath")

        // 이미 진행 중인 업로드가 있으면 취소
        uploadJob?.cancel()

        // 이전 업로드 데이터 초기화
        uploadedFileUrl = null

        uploadJob = applicationScope.launch {
            _uiState.value = MySheetsAddUiState.FileUploading
            Log.d(TAG, "파일 업로드 상태 변경: FileUploading")

            uploadFileToS3UseCase(uri, uploadPath)
                .onSuccess { fileUrl ->
                    Log.d(TAG, "========== 파일 업로드 성공 ==========")
                    Log.d(TAG, "반환된 File URL: $fileUrl")
                    Log.d(TAG, "URL 확장자: ${fileUrl.substringAfterLast('.')}")

                    uploadedFileUrl = fileUrl
                    _uiState.value = MySheetsAddUiState.FileUploadSuccess(fileUrl)
                    Log.d(TAG, "파일 업로드 상태 변경: FileUploadSuccess")

                    pendingRegistration?.let { data ->
                        Log.d(TAG, "대기 중인 등록 발견 - 자동 등록 시작")
                        Log.d(TAG, "등록할 악보: ${data.title}")
                        registerSheetImmediately(data)
                    } ?: run {
                        Log.d(TAG, "대기 중인 등록 없음 - 사용자 등록 버튼 대기")
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "========== 파일 업로드 실패 ==========")
                    Log.e(TAG, "에러 메시지: ${error.message}")
                    Log.e(TAG, "에러 스택:", error)

                    _uiState.value = MySheetsAddUiState.FileUploadError(
                        error.message ?: "파일 업로드에 실패했습니다."
                    )
                }
        }
    }

    fun cancelUpload() {
        Log.d(TAG, "업로드 취소 요청")
        uploadJob?.cancel()
        uploadJob = null
        uploadedFileUrl = null
        pendingRegistration = null
        _uiState.value = MySheetsAddUiState.Idle
        Log.d(TAG, "업로드 취소 완료")
    }

    fun onRegisterButtonClicked(
        title: String,
        composer: String,
        genre: String,
        instrument: String
    ) {
        Log.d(TAG, "========== 등록 버튼 클릭 ==========")
        Log.d(TAG, "제목: $title")
        Log.d(TAG, "작곡가: $composer")
        Log.d(TAG, "장르: $genre")
        Log.d(TAG, "악기: $instrument")

        val data = PendingSheetData(
            title = title,
            composer = composer,
            genre = genre,
            instrument = instrument
        )

        when (val currentState = _uiState.value) {
            is MySheetsAddUiState.FileUploadSuccess -> {
                Log.d(TAG, "파일 업로드 완료 상태 - 즉시 등록 시작")
                registerSheetImmediately(data)
            }
            is MySheetsAddUiState.FileUploading -> {
                Log.d(TAG, "파일 업로드 중 - 등록 정보 저장 후 대기")
                pendingRegistration = data
            }
            is MySheetsAddUiState.FileUploadError -> {
                Log.w(TAG, "파일 업로드 에러 상태 - 등록 불가")
                return
            }
            else -> {
                Log.w(TAG, "파일 업로드 안됨 - 등록 불가")
                _uiState.value = MySheetsAddUiState.FileUploadError(
                    "파일을 먼저 업로드해주세요."
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerSheetImmediately(data: PendingSheetData) {
        val fileUrl = uploadedFileUrl ?: run {
            Log.e(TAG, "파일 URL이 없어서 등록 불가")
            _uiState.value = MySheetsAddUiState.SheetRegistrationError(
                "파일 URL이 없습니다."
            )
            return
        }

        applicationScope.launch {
            Log.d(TAG, "========== 악보 등록 API 호출 시작 ==========")
            _uiState.value = MySheetsAddUiState.SheetRegistering

            val registration = SheetRegistration(
                title = data.title,
                composer = data.composer,
                genre = data.genre,
                instrument = data.instrument,
                xmlUrl = fileUrl
            )

            Log.d(TAG, "등록 데이터:")
            Log.d(TAG, "  - title: ${registration.title}")
            Log.d(TAG, "  - composer: ${registration.composer}")
            Log.d(TAG, "  - genre: ${registration.genre}")
            Log.d(TAG, "  - instrument: ${registration.instrument}")
            Log.d(TAG, "  - xmlUrl: ${registration.xmlUrl}")

            registerUserSheetUseCase(registration)
                .onSuccess {
                    Log.d(TAG, "========== 악보 등록 성공! ==========")
                    Log.d(TAG, "알림 표시 시작")

                    // 로컬 알림 표시
                    NotificationHelper.showSheetRegistrationSuccessNotification(
                        context = context,
                        sheetTitle = data.title
                    )

                    Log.d(TAG, "알림 표시 완료")
                    _uiState.value = MySheetsAddUiState.SheetRegistrationSuccess
                    cleanup()
                }
                .onFailure { error ->
                    Log.e(TAG, "========== 악보 등록 실패 ==========")
                    Log.e(TAG, "에러 메시지: ${error.message}")
                    Log.e(TAG, "에러 스택:", error)

                    // 실패 알림 표시
                    NotificationHelper.showSheetRegistrationErrorNotification(
                        context = context,
                        errorMessage = error.message ?: "악보 등록에 실패했습니다."
                    )

                    _uiState.value = MySheetsAddUiState.SheetRegistrationError(
                        error.message ?: "악보 등록에 실패했습니다."
                    )
                }
        }
    }

    private fun cleanup() {
        Log.d(TAG, "정리 작업 시작")
        uploadedFileUrl = null
        pendingRegistration = null
        uploadJob = null
        Log.d(TAG, "정리 작업 완료")
    }

    fun resetErrorState() {
        if (_uiState.value is MySheetsAddUiState.FileUploadError ||
            _uiState.value is MySheetsAddUiState.SheetRegistrationError) {
            _uiState.value = MySheetsAddUiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel 종료")
    }
}

private fun Uri.toMultipartPart(
    context: Context,
    partName: String,
    displayName: String,
    mime: String
): okhttp3.MultipartBody.Part {
    val resolver = context.contentResolver
    val body = object : okhttp3.RequestBody() {
        override fun contentType() = mime.toMediaTypeOrNull()
        override fun contentLength(): Long =
            resolver.query(this@toMultipartPart, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { if (it.moveToFirst()) it.getLong(it.getColumnIndexOrThrow(OpenableColumns.SIZE)) else -1 }
                ?: -1
        override fun writeTo(sink: okio.BufferedSink) {
            resolver.openInputStream(this@toMultipartPart)?.use { input ->
                input.source().use { source -> sink.writeAll(source) }
            } ?: throw IllegalStateException("InputStream 열기 실패: $this@toMultipartPart")
        }
    }
    return okhttp3.MultipartBody.Part.createFormData(partName, displayName, body)
}
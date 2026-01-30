package com.d102.crescendo.presentation.ui.screen.mysheets

import android.R.attr.scaleX
import android.R.attr.scaleY
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.d102.crescendo.R
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.LLight_Gray
import com.d102.crescendo.presentation.theme.Light_Gray
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.presentation.ui.component.mysheet.CustomTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySheetsAddScreen(
    viewModel: MySheetsAddViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var composer by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("선택") }
    var selectedInstrument by remember { mutableStateOf("피아노") }
    var showGenreDropdown by remember { mutableStateOf(false) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileType by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var isRegistering by remember { mutableStateOf(false) } // 등록 버튼 눌렀는지 추적

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsState()

    // 악보 파일 업로드 버튼 부분 수정
    var isUploadBoxPressed by remember { mutableStateOf(false) }
    val uploadBoxScale by animateFloatAsState(
        targetValue = if (isUploadBoxPressed) 0.90f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "uploadBoxScale"
    )

    // 뒤로가기 처리 - 등록하지 않고 나가면 업로드 취소
    DisposableEffect(Unit) {
        onDispose {
            if (!isRegistering) {
                viewModel.cancelUpload()
            }
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is MySheetsAddUiState.FileUploadError -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetErrorState()
            }

            is MySheetsAddUiState.SheetRegistrationSuccess -> {
                Toast.makeText(context, "악보가 등록되었습니다.", Toast.LENGTH_SHORT).show()
            }

            is MySheetsAddUiState.SheetRegistrationError -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetErrorState()
            }

            else -> {}
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(it)
            val cursor = contentResolver.query(it, null, null, null, null)

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val displayNameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val fileName = if (displayNameIndex != -1) {
                        c.getString(displayNameIndex)
                    } else {
                        "Unknown"
                    }

                    // PDF 또는 MusicXML 파일만 허용
                    when {
                        mimeType == "application/pdf" || fileName.endsWith(
                            ".pdf",
                            ignoreCase = true
                        ) -> {
                            selectedFileName = fileName
                            selectedFileType = "PDF"
                            selectedFileUri = it
                            // PDF 파일 선택 시 변환 시작
                            viewModel.onFilePicked(
                                uri = it,
                                fileName = fileName,
                                mimeType = mimeType ?: "application/pdf"
                            )
                        }

                        mimeType == "application/vnd.recordare.musicxml+xml" ||
                                mimeType == "application/vnd.recordare.musicxml" ||
                                fileName.endsWith(".musicxml", ignoreCase = true) ||
                                fileName.endsWith(".xml", ignoreCase = true) ||
                                fileName.endsWith(".mxl", ignoreCase = true) -> {
                            selectedFileName = fileName
                            selectedFileType = "MusicXML"
                            selectedFileUri = it
                            // MusicXML 파일 선택 시 S3 업로드 시작
                            viewModel.uploadFileInBackground(it, "sheets")
                        }

                        else -> {
                            selectedFileName = null
                            selectedFileType = null
                            selectedFileUri = null
                            Toast.makeText(
                                context,
                                "올바른 형식의 파일을 업로드해주세요.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    val genres = listOf("클래식", "가요", "뉴에이지", "OST", "동요", "CCM")
    val scrollState = rememberScrollState()

    val isFileUploading = uiState is MySheetsAddUiState.FileUploading
    val isFileUploadSuccess = uiState is MySheetsAddUiState.FileUploadSuccess

    // 모든 필드가 입력되었는지 확인
    val isFormValid = selectedFileUri != null &&
            title.isNotBlank() &&
            composer.isNotBlank() &&
            selectedGenre != "선택"

    val customTextSelectionColors = TextSelectionColors(
        handleColor = DarkHover,
        backgroundColor = DarkHover
    )

    val imePaddingValues = WindowInsets.ime.asPaddingValues()
    val bottomImePadding = imePaddingValues.calculateBottomPadding()

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        Box(modifier = Modifier
            .fillMaxSize()) {
            // 메인 콘텐츠
            Box(modifier = Modifier
                .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = 16.dp, end = 16.dp, bottom = bottomImePadding)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // 악보 파일 업로드 버튼
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = uploadBoxScale
                                scaleY = uploadBoxScale
                            }
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(12.dp),
                                clip = false
                            )
                            .border(
                                width = 1.5.dp,
                                color = DarkHover,  // 보더 색상 추가
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        if (!isFileUploading) {
                                            isUploadBoxPressed = true
                                            val released = tryAwaitRelease()
                                            isUploadBoxPressed = false
                                        }
                                    },
                                    onTap = {
                                        if (!isFileUploading) {
                                            filePickerLauncher.launch("*/*")
                                        }
                                    }
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_upload_sheet),
                                contentDescription = "파일",
                                modifier = Modifier.size(44.dp),
                                tint = Color.Unspecified
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isFileUploading -> selectedFileName ?: "파일 업로드 중..."
                                        isFileUploadSuccess -> selectedFileName ?: "업로드 완료"
                                        else -> selectedFileName ?: "악보 파일 업로드"
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkHover,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        isFileUploading -> selectedFileType ?: "계속 등록하셔도 업로드는 자동으로 진행됩니다."
                                        isFileUploadSuccess -> selectedFileType ?: "업로드 완료"
                                        else -> selectedFileType ?: "PDF, MusicXML 지원"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 제목 입력
                    Column {
                        Text(
                            text = "제목",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkHover
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CustomTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "제목을 입력해주세요."
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 작곡가 입력
                    Column {
                        Text(
                            text = "작곡가",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkHover
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CustomTextField(
                            value = composer,
                            onValueChange = { composer = it },
                            placeholder = "작곡가를 입력해주세요."
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 장르 선택
                    Column {
                        Text(
                            text = "장르",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkHover
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Box {
                            // 선택 버튼
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = DarkHover,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        // 키보드 먼저 닫기
                                        keyboardController?.hide()
                                        // 드롭다운 토글
                                        showGenreDropdown = !showGenreDropdown
                                    }
                                    .padding(horizontal = 18.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedGenre,
                                        color = White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        painter = painterResource(R.drawable.ic_down_arrow),
                                        contentDescription = "선택",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }

                            // 드롭다운 메뉴
                            DropdownMenu(
                                expanded = showGenreDropdown,
                                onDismissRequest = { showGenreDropdown = false },
                                containerColor = Color.Transparent,
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp,
                                offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 0.dp),
                                modifier = Modifier.width(100.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = White,
                                    border = BorderStroke(1.dp, DarkHover)
                                ) {
                                    Column {
                                        genres.forEachIndexed { index, genre ->
                                            var isPressed by remember { mutableStateOf(false) }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (isPressed) DarkHover else White)
                                                    .pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onPress = {
                                                                isPressed = true
                                                                val released = tryAwaitRelease()
                                                                isPressed = false
                                                                if (released) {
                                                                    selectedGenre = genre
                                                                    showGenreDropdown = false
                                                                }
                                                            }
                                                        )
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = genre,
                                                    color = if (isPressed) White else DarkHover,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            if (index < genres.size - 1) {
                                                HorizontalDivider(
                                                    color = DarkHover,
                                                    thickness = 0.5.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 악기 선택
                    Column {
                        Text(
                            text = "악기",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkHover
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            InstrumentButton(
                                text = "피아노",
                                isSelected = selectedInstrument == "피아노",
                                onClick = { selectedInstrument = "피아노" }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            InstrumentButton(
                                text = "기타",
                                isSelected = selectedInstrument == "기타",
                                onClick = { selectedInstrument = "기타" }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(120.dp))

                    // 등록 버튼
                    Button(
                        onClick = {
                            isRegistering = true
                            // 백그라운드 처리 안내 토스트
                            Toast.makeText(
                                context,
                                "악보 등록이 완료되면 알림으로 안내드립니다.",
                                Toast.LENGTH_LONG
                            ).show()

                            // 등록 버튼 클릭 시 ViewModel에 데이터 전달
                            viewModel.onRegisterButtonClicked(
                                title = title,
                                composer = composer,
                                genre = selectedGenre,
                                instrument = selectedInstrument
                            )
                            // 즉시 화면 이동 (백그라운드에서 처리 계속됨)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFormValid) DarkHover else LLight_Gray,
                            contentColor = if (isFormValid) White else Gray,
                            disabledContainerColor = LLight_Gray,
                            disabledContentColor = Gray
                        ),
                        enabled = isFormValid,
                    ) {
                        Text(
                            text = "등록",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 외부 클릭 감지 오버레이
            if (showGenreDropdown) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(5f)
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showGenreDropdown = false
                        }
                )
            }
        }
    }
}

@Composable
fun InstrumentButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp, 44.dp)
            .background(
                color = White,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 2.dp,
                color = if (isSelected) DarkHover else Light_Gray,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    id = if (text == "피아노")
                        R.drawable.ic_piano
                    else
                        R.drawable.ic_guitar
                ),
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) DarkHover else Light_Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) DarkHover else Light_Gray
            )
        }
    }
}
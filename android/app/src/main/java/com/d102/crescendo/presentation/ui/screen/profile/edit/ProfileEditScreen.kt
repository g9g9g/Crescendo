package com.d102.crescendo.presentation.ui.screen.profile.edit

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d102.crescendo.presentation.theme.Dark
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.presentation.ui.component.profile.edit.EmailDisplay
import com.d102.crescendo.presentation.ui.component.profile.edit.GenreSelectionGroup
import com.d102.crescendo.presentation.ui.component.profile.edit.NicknameTextField
import com.d102.crescendo.presentation.ui.component.profile.edit.ProfileImageEditor
import androidx.activity.result.PickVisualMediaRequest
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray2
import com.d102.crescendo.presentation.theme.Light_Gray

/**
 * 3단계: 프로필 수정 화면 (골격)
 */
@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit,
    onCompleteClick: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel()
) {
    // ViewModel의 모든 상태 구독
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()  // API 통신 상태
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()
    val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
    val displayImage by viewModel.displayImage.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // API 상태(Complete, Error) 처리
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ProfileEditUiState.Complete -> {
                Toast.makeText(context, "프로필이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                viewModel.consumeUiState()
                onCompleteClick() // 내비게이션 콜백 호출
            }
            is ProfileEditUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.consumeUiState()
            }
            else -> {
                // Idle, Loading은 UI에서 처리
            }
        }
    }

    // 안드로이드 갤러리(Photo Picker) 런처
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            viewModel.onImageSelected(uri)
        }
    )

    val isButtonEnabled = nickname.isNotEmpty() && selectedTags.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally // 프로필 이미지 중앙 정렬
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        //  ProfileImageEditor에 상태 전달 및 런처 연결
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProfileImageEditor(
                imageUri = displayImage, // ViewModel의 Uri 전달
                onClick = {
                    // 갤러리 런처 실행
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        NicknameTextField(
            nickname = nickname,
            onNicknameChanged = viewModel::onNicknameChanged,
        )

        Spacer(modifier = Modifier.height(24.dp))

        EmailDisplay(email = email)

        Spacer(modifier = Modifier.height(36.dp))

        GenreSelectionGroup(
            availableTags = availableTags,
            selectedTags = selectedTags,
            onTagClicked = viewModel::onTagClicked
        )

        Spacer(modifier = Modifier.height(120.dp))

        Button(
            onClick = { viewModel.onSubmitClicked() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // 버튼 높이
            shape = RoundedCornerShape(12.dp),

            enabled = isButtonEnabled && (uiState !is ProfileEditUiState.Loading),
            colors = ButtonDefaults.buttonColors(
                // 활성화
                containerColor = DarkHover,
                contentColor = White,
                disabledContainerColor = Gray3,
                disabledContentColor = Gray
            )
        ) {
            // API 통신 중일 때 로딩 스피너 표시
            if (uiState is ProfileEditUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "프로필 수정",
                    style = Typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
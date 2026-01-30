package com.d102.crescendo.presentation.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.presentation.ui.component.loading.CustomLoadingIndicator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 전역 로딩 상태를 관리하는 ViewModel
 * MainActivity에서 TopAppBar를 포함한 전체 화면 로딩 오버레이를 표시하기 위해 사용
 */
@HiltViewModel
class GlobalLoadingViewModel @Inject constructor() : ViewModel() {

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    fun showLoading(message: String) {
        _loadingState.value = LoadingState.Loading(message)
    }

    fun hideLoading() {
        _loadingState.value = LoadingState.Idle
    }
}

sealed class LoadingState {
    object Idle : LoadingState()
    data class Loading(val message: String) : LoadingState()
}

/**
 * 전역 로딩 오버레이 컴포저블
 * TopAppBar를 포함한 전체 화면을 덮는 로딩 화면
 */
@Composable
fun GlobalLoadingOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x99000000)) // 80% 불투명 검은색
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // 클릭 효과 없음
            ) {
                // 클릭 이벤트 소비하여 하위 레이어로 전달 차단
            },
        contentAlignment = Alignment.Center
    ) {
        CustomLoadingIndicator(
            textColor = White,
            iconColor = White,
            title = message
        )
    }
}
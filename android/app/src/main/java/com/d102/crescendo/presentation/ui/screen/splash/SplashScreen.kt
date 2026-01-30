package com.d102.crescendo.presentation.ui.screen.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.ui.component.splash.SplashStartingMessage


// Auth의 역할을 담당함
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {

    // 스플래시 스테이트 구독
    val splashUiState by viewModel.splashUiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = DarkHover) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), // Scaffold의 패딩 적용
            contentAlignment = Alignment.Center
        ) {
            LaunchedEffect(splashUiState) {
                when (splashUiState) {
                    is SplashUiState.NavigateToMain -> {  // 로그인 되면
                        onNavigateToMain()  // 메인으로 이동
                    }
                    is SplashUiState.NavigateToLogin -> {  // 로그아웃 되면
                        onNavigateToLogin()  // 로그인으로 이동
                    }
                    is SplashUiState.NavigateToOnboarding -> {  // 최초 로그인 되면
                        onNavigateToOnboarding()  // 온보딩으로 이동
                    }
                    is SplashUiState.Loading -> {  // 로딩 중이면
                        // 스피너 표시
                    }
                }
            }

            if (splashUiState is SplashUiState.Loading) {
                SplashStartingMessage()
            }
        }
    }
}
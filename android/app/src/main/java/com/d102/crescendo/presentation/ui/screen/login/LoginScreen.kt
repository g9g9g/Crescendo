package com.d102.crescendo.presentation.ui.screen.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d102.crescendo.R
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.White

@Composable
fun LoginScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    // 상태 관찰
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> {
                if (state.isFirstLogin) {
                    onNavigateToOnboarding()
                } else {
                    onNavigateToMain()
                }
                viewModel.consumeUiState()
            }
            is LoginUiState.Error -> {
                val message = state.message
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                viewModel.consumeUiState()
            }
            else -> {}
        }
    }

    Scaffold(containerColor = DarkHover) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // 로고 영역
                Image(
                    painter = painterResource(id = R.drawable.ic_crescendo_logo),
                    contentDescription = "Crescendo Logo",
                    modifier = Modifier.size(220.dp)
                )

                // 로그인 버튼 영역
                when (uiState) {
                    is LoginUiState.Loading -> {
                        CircularProgressIndicator(color = White)
                    }
                    else -> {
                        Button(
                            onClick = {
                                viewModel.initiateGoogleLogin(context)
                            },
                            enabled = (uiState !is LoginUiState.Loading),
                            modifier = Modifier
                                .width(320.dp)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = White,
                                contentColor = Black
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Google Logo",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.CenterStart)
                                )

                                Text(
                                    text = "Google로 로그인",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Black,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
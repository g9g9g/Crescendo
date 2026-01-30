package com.d102.crescendo.presentation.ui.screen.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.d102.crescendo.presentation.navigation.AppNavHost
import com.d102.crescendo.presentation.navigation.BottomNavigationBar
import com.d102.crescendo.presentation.navigation.Login
import com.d102.crescendo.presentation.navigation.Splash
import com.d102.crescendo.presentation.navigation.shouldShowBottomBar
import com.d102.crescendo.presentation.navigation.topLevelRoutes
import com.d102.crescendo.presentation.theme.Dark
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.presentation.ui.component.CustomTopAppBar
import com.d102.crescendo.presentation.ui.component.TopBarActions
import com.d102.crescendo.presentation.ui.component.getTopBarTitle
import com.d102.crescendo.presentation.ui.component.shouldShowTopBar

@Composable
fun MainScreen(
    navController: NavHostController,
    globalLoadingViewModel: GlobalLoadingViewModel
    ) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 현재 목적지가 topLevelRoutes 중 하나인지 확인
    val isTopLevel = currentDestination?.hierarchy?.any { destination ->
        topLevelRoutes.any { route -> destination.hasRoute(route.route::class) }
    } == true

    // Splash나 Login 화면이면 Dark, 아니면 White
    val containerColor = when {
        currentDestination == null -> Dark  // 초기 상태는 Dark!
        currentDestination.hierarchy?.any { it.hasRoute(Splash::class) || it.hasRoute(Login::class) } == true -> Dark
        else -> White
    }
    Scaffold(
        containerColor = containerColor,
        topBar = {
            if (shouldShowTopBar(navController)) {
                CustomTopAppBar(
                    title = getTopBarTitle(navController),
                    // 탑레벨이면 뒤로가기 제거, 아니면 popBackStack
                    onBackClick = if (isTopLevel) null else {
                        { navController.popBackStack() }
                    },
                    navController = navController,
                    actions = {
                        TopBarActions(
                            navController = navController
                        )
                    }
                )
            }
        },
        bottomBar = {
            if (shouldShowBottomBar(navController)) {
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->  // 2. BottomNavigationBar에서 추가한 insetPadding도 고려하는 내부 로직임.
        AppNavHost(
            navController = navController,
            globalLoadingViewModel = globalLoadingViewModel,
            modifier = Modifier.padding(innerPadding)
                .fillMaxSize()  // 3. 패딩 공간을 비우고 가득 채움 -> 문제 해결
        )
    }
}

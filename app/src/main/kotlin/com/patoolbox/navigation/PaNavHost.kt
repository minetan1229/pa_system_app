package com.patoolbox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PlaceholderScreen
import com.patoolbox.feature.home.HomeScreen
import com.patoolbox.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/**
 * ツール画面への共通ルート。
 * 実装済みのツールは [toolScreen] で実画面に振り分け、未実装は PlaceholderScreen に落とす。
 * Phase が進んで画面が増えても、ホーム側は ToolId を渡すだけで済む。
 */
@Serializable
data class ToolRoute(val toolId: String)

@Serializable
data object SettingsRoute

@Composable
fun PaNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onToolClick = { tool -> navController.navigate(ToolRoute(tool.name)) },
                onSettingsClick = { navController.navigate(SettingsRoute) },
            )
        }

        composable<ToolRoute> { backStackEntry ->
            val route: ToolRoute = backStackEntry.toRoute()
            val tool = ToolId.fromIdOrNull(route.toolId)

            if (tool == null) {
                // 保存済みの favorite などから消えた ID が来た場合
                LaunchedEffect(route.toolId) { navController.popBackStack() }
            } else {
                ToolDestination(
                    tool = tool,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * ToolId → 実画面の振り分け。
 * Phase 1 で :feature:spl などが入ったら、ここに分岐を足していく。
 */
@Composable
private fun ToolDestination(
    tool: ToolId,
    onBack: () -> Unit,
) {
    PlaceholderScreen(tool = tool, onBack = onBack)
}

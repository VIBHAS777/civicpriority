package com.civic.priority.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.civic.priority.data.UserRole
import com.civic.priority.ui.theme.CivicColors
import com.civic.priority.viewmodel.AppViewModel
import androidx.compose.ui.unit.dp

@Composable
fun ContentScreen(viewModel: AppViewModel) {
    val currentUser by viewModel.currentUser

    if (currentUser == null) {
        AuthScreen(viewModel = viewModel)
    } else {
        MainTabScreen(viewModel = viewModel)
    }
}

@Composable
fun MainTabScreen(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val currentUser by viewModel.currentUser
    val role = currentUser?.role ?: UserRole.COMMUNITY

    val mainRoute = when (role) {
        UserRole.COMMUNITY -> "community"
        UserRole.ADMIN -> "admin"
        UserRole.SYSTEM_ADMIN -> "system_admin"
    }

    val mainLabel = when (role) {
        UserRole.COMMUNITY -> "Community"
        UserRole.ADMIN -> "Dashboard"
        UserRole.SYSTEM_ADMIN -> "System"
    }

    val mainIcon = when (role) {
        UserRole.COMMUNITY -> Icons.Default.Groups
        UserRole.ADMIN -> Icons.Default.BarChart
        UserRole.SYSTEM_ADMIN -> Icons.Default.Storage
    }

    Scaffold(
        containerColor = CivicColors.Background,
        bottomBar = {
            NavigationBar(
                containerColor = CivicColors.CardBackground,
                contentColor = CivicColors.TextPrimary
            ) {
                val navBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStack?.destination?.route

                NavigationBarItem(
                    icon = { Icon(mainIcon, contentDescription = mainLabel) },
                    label = { Text(mainLabel) },
                    selected = currentRoute == mainRoute,
                    onClick = {
                        navController.navigate(mainRoute) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CivicColors.Primary,
                        selectedTextColor = CivicColors.Primary,
                        unselectedIconColor = CivicColors.TextSecondary,
                        unselectedTextColor = CivicColors.TextSecondary,
                        indicatorColor = CivicColors.Primary.copy(alpha = 0.12f)
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = currentRoute == "profile",
                    onClick = {
                        navController.navigate("profile") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CivicColors.Primary,
                        selectedTextColor = CivicColors.Primary,
                        unselectedIconColor = CivicColors.TextSecondary,
                        unselectedTextColor = CivicColors.TextSecondary,
                        indicatorColor = CivicColors.Primary.copy(alpha = 0.12f)
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Dynamic glowing background elements
            GlowArc(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .align(Alignment.TopCenter)
            )
            FloatingParticles()

            NavHost(
                navController = navController,
                startDestination = mainRoute,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
            ) {
                composable("community") {
                    CommunityScreen(viewModel = viewModel, navController = navController)
                }
                composable("admin") {
                    AdminScreen(viewModel = viewModel, navController = navController)
                }
                composable("system_admin") {
                    SystemAdminScreen(viewModel = viewModel)
                }
                composable("profile") {
                    ProfileScreen(viewModel = viewModel)
                }
                composable(
                    "issue_detail/{issueId}",
                    arguments = listOf(navArgument("issueId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
                    IssueDetailScreen(viewModel = viewModel, navController = navController, issueId = issueId)
                }
                composable("new_issue") {
                    NewIssueScreen(viewModel = viewModel, navController = navController)
                }
            }
        }
    }
}

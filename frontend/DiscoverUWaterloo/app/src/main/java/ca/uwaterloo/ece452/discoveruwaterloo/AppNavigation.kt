package ca.uwaterloo.ece452.discoveruwaterloo

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ca.uwaterloo.ece452.discoveruwaterloo.data.UserRole
import ca.uwaterloo.ece452.discoveruwaterloo.ui.auth.LoginScreen
import ca.uwaterloo.ece452.discoveruwaterloo.ui.auth.RegisterScreen
import ca.uwaterloo.ece452.discoveruwaterloo.ui.auth.VerifyEmailScreen
import ca.uwaterloo.ece452.discoveruwaterloo.ui.events.EventDetailScreen
import ca.uwaterloo.ece452.discoveruwaterloo.ui.planner.PlannerScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val VERIFY_EMAIL = "verify_email/{email}"
    fun verifyEmail(email: String) = "verify_email/${java.net.URLEncoder.encode(email, "UTF-8")}"
    const val HOME = "home"
    const val MAP = "map"
    const val PLANNER = "planner"
    const val ORGANIZER = "organizer"
    const val ADMIN = "admin"
    const val EVENT_DETAIL = "event_detail/{eventId}"
    fun eventDetail(eventId: Int) = "event_detail/$eventId"
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentUser by viewModel.currentUser.collectAsState()

    // Hide nav chrome on all auth screens
    val isAuthRoute = currentRoute == Routes.LOGIN
        || currentRoute == Routes.REGISTER
        || currentRoute?.startsWith("verify_email/") == true

    val bottomNavItems = remember(currentUser) {
        buildList {
            add(BottomNavItem(Routes.HOME, "Home", Icons.Default.Home))
            add(BottomNavItem(Routes.MAP, "Map", Icons.Default.LocationOn))
            add(BottomNavItem(Routes.PLANNER, "Planner", Icons.Default.DateRange))
            when (currentUser?.role) {
                UserRole.ORGANIZER -> add(BottomNavItem(Routes.ORGANIZER, "Create", Icons.Default.Add))
                UserRole.ADMIN -> add(BottomNavItem(Routes.ADMIN, "Admin", Icons.Default.Settings))
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isAuthRoute) {
                TopAppBar(
                    title = { Text("DiscoverUW") },
                    actions = {
                        IconButton(onClick = {
                            viewModel.logout()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isAuthRoute) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLogin = { email, password, onError ->
                        viewModel.login(email, password,
                            onSuccess = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                            onError = onError
                        )
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegister = { name, email, password, role, onError ->
                        viewModel.register(name, email, password, role,
                            onSuccess = {
                                navController.navigate(Routes.verifyEmail(email)) {
                                    popUpTo(Routes.REGISTER) { inclusive = true }
                                }
                            },
                            onError = onError
                        )
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.VERIFY_EMAIL,
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                val email = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("email") ?: "", "UTF-8"
                )
                VerifyEmailScreen(
                    email = email,
                    onVerify = { code, onError ->
                        viewModel.verifyEmail(email, code,
                            onSuccess = {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onError = onError
                        )
                    },
                    onResend = { onError -> viewModel.resendCode(email, onError) }
                )
            }
            composable(Routes.HOME) {
                ca.uwaterloo.ece452.discoveruwaterloo.ui.events.EventFeedScreen(
                    viewModel = viewModel,
                    onEventClick = { eventId -> navController.navigate(Routes.eventDetail(eventId)) }
                )
            }
            composable(Routes.MAP) { ca.uwaterloo.ece452.discoveruwaterloo.ui.map.MapScreen(viewModel) }
            composable(Routes.PLANNER) {
                PlannerScreen(
                    viewModel = viewModel,
                    onEventClick = { eventId -> navController.navigate(Routes.eventDetail(eventId)) }
                )
            }
            composable(
                route = Routes.EVENT_DETAIL,
                arguments = listOf(navArgument("eventId") { type = NavType.IntType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getInt("eventId") ?: return@composable
                EventDetailScreen(
                    eventId = eventId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.ORGANIZER) {
                ca.uwaterloo.ece452.discoveruwaterloo.ui.organizer.CreateEventScreen(viewModel = viewModel)
            }
            composable(Routes.ADMIN) {
                ca.uwaterloo.ece452.discoveruwaterloo.ui.admin.AdminScreen(viewModel = viewModel)
            }
        }
    }
}

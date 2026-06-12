package ca.uwaterloo.ece452.discoveruwaterloo

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ca.uwaterloo.ece452.discoveruwaterloo.data.UserRole
import ca.uwaterloo.ece452.discoveruwaterloo.ui.auth.LoginScreen
import ca.uwaterloo.ece452.discoveruwaterloo.ui.auth.RegisterScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val MAP = "map"
    const val PLANNER = "planner"
    const val ORGANIZER = "organizer"
    const val ADMIN = "admin"
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val authRoutes = setOf(Routes.LOGIN, Routes.REGISTER)

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentUser by viewModel.currentUser.collectAsState()

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
        bottomBar = {
            if (currentRoute !in authRoutes) {
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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLogin = { email, password ->
                        viewModel.login(email, password,
                            onSuccess = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                            onError = {}
                        )
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegister = { name, email, password, role ->
                        viewModel.register(name, email, password, role,
                            onSuccess = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                            onError = {}
                        )
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Routes.HOME) { Text("Home — Person 3") }
            composable(Routes.MAP) { Text("Map — Person 5") }
            composable(Routes.PLANNER) { Text("Planner — Person 4") }
            composable(Routes.ORGANIZER) { Text("Create Event — Person 6") }
            composable(Routes.ADMIN) { Text("Admin — Person 6") }
        }
    }
}

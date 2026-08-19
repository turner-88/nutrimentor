package com.sebaya.dm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sebaya.dm.ui.AppViewModel
import com.sebaya.dm.ui.ArticleScreen
import com.sebaya.dm.ui.DashboardScreen
import com.sebaya.dm.ui.EducationScreen
import com.sebaya.dm.ui.LeaderboardScreen
import com.sebaya.dm.ui.LoginScreen
import com.sebaya.dm.ui.ProfileScreen
import com.sebaya.dm.ui.RegisterScreen
import com.sebaya.dm.ui.SebayaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as SebayaApp).repository
        setContent {
            SebayaTheme {
                val vm: AppViewModel = viewModel(factory = AppViewModel.Factory(repo))
                RootNav(vm)
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("dashboard", "Beranda", Icons.Filled.Home),
    Tab("education", "Edukasi", Icons.Filled.Book),
    Tab("leaderboard", "Kelompok", Icons.Filled.Leaderboard),
    Tab("profile", "Profil", Icons.Filled.Person),
)

@Composable
private fun RootNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val start = if (vm.loggedIn) "home" else "login"

    NavHost(navController = nav, startDestination = start) {
        composable("login") {
            LoginScreen(vm,
                onLoggedIn = { nav.navigate("home") { popUpTo("login") { inclusive = true } } },
                onRegister = { nav.navigate("register") })
        }
        composable("register") {
            RegisterScreen(vm,
                onDone = { nav.navigate("home") { popUpTo("login") { inclusive = true } } },
                onBack = { nav.popBackStack() })
        }
        composable("home") { HomeShell(vm, rootNav = nav) }
    }
}

@Composable
private fun HomeShell(vm: AppViewModel, rootNav: androidx.navigation.NavController) {
    val tabNav = rememberNavController()
    Scaffold(
        bottomBar = {
            val backStack by tabNav.currentBackStackEntryAsState()
            val current = backStack?.destination
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = current?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            tabNav.navigate(tab.route) {
                                popUpTo(tabNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { pad ->
        NavHost(tabNav, startDestination = "dashboard", modifier = Modifier.padding(pad)) {
            composable("dashboard") { DashboardScreen(vm) }
            composable("education") { EducationScreen(vm, onOpen = { slug -> tabNav.navigate("article/$slug") }) }
            composable("article/{slug}") { entry ->
                ArticleScreen(vm, slug = entry.arguments?.getString("slug").orEmpty(), onBack = { tabNav.popBackStack() })
            }
            composable("leaderboard") { LeaderboardScreen(vm) }
            composable("profile") {
                ProfileScreen(vm, onLoggedOut = {
                    rootNav.navigate("login") { popUpTo("home") { inclusive = true } }
                })
            }
        }
    }
}

package com.sebaya.dm

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sebaya.dm.ui.components.BrandMark
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sebaya.dm.ui.AppViewModel
import com.sebaya.dm.ui.ArticleScreen
import com.sebaya.dm.ui.ChangePasswordScreen
import com.sebaya.dm.ui.DashboardScreen
import com.sebaya.dm.ui.EditProfileScreen
import com.sebaya.dm.ui.EducationScreen
import com.sebaya.dm.ui.ForgotPasswordScreen
import com.sebaya.dm.ui.HistoryScreen
import com.sebaya.dm.ui.LeaderboardScreen
import com.sebaya.dm.ui.LoginScreen
import com.sebaya.dm.ui.ProfileScreen
import com.sebaya.dm.ui.RegisterScreen
import com.sebaya.dm.ui.theme.SebayaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as SebayaApp).repository
        setContent {
            SebayaTheme {
                val vm: AppViewModel = viewModel(factory = AppViewModel.Factory(repo))

                val notifPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* granted or not, push registration still proceeds */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    // Refresh the device token for an already-authenticated session.
                    vm.syncPushTokenIfLoggedIn()
                }

                RootNav(vm)
            }
        }
    }
}

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,          // selected (filled)
    val iconOutline: ImageVector,   // unselected (outlined)
)

private val tabs = listOf(
    Tab("dashboard", "Beranda", Icons.Filled.Home, Icons.Outlined.Home),
    Tab("education", "Edukasi", Icons.Filled.Book, Icons.Outlined.Book),
    Tab("leaderboard", "Kelompok", Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard),
    Tab("profile", "Profil", Icons.Filled.Person, Icons.Outlined.Person),
)

@Composable
private fun RootNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val start = if (vm.loggedIn) "home" else "login"

    NavHost(navController = nav, startDestination = start) {
        composable("login") {
            LoginScreen(vm,
                onLoggedIn = { nav.navigate("home") { popUpTo("login") { inclusive = true } } },
                onRegister = { nav.navigate("register") },
                onForgot = { nav.navigate("forgot") })
        }
        composable("register") {
            RegisterScreen(vm,
                onDone = { nav.navigate("home") { popUpTo("login") { inclusive = true } } },
                onBack = { nav.popBackStack() })
        }
        composable("forgot") {
            ForgotPasswordScreen(vm, onBack = { nav.popBackStack() })
        }
        composable("home") { HomeShell(vm, rootNav = nav) }
    }
}

@Composable
private fun HomeShell(vm: AppViewModel, rootNav: androidx.navigation.NavController) {
    val tabNav = rememberNavController()
    val backStack by tabNav.currentBackStackEntryAsState()
    val current = backStack?.destination
    // The app-name bar shows on the four main tabs; the article detail brings its own bar.
    val showAppBar = tabs.any { tab -> current?.hierarchy?.any { it.route == tab.route } == true }
    Scaffold(
        topBar = { if (showAppBar) HomeTopBar() },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    tabs.forEach { tab ->
                        val selected = current?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                tabNav.navigate(tab.route) {
                                    popUpTo(tabNav.graph.findStartDestination().id)
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) tab.icon else tab.iconOutline,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(tabNav, startDestination = "dashboard", modifier = Modifier.padding(pad).consumeWindowInsets(pad)) {
            composable("dashboard") { DashboardScreen(vm) }
            composable("education") {
                EducationScreen(
                    vm,
                    onOpen = { slug -> tabNav.navigate("article/$slug") },
                    onOpenSingle = { slug ->
                        tabNav.navigate("article/$slug") {
                            popUpTo("education") { inclusive = true }
                        }
                    },
                )
            }
            composable("article/{slug}") { entry ->
                ArticleScreen(vm, slug = entry.arguments?.getString("slug").orEmpty(), onBack = { tabNav.popBackStack() })
            }
            composable("leaderboard") { LeaderboardScreen(vm) }
            composable("profile") {
                ProfileScreen(vm,
                    onLoggedOut = {
                        rootNav.navigate("login") { popUpTo("home") { inclusive = true } }
                    },
                    onEditProfile = { tabNav.navigate("profile/edit") },
                    onChangePassword = { tabNav.navigate("profile/password") },
                    onHistory = { tabNav.navigate("history") })
            }
            composable("profile/edit") {
                EditProfileScreen(vm, onBack = { tabNav.popBackStack() })
            }
            composable("profile/password") {
                ChangePasswordScreen(vm, onBack = { tabNav.popBackStack() })
            }
            composable("history") {
                HistoryScreen(vm, onBack = { tabNav.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar() {
    Column {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(size = 30.dp)
                    Text(
                        "SebayaDM",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

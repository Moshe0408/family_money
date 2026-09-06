package com.familymoney.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.familymoney.AppContainer
import com.familymoney.data.update.ReleaseInfo
import com.familymoney.data.update.UpdateState
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.nav.bottomTabs
import com.familymoney.ui.screens.AccountsScreen
import com.familymoney.ui.screens.AiChatScreen
import com.familymoney.ui.screens.BudgetScreen
import com.familymoney.ui.screens.CardsScreen
import com.familymoney.ui.screens.ChildrenScreen
import com.familymoney.ui.screens.FamilyScreen
import com.familymoney.ui.screens.ForecastScreen
import com.familymoney.ui.screens.GoalsScreen
import com.familymoney.ui.screens.HomeScreen
import com.familymoney.ui.screens.ImportScreen
import com.familymoney.ui.screens.InsightsScreen
import com.familymoney.ui.screens.InvestmentsScreen
import com.familymoney.ui.screens.LiabilitiesScreen
import com.familymoney.ui.screens.LockScreen
import com.familymoney.ui.screens.NetWorthScreen
import com.familymoney.ui.screens.OnboardingScreen
import com.familymoney.ui.screens.PrivacyScreen
import com.familymoney.ui.screens.ProfileScreen
import com.familymoney.ui.screens.RecurringScreen
import com.familymoney.ui.screens.ReportsScreen
import com.familymoney.ui.screens.SettingsScreen
import com.familymoney.ui.screens.SimulatorScreen
import com.familymoney.ui.screens.TransactionsScreen
import com.familymoney.ui.screens.UpdateScreen
import com.familymoney.ui.screens.WalletScreen
import com.familymoney.ui.sheets.AddEntrySheet
import com.familymoney.ui.vm.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(container: AppContainer, pendingInviteCode: String?) {
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(container))
    val navController = rememberNavController()
    val context = LocalContext.current

    var unlocked by remember { mutableStateOf(!container.settings.isLockRequired()) }
    var showAddSheet by remember { mutableStateOf(false) }

    val onboardingDone by vm.onboardingDone.collectAsState()
    val toast by vm.toast.collectAsState()

    LaunchedEffect(Unit) { vm.captureSnapshotIfNeeded() }

    // Section: in-app updates. Checks quietly at most once a day and only
    // surfaces a banner when a newer build is actually published.
    var pendingUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }
    LaunchedEffect(unlocked, onboardingDone) {
        if (!unlocked || !onboardingDone) return@LaunchedEffect
        if (!container.updateManager.configured) return@LaunchedEffect
        if (!container.settings.shouldCheckForUpdate()) return@LaunchedEffect

        container.settings.lastUpdateCheckAt = System.currentTimeMillis()
        val result = container.updateManager.check()
        if (result is UpdateState.Available &&
            result.release.versionCode != container.settings.dismissedUpdateCode
        ) {
            pendingUpdate = result.release
        }
    }

    // A deep-linked invite goes straight to the family screen once unlocked.
    LaunchedEffect(pendingInviteCode, unlocked, onboardingDone) {
        if (pendingInviteCode != null && unlocked && onboardingDone) {
            navController.navigate("${Routes.FAMILY}?code=$pendingInviteCode")
        }
    }

    if (!unlocked) {
        LockScreen(
            settings = container.settings,
            onUnlocked = {
                container.settings.lastUnlockAt = System.currentTimeMillis()
                unlocked = true
            }
        )
        return
    }

    if (!onboardingDone) {
        OnboardingScreen(vm = vm, onFinished = { })
        return
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                BottomBar(navController, currentRoute)
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.HOME || currentRoute == Routes.TRANSACTIONS) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "הוספה")
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AppNavHost(navController, vm, container)

            pendingUpdate?.let { release ->
                UpdateBanner(
                    release = release,
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    onOpen = {
                        pendingUpdate = null
                        navController.navigate(Routes.UPDATE)
                    },
                    onDismiss = {
                        container.settings.dismissedUpdateCode = release.versionCode
                        pendingUpdate = null
                    }
                )
            }

            AnimatedVisibility(
                visible = toast != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            ) {
                Snackbar(
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface
                ) { Text(toast.orEmpty()) }
            }
        }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2600)
            vm.clearToast()
        }
    }

    if (showAddSheet) {
        AddEntrySheet(
            vm = vm,
            onDismiss = { showAddSheet = false },
            onNavigate = { route ->
                showAddSheet = false
                navController.navigate(route)
            }
        )
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        bottomTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(tab.label, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    vm: MainViewModel,
    container: AppContainer
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(vm, navController) }
        composable(Routes.TRANSACTIONS) { TransactionsScreen(vm, navController) }
        composable(Routes.GOALS) { GoalsScreen(vm, navController) }
        composable(Routes.INVESTMENTS) { InvestmentsScreen(vm, navController) }
        composable(Routes.PROFILE) { ProfileScreen(vm, navController, container) }

        composable(Routes.AI) { AiChatScreen(vm, container, navController) }
        composable(Routes.INSIGHTS) { InsightsScreen(vm, navController) }
        composable(Routes.FORECAST) { ForecastScreen(vm, navController) }
        composable(Routes.BUDGET) { BudgetScreen(vm, navController) }
        composable(Routes.REPORTS) { ReportsScreen(vm, navController) }
        composable(Routes.SIMULATOR) { SimulatorScreen(navController) }
        composable(Routes.CHILDREN) { ChildrenScreen(vm, navController) }
        composable(Routes.NET_WORTH) { NetWorthScreen(vm, navController) }
        composable(Routes.RECURRING) { RecurringScreen(vm, navController) }
        composable(Routes.ACCOUNTS) { AccountsScreen(vm, navController) }
        composable(Routes.CARDS) { CardsScreen(vm, navController) }
        composable(Routes.WALLET) { WalletScreen(vm, navController) }
        composable(Routes.SETTINGS) { SettingsScreen(vm, container, navController) }
        composable(Routes.PRIVACY) { PrivacyScreen(vm, container, navController) }
        composable(Routes.IMPORT) { ImportScreen(vm, container, navController) }
        composable(Routes.LIABILITIES) { LiabilitiesScreen(vm, navController) }
        composable(Routes.UPDATE) { UpdateScreen(container, navController) }
        composable("${Routes.FAMILY}?code={code}") { entry ->
            FamilyScreen(vm, container, navController, entry.arguments?.getString("code"))
        }
        composable(Routes.FAMILY) { FamilyScreen(vm, container, navController, null) }
    }
}

/**
 * Slim, dismissible prompt shown on the dashboard when a newer build exists.
 * A mandatory release cannot be dismissed.
 */
@Composable
private fun UpdateBanner(
    release: ReleaseInfo,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 6.dp,
        onClick = onOpen
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (release.mandatory) "⚠️" else "⬆️")
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (release.mandatory) "עדכון חובה זמין" else "גרסה חדשה זמינה",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "גרסה ${release.versionName} · הקישו להתקנה",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (!release.mandatory) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("אחר כך", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** Shared full-bleed gradient used by hero headers. */
@Composable
fun heroBrush(): Brush {
    val dark = com.familymoney.ui.theme.LocalIsDark.current
    return Brush.linearGradient(
        if (dark) listOf(
            com.familymoney.ui.theme.HeroGradientStartDark,
            com.familymoney.ui.theme.HeroGradientMidDark,
            com.familymoney.ui.theme.HeroGradientEndDark
        ) else listOf(
            com.familymoney.ui.theme.HeroGradientStart,
            com.familymoney.ui.theme.HeroGradientMid,
            com.familymoney.ui.theme.HeroGradientEnd
        )
    )
}

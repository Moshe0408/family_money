package com.familymoney.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val GOALS = "goals"
    const val INVESTMENTS = "investments"
    const val PROFILE = "profile"

    const val AI = "ai"
    const val INSIGHTS = "insights"
    const val FORECAST = "forecast"
    const val BUDGET = "budget"
    const val REPORTS = "reports"
    const val SIMULATOR = "simulator"
    const val CHILDREN = "children"
    const val NET_WORTH = "net_worth"
    const val RECURRING = "recurring"
    const val ACCOUNTS = "accounts"
    const val CARDS = "cards"
    const val WALLET = "wallet"
    const val FAMILY = "family"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val IMPORT = "import"
    const val LIABILITIES = "liabilities"
    const val UPDATE = "update"
}

data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

/** Section 37: bottom navigation. */
val bottomTabs = listOf(
    BottomTab(Routes.HOME, "בית", Icons.Filled.Home, Icons.Outlined.Home),
    BottomTab(Routes.TRANSACTIONS, "עסקאות", Icons.Filled.CreditCard, Icons.Outlined.CreditCard),
    BottomTab(Routes.GOALS, "יעדים", Icons.Filled.TrackChanges, Icons.Outlined.TrackChanges),
    BottomTab(Routes.INVESTMENTS, "השקעות", Icons.Filled.ShowChart, Icons.Outlined.ShowChart),
    BottomTab(Routes.PROFILE, "פרופיל", Icons.Filled.Person, Icons.Outlined.Person)
)

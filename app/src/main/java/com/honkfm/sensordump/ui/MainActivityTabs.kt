package com.honkfm.sensordump.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class MainActivityTab(val route: String, val label: String, val icon: ImageVector) {
    object Home : MainActivityTab("home", "Hjem", Icons.Default.Home)
    object Profile : MainActivityTab("profile", "Profil", Icons.Default.Person)
    object Settings : MainActivityTab("settings", "Indstillinger", Icons.Default.Settings)
}
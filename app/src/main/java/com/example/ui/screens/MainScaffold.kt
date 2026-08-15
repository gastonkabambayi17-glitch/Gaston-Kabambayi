package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.ui.components.MatchSuccessDialog
import com.example.ui.components.ReportUserDialog
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.DatingViewModel

enum class MainTab {
    HOME,
    DISCOVER,
    MATCHES,
    MESSAGES,
    PROFILE
}

@Composable
fun MainScaffold(
    currentUser: UserEntity,
    datingViewModel: DatingViewModel,
    adminViewModel: AdminViewModel,
    onLogout: () -> Unit
) {
    val datingState by datingViewModel.uiState.collectAsState()
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var showNotifications by remember { mutableStateOf(false) }
    var showAdminScreen by remember { mutableStateOf(false) }

    // Match Celebration Modal
    datingState.activeMatchDialog?.matchedUser?.let { matchedUser ->
        MatchSuccessDialog(
            currentUser = currentUser,
            matchedUser = matchedUser,
            onSendMessage = {
                val matchId = datingState.activeMatchDialog?.matchId
                datingViewModel.dismissMatchDialog()
                // Find or open chat with this user
                val match = datingState.matches.find { it.otherUser.id == matchedUser.id }
                if (match != null) {
                    datingViewModel.openChat(match)
                }
                currentTab = MainTab.MESSAGES
            },
            onContinueSwiping = {
                datingViewModel.dismissMatchDialog()
                currentTab = MainTab.DISCOVER
            }
        )
    }

    // Report User Dialog
    datingState.userToReport?.let { userToReport ->
        ReportUserDialog(
            user = userToReport,
            onDismiss = { datingViewModel.dismissReportDialog() },
            onSubmitReport = { reason, details ->
                datingViewModel.submitReport(reason, details)
            }
        )
    }

    if (showAdminScreen) {
        AdminScreen(
            adminViewModel = adminViewModel,
            onBack = { showAdminScreen = false }
        )
    } else if (showNotifications) {
        NotificationsScreen(
            datingViewModel = datingViewModel,
            onBack = { showNotifications = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    // Home
                    NavigationBarItem(
                        selected = (currentTab == MainTab.HOME),
                        onClick = { currentTab = MainTab.HOME },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Accueil"
                            )
                        },
                        label = { Text("Accueil", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrimsonPrimary,
                            indicatorColor = CrimsonPrimary.copy(alpha = 0.15f)
                        )
                    )

                    // Discover
                    NavigationBarItem(
                        selected = (currentTab == MainTab.DISCOVER),
                        onClick = { currentTab = MainTab.DISCOVER },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.DISCOVER) Icons.Filled.LocalFireDepartment else Icons.Outlined.LocalFireDepartment,
                                contentDescription = "Découvrir"
                            )
                        },
                        label = { Text("Découvrir", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.DISCOVER) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrimsonPrimary,
                            indicatorColor = CrimsonPrimary.copy(alpha = 0.15f)
                        )
                    )

                    // Matches
                    NavigationBarItem(
                        selected = (currentTab == MainTab.MATCHES),
                        onClick = { currentTab = MainTab.MATCHES },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (datingState.matches.isNotEmpty()) {
                                        Badge(containerColor = CrimsonPrimary, contentColor = Color.White) {
                                            Text("${datingState.matches.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentTab == MainTab.MATCHES) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Matchs"
                                )
                            }
                        },
                        label = { Text("Matchs", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.MATCHES) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrimsonPrimary,
                            indicatorColor = CrimsonPrimary.copy(alpha = 0.15f)
                        )
                    )

                    // Messages
                    NavigationBarItem(
                        selected = (currentTab == MainTab.MESSAGES),
                        onClick = { currentTab = MainTab.MESSAGES },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.MESSAGES) Icons.Filled.Forum else Icons.Outlined.Forum,
                                contentDescription = "Messages"
                            )
                        },
                        label = { Text("Messages", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.MESSAGES) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrimsonPrimary,
                            indicatorColor = CrimsonPrimary.copy(alpha = 0.15f)
                        )
                    )

                    // Profile
                    NavigationBarItem(
                        selected = (currentTab == MainTab.PROFILE),
                        onClick = { currentTab = MainTab.PROFILE },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "Profil"
                            )
                        },
                        label = { Text("Profil", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.PROFILE) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrimsonPrimary,
                            indicatorColor = CrimsonPrimary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    MainTab.HOME -> HomeScreen(
                        currentUser = currentUser,
                        datingViewModel = datingViewModel,
                        onNavigateToDiscover = { currentTab = MainTab.DISCOVER },
                        onNavigateToMatches = { currentTab = MainTab.MATCHES },
                        onNavigateToMessages = { currentTab = MainTab.MESSAGES },
                        onNavigateToNotifications = { showNotifications = true },
                        onOpenProfile = { currentTab = MainTab.PROFILE }
                    )
                    MainTab.DISCOVER -> DiscoverScreen(
                        datingViewModel = datingViewModel,
                        currentUser = currentUser
                    )
                    MainTab.MATCHES -> MatchesScreen(
                        datingViewModel = datingViewModel,
                        onOpenChat = { match ->
                            datingViewModel.openChat(match)
                            currentTab = MainTab.MESSAGES
                        },
                        onNavigateToDiscover = { currentTab = MainTab.DISCOVER }
                    )
                    MainTab.MESSAGES -> MessagesScreen(
                        datingViewModel = datingViewModel,
                        currentUser = currentUser,
                        onNavigateToDiscover = { currentTab = MainTab.DISCOVER }
                    )
                    MainTab.PROFILE -> ProfileScreen(
                        currentUser = currentUser,
                        datingViewModel = datingViewModel,
                        onLogout = onLogout,
                        onOpenAdmin = { showAdminScreen = true }
                    )
                }
            }
        }
    }
}

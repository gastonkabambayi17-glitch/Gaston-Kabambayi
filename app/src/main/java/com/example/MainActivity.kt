package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.GastonLoveDatabase
import com.example.data.repository.GastonLoveRepository
import com.example.ui.screens.*
import com.example.ui.theme.GastonLoveTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AdminViewModelFactory
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.AuthViewModelFactory
import com.example.ui.viewmodel.DatingViewModel
import com.example.ui.viewmodel.DatingViewModelFactory

enum class AppDestination {
    LANDING,
    LOGIN,
    REGISTER,
    MAIN_APP,
    STANDALONE_ADMIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = GastonLoveDatabase.getDatabase(applicationContext)
        val repository = GastonLoveRepository(database.dao())
        val firebaseAuthService = com.example.data.auth.FirebaseAuthService(applicationContext)

        setContent {
            GastonLoveTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModelFactory(repository, firebaseAuthService)
                    )
                    val datingViewModel: DatingViewModel = viewModel(
                        factory = DatingViewModelFactory(repository)
                    )
                    val adminViewModel: AdminViewModel = viewModel(
                        factory = AdminViewModelFactory(repository)
                    )

                    val authState by authViewModel.uiState.collectAsState()
                    var destination by remember { mutableStateOf(AppDestination.LANDING) }

                    // Sync logged-in user with DatingViewModel
                    LaunchedEffect(authState.currentUser) {
                        authState.currentUser?.let { user ->
                            datingViewModel.setCurrentUser(user)
                            destination = AppDestination.MAIN_APP
                        }
                    }

                    when (destination) {
                        AppDestination.LANDING -> {
                            LandingScreen(
                                onNavigateToRegister = { destination = AppDestination.REGISTER },
                                onNavigateToLogin = { destination = AppDestination.LOGIN },
                                onQuickDemo = {
                                    authViewModel.login("sophie.martin@example.com", "secret123")
                                },
                                onQuickAdminDemo = {
                                    destination = AppDestination.STANDALONE_ADMIN
                                }
                            )
                        }
                        AppDestination.LOGIN -> {
                            AuthScreen(
                                authViewModel = authViewModel,
                                initialMode = AuthMode.LOGIN,
                                onBackToLanding = { destination = AppDestination.LANDING }
                            )
                        }
                        AppDestination.REGISTER -> {
                            AuthScreen(
                                authViewModel = authViewModel,
                                initialMode = AuthMode.REGISTER,
                                onBackToLanding = { destination = AppDestination.LANDING }
                            )
                        }
                        AppDestination.STANDALONE_ADMIN -> {
                            AdminScreen(
                                adminViewModel = adminViewModel,
                                onBack = { destination = AppDestination.LANDING }
                            )
                        }
                        AppDestination.MAIN_APP -> {
                            val user = authState.currentUser
                            if (user != null) {
                                MainScaffold(
                                    currentUser = user,
                                    datingViewModel = datingViewModel,
                                    adminViewModel = adminViewModel,
                                    onLogout = {
                                        authViewModel.logout()
                                        destination = AppDestination.LANDING
                                    }
                                )
                            } else {
                                destination = AppDestination.LANDING
                            }
                        }
                    }
                }
            }
        }
    }
}


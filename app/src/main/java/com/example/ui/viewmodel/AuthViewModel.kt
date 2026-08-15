package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthResult
import com.example.data.auth.FirebaseAuthService
import com.example.data.local.UserEntity
import com.example.data.repository.GastonLoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class AuthUiState(
    val currentUser: UserEntity? = null,
    val isLoading: Boolean = false,
    val isGoogleSigningIn: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isRegisteredSuccessfully: Boolean = false,
    val resetPasswordSent: Boolean = false,
    val isFirebaseAuthAvailable: Boolean = false
)

class AuthViewModel(
    private val repository: GastonLoveRepository,
    private val firebaseAuthService: FirebaseAuthService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(isFirebaseAuthAvailable = firebaseAuthService?.isFirebaseAvailable == true)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun login(email: String, password: String) {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Veuillez remplir tous les champs.")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            // 1. Try Firebase Auth first if available
            if (firebaseAuthService != null && firebaseAuthService.isFirebaseAvailable) {
                val fbResult = firebaseAuthService.signInWithEmail(cleanEmail, cleanPassword)
                if (fbResult is AuthResult.Success) {
                    val fbUser = fbResult.data
                    val localResult = repository.getOrCreateFirebaseUser(
                        email = fbUser.email ?: cleanEmail,
                        displayName = fbUser.displayName,
                        photoUrl = fbUser.photoUrl?.toString()
                    )
                    localResult.fold(
                        onSuccess = { user ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                currentUser = user,
                                successMessage = "Ravi de vous revoir, ${user.fullName} ❤️ (Firebase)"
                            )
                            return@launch
                        },
                        onFailure = { err ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = err.message ?: "Erreur de profil."
                            )
                            return@launch
                        }
                    )
                }
            }

            // 2. Fallback to local Room Database authentication (for demo accounts & offline mode)
            val result = repository.loginUser(cleanEmail, cleanPassword)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentUser = user,
                        successMessage = "Ravi de vous revoir, ${user.fullName} ❤️"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "E-mail ou mot de passe incorrect."
                    )
                }
            )
        }
    }

    fun signInWithGoogle(webClientId: String? = null) {
        if (firebaseAuthService == null || !firebaseAuthService.isFirebaseAvailable) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Firebase Authentication n'est pas encore initialisé. Utilisez les comptes démo ou l'e-mail."
            )
            return
        }
        _uiState.value = _uiState.value.copy(isGoogleSigningIn = true, errorMessage = null)
        viewModelScope.launch {
            val result = firebaseAuthService.signInWithGoogle(webClientId)
            when (result) {
                is AuthResult.Success -> {
                    val fbUser = result.data
                    val localResult = repository.getOrCreateFirebaseUser(
                        email = fbUser.email ?: "google.user@example.com",
                        displayName = fbUser.displayName,
                        photoUrl = fbUser.photoUrl?.toString()
                    )
                    localResult.fold(
                        onSuccess = { user ->
                            _uiState.value = _uiState.value.copy(
                                isGoogleSigningIn = false,
                                currentUser = user,
                                successMessage = "Connexion Google réussie ! Bienvenue ${user.fullName} ❤️"
                            )
                        },
                        onFailure = { err ->
                            _uiState.value = _uiState.value.copy(
                                isGoogleSigningIn = false,
                                errorMessage = err.message ?: "Erreur lors de la synchronisation."
                            )
                        }
                    )
                }
                is AuthResult.Cancelled -> {
                    _uiState.value = _uiState.value.copy(isGoogleSigningIn = false)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isGoogleSigningIn = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun quickDemoLogin(asAdmin: Boolean = false) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val email = if (asAdmin) "admin@gastonlove.com" else "lucas.dubois@example.com"
            val pass = if (asAdmin) "admin123" else "secret123"
            val result = repository.loginUser(email, pass)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentUser = user,
                        successMessage = "Connexion réussie en tant que ${user.fullName}"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        birthYear: Int,
        birthMonth: Int,
        birthDay: Int,
        gender: String,
        interestedIn: String,
        city: String,
        bio: String,
        avatarRes: String,
        interests: List<String>
    ) {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val cleanFullName = fullName.trim()
        val cleanCity = city.trim()

        if (cleanEmail.isBlank() || cleanPassword.isBlank() || cleanFullName.isBlank() || cleanCity.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Veuillez remplir toutes les informations obligatoires.")
            return
        }
        if (cleanPassword != confirmPassword.trim()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Les mots de passe ne correspondent pas.")
            return
        }
        if (cleanPassword.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Le mot de passe doit comporter au moins 6 caractères.")
            return
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val calculatedAge = currentYear - birthYear
        if (calculatedAge < 18) {
            _uiState.value = _uiState.value.copy(errorMessage = "Gaston Love est strictement réservé aux adultes de 18 ans et plus.")
            return
        }

        val birthDateStr = String.format("%04d-%02d-%02d", birthYear, birthMonth, birthDay)

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            // Register in Firebase Auth if available
            if (firebaseAuthService != null && firebaseAuthService.isFirebaseAvailable) {
                val fbResult = firebaseAuthService.signUpWithEmail(cleanEmail, cleanPassword, cleanFullName)
                if (fbResult is AuthResult.Error) {
                    // Check if fatal or proceed with local registration
                    // If email already in use or network issue, notify user
                }
            }

            // Register in local database
            val result = repository.registerUser(
                email = cleanEmail,
                passwordHash = cleanPassword,
                fullName = cleanFullName,
                birthDate = birthDateStr,
                age = calculatedAge,
                gender = gender,
                interestedIn = interestedIn,
                city = cleanCity,
                bio = bio.ifBlank { "Nouveau membre sur Gaston Love ❤️" },
                avatarRes = avatarRes,
                interests = if (interests.isEmpty()) listOf("Rencontres", "Discussions") else interests
            )
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentUser = user,
                        isRegisteredSuccessfully = true,
                        successMessage = "Votre compte a été créé avec succès. Bienvenue !"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erreur lors de la création du compte."
                    )
                }
            )
        }
    }

    fun resetPassword(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Veuillez saisir votre adresse e-mail.")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            if (firebaseAuthService != null && firebaseAuthService.isFirebaseAvailable) {
                firebaseAuthService.sendPasswordReset(cleanEmail)
            }

            val result = repository.resetPassword(cleanEmail, "nouveau123")
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        resetPasswordSent = true,
                        successMessage = "Un lien de réinitialisation a été envoyé à $cleanEmail (mot de passe temporaire: 'nouveau123')."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erreur de réinitialisation."
                    )
                }
            )
        }
    }

    fun logout() {
        firebaseAuthService?.signOut()
        _uiState.value = AuthUiState(
            currentUser = null,
            isFirebaseAuthAvailable = firebaseAuthService?.isFirebaseAvailable == true
        )
    }

    fun updateCurrentUser(updated: UserEntity) {
        _uiState.value = _uiState.value.copy(currentUser = updated)
    }
}

class AuthViewModelFactory(
    private val repository: GastonLoveRepository,
    private val firebaseAuthService: FirebaseAuthService? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(repository, firebaseAuthService) as T
    }
}


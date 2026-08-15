package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : AuthResult<Nothing>()
    data object Cancelled : AuthResult<Nothing>()
}

class FirebaseAuthService(private val context: Context) {

    private val tag = "FirebaseAuthService"

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(tag, "Firebase is not yet initialized or google-services.json is missing: ${e.message}")
            null
        }
    }

    val currentUser: FirebaseUser?
        get() = firebaseAuth?.currentUser

    val isFirebaseAvailable: Boolean
        get() = firebaseAuth != null

    suspend fun signInWithEmail(email: String, pass: String): AuthResult<FirebaseUser> {
        val auth = firebaseAuth ?: return AuthResult.Error("Firebase n'est pas initialisé sur cet appareil.")
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Échec de la connexion. Utilisateur introuvable.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Email sign-in failed", e)
            AuthResult.Error(e.localizedMessage ?: "Erreur de connexion Firebase.", e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, displayName: String): AuthResult<FirebaseUser> {
        val auth = firebaseAuth ?: return AuthResult.Error("Firebase n'est pas initialisé sur cet appareil.")
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user
            if (user != null) {
                try {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdate).await()
                } catch (pe: Exception) {
                    Log.w(tag, "Profile display name update non-fatal error: ${pe.message}")
                }
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Échec de l'inscription. Compte non créé.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Sign-up failed", e)
            AuthResult.Error(e.localizedMessage ?: "Erreur lors de l'inscription Firebase.", e)
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult<Unit> {
        val auth = firebaseAuth ?: return AuthResult.Error("Firebase n'est pas initialisé.")
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Password reset failed", e)
            AuthResult.Error(e.localizedMessage ?: "Impossible d'envoyer l'e-mail de réinitialisation.", e)
        }
    }

    suspend fun signInWithGoogle(webClientId: String? = null): AuthResult<FirebaseUser> {
        val auth = firebaseAuth ?: return AuthResult.Error("Firebase n'est pas disponible.")
        val credentialManager = CredentialManager.create(context)

        return try {
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOptionBuilder = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)

            // If web client ID provided, configure it
            val serverClientId = webClientId?.ifBlank { null }
                ?: "dummy-web-client-id.apps.googleusercontent.com"
            googleIdOptionBuilder.setServerClientId(serverClientId)

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOptionBuilder.build())
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Impossible de récupérer l'utilisateur Google.")
                }
            } else {
                AuthResult.Error("Type d'identifiant Google inattendu.")
            }
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.e(tag, "CredentialManager error: ${e.message}", e)
            AuthResult.Error("Connexion Google annulée ou indisponible: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(tag, "Google sign-in exception: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Erreur de connexion avec Google.", e)
        }
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e(tag, "Sign out error", e)
        }
    }
}

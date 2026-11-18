package com.ucompensar.ecomarket.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.AppException
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.PRIVACY_POLICY_VERSION
import com.ucompensar.ecomarket.data.model.Role
import com.ucompensar.ecomarket.data.model.UserProfile
import kotlinx.coroutines.tasks.await

/**
 * Autenticación con Firebase Authentication (correo y contraseña).
 * Tras autenticar, carga el perfil de Firestore para conocer el rol del usuario.
 */
object AuthRepository {

    private val auth get() = Firebase.auth

    /** Inicia sesión y devuelve el perfil activo; falla si la cuenta fue eliminada o suspendida. */
    suspend fun signIn(email: String, password: String): UserProfile {
        auth.signInWithEmailAndPassword(email, password).await()
        return loadActiveProfile()
    }

    /**
     * Registra una cuenta nueva de comprador o vendedor y crea su perfil en Firestore.
     * El rol de administrador no se puede elegir al registrarse (lo asigna otro administrador).
     */
    suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: Role,
        business: String
    ): UserProfile {
        require(role != Role.ADMIN) { "El rol administrador no se puede autoasignar" }
        val user = checkNotNull(auth.createUserWithEmailAndPassword(email, password).await().user)
        user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        val profile = UserProfile(
            uid = user.uid, name = name, email = email, phone = phone, role = role,
            business = if (role == Role.SELLER) business else "",
            // Se conserva la fecha y la versión de la política aceptada como prueba de la autorización
            privacyAcceptedAt = java.util.Date(), privacyPolicyVersion = PRIVACY_POLICY_VERSION
        )
        FirestorePaths.users().document(user.uid).set(profile).await()
        Session.profile = profile.copy(uid = user.uid)
        return Session.current
    }

    /**
     * Lee `users/{uid}` del usuario autenticado. Si no existe o está suspendido, cierra la sesión
     * de Firebase y lanza un error para que la pantalla de login lo muestre.
     */
    suspend fun loadActiveProfile(): UserProfile {
        val uid = auth.currentUser?.uid ?: throw AppException(R.string.error_not_signed_in)
        val profile = FirestorePaths.users().document(uid).get().await().toObject(UserProfile::class.java)
        when {
            profile == null -> {
                auth.signOut()
                throw AppException(R.string.error_profile_missing)
            }
            !profile.active -> {
                auth.signOut()
                throw AppException(R.string.error_account_suspended)
            }
        }
        Session.profile = profile
        return profile!!
    }

    /** Envía el correo de restablecimiento de contraseña de Firebase (enlace de un solo uso). */
    suspend fun sendPasswordReset(email: String) {
        auth.setLanguageCode("es")
        auth.sendPasswordResetEmail(email).await()
    }

    /** Cambia la contraseña validando primero la actual (reautenticación exigida por Firebase). */
    suspend fun changePassword(current: String, newPassword: String) {
        val user = auth.currentUser ?: throw AppException(R.string.error_not_signed_in)
        val credential = EmailAuthProvider.getCredential(checkNotNull(user.email), current)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }

    /** Cierra la sesión en Firebase y borra el perfil en memoria. */
    fun signOut() {
        auth.signOut()
        Session.clear()
    }
}

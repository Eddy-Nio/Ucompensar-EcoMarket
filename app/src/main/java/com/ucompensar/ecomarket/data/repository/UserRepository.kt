package com.ucompensar.ecomarket.data.repository

import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.dataObjects
import com.ucompensar.ecomarket.BuildConfig
import com.ucompensar.ecomarket.EcoMarketApp
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.Role
import com.ucompensar.ecomarket.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** CRUD de usuarios sobre la colección `users` de Firestore. */
object UserRepository {

    private const val ADMIN_HELPER_APP = "admin-helper"

    /** Todos los usuarios en tiempo real, ordenados por nombre (solo lo permite el rol administrador). */
    fun observeAll(): Flow<List<UserProfile>> =
        FirestorePaths.users().dataObjects<UserProfile>().map { list -> list.sortedBy { it.name.lowercase() } }

    /** Lee un perfil una sola vez. */
    suspend fun get(uid: String): UserProfile? =
        FirestorePaths.users().document(uid).get().await().toObject(UserProfile::class.java)

    /** Actualiza los datos editables del propio perfil (no rol, estado ni correo). */
    suspend fun updateOwnProfile(name: String, phone: String, business: String) {
        val current = Session.current
        FirestorePaths.users().document(current.uid)
            .update(mapOf("name" to name, "phone" to phone, "business" to business)).await()
        Session.profile = current.copy(name = name, phone = phone, business = business)
    }

    /**
     * Crea un usuario desde el panel del administrador.
     *
     * Firebase cambia la sesión al usuario creado si se usa la instancia principal de Auth, así que
     * la cuenta se crea con una instancia secundaria de Firebase y luego se cierra esa sesión. Así
     * el administrador sigue autenticado.
     */
    suspend fun createByAdmin(profile: UserProfile, password: String): UserProfile {
        val helperAuth = Firebase.auth(helperApp())
        val uid = checkNotNull(helperAuth.createUserWithEmailAndPassword(profile.email, password).await().user).uid
        helperAuth.signOut()
        val created = profile.copy(uid = uid)
        FirestorePaths.users().document(uid).set(created).await()
        return created
    }

    /** Actualiza un usuario existente desde el panel del administrador (incluye rol y estado). */
    suspend fun updateByAdmin(profile: UserProfile) {
        FirestorePaths.users().document(profile.uid).update(
            mapOf(
                "name" to profile.name,
                "phone" to profile.phone,
                "role" to profile.role.name,
                "active" to profile.active,
                "business" to if (profile.role == Role.SELLER) profile.business else ""
            )
        ).await()
    }

    /**
     * Elimina el perfil de Firestore. Sin perfil, la cuenta de Auth ya no puede entrar a la app
     * (AuthRepository.loadActiveProfile la rechaza). Borrar la credencial de Auth requiere el
     * Admin SDK en un servidor, fuera del alcance de una app cliente.
     */
    suspend fun delete(uid: String) {
        FirestorePaths.users().document(uid).delete().await()
        FirestorePaths.cart(uid).get().await().documents.forEach { it.reference.delete() }
    }

    /** Instancia secundaria de Firebase (reutilizada) para crear cuentas sin perder la sesión. */
    private fun helperApp(): FirebaseApp {
        FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext)
            .firstOrNull { it.name == ADMIN_HELPER_APP }?.let { return it }
        val app = FirebaseApp.initializeApp(
            FirebaseApp.getInstance().applicationContext,
            FirebaseApp.getInstance().options,
            ADMIN_HELPER_APP
        )
        if (BuildConfig.USE_FIREBASE_EMULATOR) EcoMarketApp.connectToEmulators(app)
        return app
    }
}

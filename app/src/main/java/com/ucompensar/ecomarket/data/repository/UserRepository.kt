package com.ucompensar.ecomarket.data.repository

import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.dataObjects
import com.ucompensar.ecomarket.BuildConfig
import com.ucompensar.ecomarket.EcoMarketApp
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.AppException
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.AuditAction
import com.ucompensar.ecomarket.data.model.AuditLog
import com.ucompensar.ecomarket.data.model.Role
import com.ucompensar.ecomarket.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * CRUD de usuarios sobre la colección `users` de Firestore.
 *
 * Criterios de seguridad en la asignación de roles (reforzados también en las reglas de Firestore):
 * - El rol de administrador nunca se autoasigna: solo otro administrador lo concede.
 * - Un administrador no puede cambiar su propio rol, suspenderse ni eliminarse.
 * - Siempre debe quedar al menos un administrador activo.
 * - Cada creación, cambio de rol, suspensión o eliminación se registra en la bitácora `audit_logs`.
 */
object UserRepository {

    private const val ADMIN_HELPER_APP = "admin-helper"
    private const val RECENT_AUDIT = 6L

    /** Todos los usuarios en tiempo real, ordenados por nombre (solo lo permite el rol administrador). */
    fun observeAll(): Flow<List<UserProfile>> =
        FirestorePaths.users().dataObjects<UserProfile>().map { list -> list.sortedBy { it.name.lowercase() } }

    /** Últimas acciones administrativas, de la más reciente a la más antigua. */
    fun observeRecentAudit(): Flow<List<AuditLog>> =
        FirestorePaths.auditLogs().orderBy("createdAt", Query.Direction.DESCENDING).limit(RECENT_AUDIT).dataObjects<AuditLog>()

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
        val batch = FirestorePaths.db.batch()
        batch.set(FirestorePaths.users().document(uid), created)
        audit(batch, AuditAction.USER_CREATED, created, toRole = created.role)
        batch.commit().await()
        return created
    }

    /**
     * Actualiza un usuario desde el panel del administrador (incluye rol y estado).
     * Valida los criterios de seguridad y deja en la bitácora cada cambio sensible, en el mismo lote.
     */
    suspend fun updateByAdmin(before: UserProfile, after: UserProfile) {
        val actor = Session.current
        val roleChanged = before.role != after.role
        val statusChanged = before.active != after.active
        if (before.uid == actor.uid && (roleChanged || statusChanged)) throw AppException(R.string.error_self_role_change)
        if (before.role == Role.ADMIN && (after.role != Role.ADMIN || !after.active)) ensureAnotherActiveAdmin(before.uid)

        val batch = FirestorePaths.db.batch()
        batch.update(
            FirestorePaths.users().document(after.uid), mapOf(
                "name" to after.name,
                "phone" to after.phone,
                "role" to after.role.name,
                "active" to after.active,
                "business" to if (after.role == Role.SELLER) after.business else ""
            )
        )
        if (roleChanged) audit(batch, AuditAction.ROLE_CHANGED, after, fromRole = before.role, toRole = after.role)
        if (statusChanged) audit(batch, if (after.active) AuditAction.USER_REACTIVATED else AuditAction.USER_SUSPENDED, after)
        batch.commit().await()
    }

    /**
     * Elimina el perfil de Firestore y su carrito. Sin perfil, la cuenta de Auth ya no puede entrar a
     * la app (AuthRepository.loadActiveProfile la rechaza). Borrar la credencial de Auth requiere el
     * Admin SDK en un servidor, fuera del alcance de una app cliente.
     */
    suspend fun delete(user: UserProfile) {
        if (user.uid == Session.current.uid) throw AppException(R.string.error_delete_self)
        if (user.role == Role.ADMIN) ensureAnotherActiveAdmin(user.uid)
        val batch = FirestorePaths.db.batch()
        batch.delete(FirestorePaths.users().document(user.uid))
        FirestorePaths.cart(user.uid).get().await().documents.forEach { batch.delete(it.reference) }
        audit(batch, AuditAction.USER_DELETED, user, fromRole = user.role)
        batch.commit().await()
    }

    /** Impide dejar el sistema sin administradores activos. */
    private suspend fun ensureAnotherActiveAdmin(excludingUid: String) {
        val admins = FirestorePaths.users().whereEqualTo("role", Role.ADMIN.name).whereEqualTo("active", true)
            .get().await().documents.count { it.id != excludingUid }
        if (admins == 0) throw AppException(R.string.error_last_admin)
    }

    /** Agrega al lote una entrada de bitácora firmada con el administrador que ejecuta la acción. */
    private fun audit(batch: WriteBatch, action: AuditAction, target: UserProfile, fromRole: Role? = null, toRole: Role? = null) {
        val actor = Session.current
        batch.set(
            FirestorePaths.auditLogs().document(),
            AuditLog(
                action = action, actorId = actor.uid, actorName = actor.name,
                targetId = target.uid, targetName = target.name,
                fromRole = fromRole?.name.orEmpty(), toRole = toRole?.name.orEmpty()
            )
        )
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

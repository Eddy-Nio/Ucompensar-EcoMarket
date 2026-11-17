package com.ucompensar.ecomarket.data

import com.ucompensar.ecomarket.data.model.UserProfile

/**
 * Perfil del usuario autenticado durante la ejecución de la app.
 * Se llena al iniciar sesión y se limpia al cerrarla; las pantallas leen de aquí el rol y el nombre.
 */
object Session {
    var profile: UserProfile? = null

    /** Perfil actual; las pantallas internas solo se abren con sesión activa. */
    val current: UserProfile get() = checkNotNull(profile) { "No hay sesión activa" }

    fun clear() {
        profile = null
    }
}

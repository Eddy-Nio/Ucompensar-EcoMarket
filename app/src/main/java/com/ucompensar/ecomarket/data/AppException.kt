package com.ucompensar.ecomarket.data

import androidx.annotation.StringRes

/** Error de negocio con un mensaje traducible para mostrar al usuario. */
class AppException(@StringRes val messageRes: Int, vararg val args: Any) : Exception()

package com.ucompensar.ecomarket.util

import android.content.Context
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.AppException
import java.io.IOException

/** Traduce las excepciones de Firebase y de red a mensajes claros en español para el usuario. */
fun Throwable.toUserMessage(context: Context): String = when (this) {
    is AppException -> context.getString(messageRes, *args)
    is FirebaseAuthWeakPasswordException -> context.getString(R.string.error_weak_password)
    is FirebaseAuthUserCollisionException -> context.getString(R.string.error_email_in_use)
    is FirebaseAuthInvalidUserException -> context.getString(R.string.error_wrong_credentials)
    is FirebaseAuthInvalidCredentialsException -> context.getString(R.string.error_wrong_credentials)
    is FirebaseNetworkException, is IOException -> context.getString(R.string.error_network)
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> context.getString(R.string.error_permission)
        FirebaseFirestoreException.Code.UNAVAILABLE -> context.getString(R.string.error_network)
        else -> context.getString(R.string.error_generic)
    }
    else -> localizedMessage ?: context.getString(R.string.error_generic)
}

package com.ucompensar.ecomarket.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda las credenciales del usuario cifradas con una llave del Android Keystore que solo se
 * puede usar después de autenticarse con huella (BiometricPrompt + CryptoObject).
 *
 * Así, el inicio de sesión biométrico es real: la huella desbloquea la llave, se descifran el
 * correo y la contraseña, y con ellos se inicia sesión en Firebase. Cerrar sesión no borra estas
 * credenciales; desactivar la huella en el perfil sí.
 */
class BiometricCredentialStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Hay credenciales guardadas y se puede ofrecer el botón de huella. */
    fun hasCredentials(): Boolean = prefs.contains(KEY_DATA) && prefs.contains(KEY_IV)

    /** Correo asociado a la huella (no está cifrado; solo sirve para mostrarlo). */
    fun savedEmail(): String? = prefs.getString(KEY_EMAIL, null)

    /** Cipher listo para cifrar; se entrega a BiometricPrompt para que la huella lo autorice. */
    fun encryptCipher(): Cipher = Cipher.getInstance(TRANSFORMATION).apply {
        init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    }

    /**
     * Cipher listo para descifrar, o null si la llave fue invalidada (por ejemplo, se registró una
     * huella nueva en el teléfono). En ese caso se borran los datos y se pide la contraseña.
     */
    fun decryptCipher(): Cipher? {
        val iv = prefs.getString(KEY_IV, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        return try {
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            clear()
            null
        }
    }

    /** Cifra y guarda las credenciales con un Cipher ya autorizado por la huella. */
    fun save(authorizedCipher: Cipher, email: String, password: String) {
        val payload = "$email\n$password".toByteArray(Charsets.UTF_8)
        val encrypted = authorizedCipher.doFinal(payload)
        prefs.edit()
            .putString(KEY_DATA, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(authorizedCipher.iv, Base64.NO_WRAP))
            .putString(KEY_EMAIL, email)
            .apply()
    }

    /** Descifra las credenciales con un Cipher ya autorizado por la huella. */
    fun read(authorizedCipher: Cipher): Pair<String, String> {
        val data = Base64.decode(prefs.getString(KEY_DATA, ""), Base64.NO_WRAP)
        val (email, password) = String(authorizedCipher.doFinal(data), Charsets.UTF_8).split("\n", limit = 2)
        return email to password
    }

    /** Borra las credenciales y la llave (desactiva el ingreso con huella). */
    fun clear() {
        prefs.edit().clear().apply()
        runCatching { keyStore().deleteEntry(KEY_ALIAS) }
    }

    /** Obtiene la llave AES del Keystore o la crea exigiendo autenticación biométrica para usarla. */
    private fun getOrCreateKey(): SecretKey {
        (keyStore().getKey(KEY_ALIAS, null) as SecretKey?)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                }
            }
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(spec)
            generateKey()
        }
    }

    private fun keyStore() = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private companion object {
        const val PREFS = "biometric_credentials"
        const val KEY_DATA = "data"
        const val KEY_IV = "iv"
        const val KEY_EMAIL = "email"
        const val KEY_ALIAS = "ecomarket_biometric_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val KEY_SIZE = 256
    }
}

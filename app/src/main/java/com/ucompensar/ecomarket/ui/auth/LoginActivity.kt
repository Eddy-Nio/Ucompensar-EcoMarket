package com.ucompensar.ecomarket.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.repository.AuthRepository
import com.ucompensar.ecomarket.databinding.ActivityLoginBinding
import com.ucompensar.ecomarket.security.BiometricAuth
import com.ucompensar.ecomarket.security.BiometricCredentialStore
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.util.isValidEmail
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toast

/**
 * Inicio de sesión con Firebase Authentication.
 *
 * Ofrece dos caminos: correo y contraseña, o huella dactilar si el usuario la activó antes.
 * El rol (comprador, vendedor o administrador) se lee del perfil en Firestore, no se elige aquí.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialStore: BiometricCredentialStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        credentialStore = BiometricCredentialStore(this)

        binding.btnLogin.setOnClickListener { loginWithPassword() }
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_DONE).also { if (it) loginWithPassword() }
        }
        binding.btnBiometric.setOnClickListener { loginWithBiometrics() }
        binding.txtForgot.setOnClickListener { startActivity(Intent(this, RecoverPasswordActivity::class.java)) }
        binding.txtRegister.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        // El botón de huella solo aparece si hay credenciales guardadas y huellas en el dispositivo
        val biometricReady = credentialStore.hasCredentials() && BiometricAuth.isAvailable(this)
        binding.btnBiometric.show(biometricReady)
        if (biometricReady && binding.etEmail.text.isNullOrBlank()) {
            binding.etEmail.setText(credentialStore.savedEmail())
        }
    }

    /** Valida el formulario e inicia sesión; después ofrece activar la huella. */
    private fun loginWithPassword() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        when {
            email.isEmpty() -> binding.tilEmail.error = getString(R.string.error_empty_fields)
            !email.isValidEmail() -> binding.tilEmail.error = getString(R.string.error_invalid_email)
            password.isEmpty() -> binding.tilPassword.error = getString(R.string.error_empty_fields)
            else -> launchSafely(::setBusy) {
                AuthRepository.signIn(email, password)
                offerBiometricEnrollment(email, password)
            }
        }
    }

    /**
     * Huella → BiometricPrompt autoriza el Cipher del Keystore → se descifran las credenciales →
     * se inicia sesión en Firebase con ellas.
     */
    private fun loginWithBiometrics() {
        val cipher = credentialStore.decryptCipher()
        if (cipher == null) {
            toast(getString(R.string.biometric_invalidated))
            binding.btnBiometric.show(false)
            return
        }
        BiometricAuth.authenticate(
            activity = this,
            cipher = cipher,
            subtitle = getString(R.string.biometric_subtitle_login, credentialStore.savedEmail().orEmpty()),
            onSuccess = { authorized ->
                val (email, password) = credentialStore.read(authorized)
                launchSafely(::setBusy) {
                    AuthRepository.signIn(email, password)
                    openMain()
                }
            },
            onError = { message -> toast(message) }
        )
    }

    /** Tras un login con contraseña, propone guardar las credenciales protegidas con huella. */
    private fun offerBiometricEnrollment(email: String, password: String) {
        val alreadyEnabled = credentialStore.hasCredentials() && credentialStore.savedEmail() == email
        if (alreadyEnabled || !BiometricAuth.isAvailable(this)) {
            openMain()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.biometric_enable_title)
            .setMessage(R.string.biometric_enable_body)
            .setIcon(R.drawable.ic_fingerprint)
            .setNegativeButton(R.string.action_not_now) { _, _ -> openMain() }
            .setPositiveButton(R.string.action_enable) { _, _ ->
                BiometricAuth.authenticate(
                    activity = this,
                    cipher = credentialStore.encryptCipher(),
                    subtitle = getString(R.string.biometric_subtitle_enable),
                    onSuccess = { authorized ->
                        credentialStore.save(authorized, email, password)
                        toast(getString(R.string.biometric_enabled))
                        openMain()
                    },
                    onError = { openMain() }
                )
            }
            .setCancelable(false)
            .show()
    }

    private fun setBusy(busy: Boolean) {
        binding.progress.visibility = if (busy) android.view.View.VISIBLE else android.view.View.INVISIBLE
        binding.btnLogin.isEnabled = !busy
        binding.btnBiometric.isEnabled = !busy
    }

    /** Abre la app principal y cierra la pila de autenticación. */
    private fun openMain() {
        binding.etPassword.text = null
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}

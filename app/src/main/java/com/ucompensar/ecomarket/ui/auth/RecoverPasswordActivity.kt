package com.ucompensar.ecomarket.ui.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.repository.AuthRepository
import com.ucompensar.ecomarket.databinding.ActivityRecoverPasswordBinding
import com.ucompensar.ecomarket.util.isValidEmail
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.show

/** Recuperación de contraseña: Firebase envía un enlace de restablecimiento al correo. */
class RecoverPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecoverPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoverPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnSendLink.setOnClickListener { sendLink() }
    }

    /**
     * Pide el correo de restablecimiento. Por seguridad el mensaje es el mismo exista o no la
     * cuenta, para no revelar qué correos están registrados.
     */
    private fun sendLink() {
        val email = binding.etRecoverEmail.text.toString().trim()
        if (!email.isValidEmail()) {
            binding.tilRecoverEmail.error = getString(R.string.error_invalid_email)
            return
        }
        binding.tilRecoverEmail.error = null
        launchSafely(::setBusy) {
            try {
                AuthRepository.sendPasswordReset(email)
            } catch (e: FirebaseAuthInvalidUserException) {
                // Cuenta inexistente: se responde igual que si existiera
            }
            binding.txtRecoverResult.text = getString(R.string.recover_sent, email)
            binding.txtRecoverResult.show(true)
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        binding.btnSendLink.isEnabled = !busy
    }
}

package com.ucompensar.ecomarket.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.model.Role
import com.ucompensar.ecomarket.data.repository.AuthRepository
import com.ucompensar.ecomarket.databinding.ActivityRegisterBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.util.isValidEmail
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toast

/**
 * Registro de cuentas nuevas (comprador o vendedor) con Firebase Authentication.
 * Exige aceptar la política de tratamiento de datos (Ley 1581 de 2012 y Decreto 1377 de 2013).
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        // El nombre del negocio solo se pide a los vendedores
        binding.chipRoles.setOnCheckedStateChangeListener { _, _ -> binding.tilBusiness.show(selectedRole() == Role.SELLER) }
        binding.btnRegister.setOnClickListener { register() }
    }

    private fun selectedRole(): Role = if (binding.chipRoles.checkedChipId == R.id.chipSeller) Role.SELLER else Role.BUYER

    /** Valida cada campo, muestra los errores junto a él y, si todo está bien, crea la cuenta. */
    private fun register() {
        val name = binding.etName.text.toString().trim()
        val business = binding.etBusiness.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirm.text.toString()
        val role = selectedRole()

        binding.tilName.error = if (name.isEmpty()) getString(R.string.error_required) else null
        binding.tilBusiness.error = if (role == Role.SELLER && business.isEmpty()) getString(R.string.error_required) else null
        binding.tilEmail.error = if (!email.isValidEmail()) getString(R.string.error_invalid_email) else null
        binding.tilPassword.error = if (password.length < MIN_PASSWORD) getString(R.string.error_password_short) else null
        binding.tilConfirm.error = if (confirm != password) getString(R.string.error_password_mismatch) else null
        val formErrors = listOf(binding.tilName, binding.tilBusiness, binding.tilEmail, binding.tilPassword, binding.tilConfirm)
            .any { it.error != null }
        if (formErrors) return
        if (!binding.chkPrivacy.isChecked) {
            toast(getString(R.string.error_privacy_required))
            return
        }

        launchSafely(::setBusy) {
            val profile = AuthRepository.register(name, email, phone, password, role, business)
            toast(getString(R.string.register_success, profile.name))
            startActivity(Intent(this@RegisterActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        binding.btnRegister.isEnabled = !busy
    }

    private companion object {
        const val MIN_PASSWORD = 6
    }
}

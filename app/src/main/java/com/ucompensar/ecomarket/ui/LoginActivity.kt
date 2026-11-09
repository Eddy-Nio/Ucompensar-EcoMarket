package com.ucompensar.ecomarket.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.data.Role
import com.ucompensar.ecomarket.databinding.ActivityLoginBinding
import com.ucompensar.ecomarket.util.isValidEmail
import com.ucompensar.ecomarket.util.toast

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.chipRoles.setOnCheckedStateChangeListener { _, _ -> fillDemoEmail() }
        fillDemoEmail()

        binding.btnLogin.setOnClickListener { loginWithPassword() }
        binding.btnBiometric.setOnClickListener { loginWithBiometrics() }
        binding.txtForgot.setOnClickListener {
            startActivity(Intent(this, RecoverPasswordActivity::class.java))
        }
    }

    private fun selectedRole(): Role = when (binding.chipRoles.checkedChipId) {
        R.id.chipSeller -> Role.SELLER
        R.id.chipAdmin -> Role.ADMIN
        else -> Role.BUYER
    }

    private fun fillDemoEmail() {
        val email = Repository.users.first { it.role == selectedRole() }.email
        binding.etEmail.setText(email)
        binding.etPassword.setText("")
    }

    private fun loginWithPassword() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        when {
            email.isEmpty() -> binding.tilEmail.error = getString(R.string.error_empty_fields)
            !email.isValidEmail() -> binding.tilEmail.error = getString(R.string.error_invalid_email)
            password.isEmpty() -> binding.tilPassword.error = getString(R.string.error_empty_fields)
            else -> openMain(email)
        }
    }

    private fun loginWithBiometrics() {
        val canUse = BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK)
        if (canUse != BiometricManager.BIOMETRIC_SUCCESS) {
            toast(getString(R.string.biometric_unavailable))
            openMain(binding.etEmail.text.toString().trim())
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    openMain(binding.etEmail.text.toString().trim())
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_cancel))
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    private fun openMain(email: String) {
        val role = selectedRole()
        Repository.login(role, email)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}

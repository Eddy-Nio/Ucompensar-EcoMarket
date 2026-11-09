package com.ucompensar.ecomarket.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.databinding.ActivityRecoverPasswordBinding
import com.ucompensar.ecomarket.util.isValidEmail
import com.ucompensar.ecomarket.util.show

class RecoverPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecoverPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoverPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnSendLink.setOnClickListener {
            val email = binding.etRecoverEmail.text.toString().trim()
            if (!email.isValidEmail()) {
                binding.tilRecoverEmail.error = getString(R.string.error_invalid_email)
                binding.txtRecoverResult.show(false)
            } else {
                binding.tilRecoverEmail.error = null
                binding.txtRecoverResult.text = getString(R.string.recover_sent, email)
                binding.txtRecoverResult.show(true)
            }
        }
    }
}

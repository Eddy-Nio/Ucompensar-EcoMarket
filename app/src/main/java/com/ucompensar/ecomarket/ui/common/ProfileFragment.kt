package com.ucompensar.ecomarket.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.Role
import com.ucompensar.ecomarket.data.repository.AuthRepository
import com.ucompensar.ecomarket.data.repository.UserRepository
import com.ucompensar.ecomarket.databinding.FragmentProfileBinding
import com.ucompensar.ecomarket.security.BiometricCredentialStore
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toast

/**
 * Perfil compartido por los tres roles: datos personales (RF-31), cambio de contraseña (RF-32),
 * ingreso con huella y preferencias (RF-34). Los cambios se guardan en `users/{uid}`.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var credentialStore: BiometricCredentialStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.profile_title)
        credentialStore = BiometricCredentialStore(requireContext())
        bindProfile()
        bindBiometricSwitch()
        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.btnChangePassword.setOnClickListener { changePassword() }
        binding.switchNotifications.isChecked = prefs().getBoolean(PREF_NOTIFICATIONS, true)
        binding.switchNotifications.setOnCheckedChangeListener { _, checked ->
            prefs().edit().putBoolean(PREF_NOTIFICATIONS, checked).apply()
        }
    }

    private fun bindProfile() {
        val user = Session.current
        binding.txtProfileInitial.text = user.name.firstOrNull()?.uppercase() ?: "?"
        binding.txtProfileRole.text = getString(R.string.profile_role, getString(user.role.labelRes))
        binding.etProfileName.setText(user.name)
        binding.etProfileEmail.setText(user.email)
        binding.etProfilePhone.setText(user.phone)
        binding.tilProfileBusiness.show(user.role == Role.SELLER)
        binding.etProfileBusiness.setText(user.business)
    }

    /** Se puede desactivar aquí; activarla requiere la contraseña, por eso se hace en el login. */
    private fun bindBiometricSwitch() {
        val enabled = credentialStore.hasCredentials() && credentialStore.savedEmail() == Session.current.email
        binding.switchBiometric.isChecked = enabled
        binding.txtBiometricHint.show(!enabled)
        binding.switchBiometric.setOnCheckedChangeListener { _, checked ->
            if (checked && !enabled) {
                binding.switchBiometric.isChecked = false
                toast(getString(R.string.profile_biometric_hint))
            } else if (!checked) {
                credentialStore.clear()
                binding.txtBiometricHint.show(true)
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etProfileName.text.toString().trim()
        val business = binding.etProfileBusiness.text.toString().trim()
        binding.tilProfileName.error = if (name.isEmpty()) getString(R.string.error_required) else null
        if (name.isEmpty()) return
        launchSafely(::setBusy) {
            UserRepository.updateOwnProfile(name, binding.etProfilePhone.text.toString().trim(), business)
            binding.txtProfileInitial.text = name.first().uppercase()
            (activity as? MainActivity)?.setupHeader()
            toast(getString(R.string.profile_saved))
        }
    }

    /** Reautentica con la contraseña actual y la reemplaza; la huella guardada queda desactualizada y se borra. */
    private fun changePassword() {
        val current = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        binding.tilCurrentPassword.error = if (current.isEmpty()) getString(R.string.error_required) else null
        binding.tilNewPassword.error = if (newPassword.length < MIN_PASSWORD) getString(R.string.error_password_short) else null
        if (binding.tilCurrentPassword.error != null || binding.tilNewPassword.error != null) return
        launchSafely(::setBusy) {
            AuthRepository.changePassword(current, newPassword)
            credentialStore.clear()
            bindBiometricSwitch()
            binding.etCurrentPassword.text = null
            binding.etNewPassword.text = null
            toast(getString(R.string.password_changed))
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        binding.btnSaveProfile.isEnabled = !busy
        binding.btnChangePassword.isEnabled = !busy
    }

    private fun prefs() = requireContext().getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val PREFS = "preferences"
        const val PREF_NOTIFICATIONS = "notifications"
        const val MIN_PASSWORD = 6
    }
}

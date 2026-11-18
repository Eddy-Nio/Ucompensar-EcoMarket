package com.ucompensar.ecomarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.Role
import com.ucompensar.ecomarket.data.model.UserProfile
import com.ucompensar.ecomarket.data.repository.UserRepository
import com.ucompensar.ecomarket.databinding.FragmentUserFormBinding
import com.ucompensar.ecomarket.util.isValidEmail
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toast

/**
 * Creación y edición de usuarios por el administrador (RF-09 y RF-10): datos, rol y estado.
 * Al crear se define la contraseña inicial; al editar, el correo queda bloqueado. Los cambios de
 * rol piden confirmación (doble si se concede administrador) y quedan en la bitácora.
 */
class UserFormFragment : Fragment() {

    private var _binding: FragmentUserFormBinding? = null
    private val binding get() = _binding!!
    private var editing: UserProfile? = null
    private val roleLabels by lazy { Role.entries.map { getString(it.labelRes) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentUserFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val uid = arguments?.getString(ARG_ID)
        activity?.title = getString(if (uid == null) R.string.user_new else R.string.user_edit)
        binding.acUfRole.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, roleLabels))
        binding.acUfRole.setText(roleLabels[Role.BUYER.ordinal], false)
        binding.acUfRole.setOnItemClickListener { _, _, _, _ -> binding.tilUfBusiness.show(selectedRole() == Role.SELLER) }
        binding.tilUfPassword.show(uid == null)
        binding.btnUfSave.setOnClickListener { save() }
        if (uid != null) launchSafely(::setBusy) { UserRepository.get(uid)?.let(::fill) }
    }

    private fun fill(user: UserProfile) {
        editing = user
        binding.etUfName.setText(user.name)
        binding.etUfEmail.setText(user.email)
        binding.etUfEmail.isEnabled = false
        binding.tilUfEmail.helperText = getString(R.string.user_email_locked)
        binding.etUfPhone.setText(user.phone)
        binding.acUfRole.setText(roleLabels[user.role.ordinal], false)
        binding.etUfBusiness.setText(user.business)
        binding.tilUfBusiness.show(user.role == Role.SELLER)
        binding.switchUfActive.isChecked = user.active
        // El administrador no puede cambiar su propio rol ni suspenderse (evita perder el acceso)
        if (user.uid == Session.current.uid) {
            binding.tilUfRole.isEnabled = false
            binding.switchUfActive.isEnabled = false
            binding.tilUfRole.helperText = getString(R.string.user_self_locked)
        }
    }

    private fun selectedRole(): Role = Role.entries[roleLabels.indexOf(binding.acUfRole.text.toString()).coerceAtLeast(0)]

    /** Valida y crea o actualiza el usuario en Firestore (y en Auth si es nuevo). */
    private fun save() {
        val name = binding.etUfName.text.toString().trim()
        val email = binding.etUfEmail.text.toString().trim()
        val password = binding.etUfPassword.text.toString()
        binding.tilUfName.error = if (name.isEmpty()) getString(R.string.error_required) else null
        binding.tilUfEmail.error = if (!email.isValidEmail()) getString(R.string.error_invalid_email) else null
        binding.tilUfPassword.error = if (editing == null && password.length < MIN_PASSWORD) getString(R.string.error_password_short) else null
        if (listOf(binding.tilUfName, binding.tilUfEmail, binding.tilUfPassword).any { it.error != null }) return

        val profile = (editing ?: UserProfile()).copy(
            name = name, email = email, phone = binding.etUfPhone.text.toString().trim(), role = selectedRole(),
            active = binding.switchUfActive.isChecked, business = binding.etUfBusiness.text.toString().trim()
        )
        val previousRole = editing?.role
        when {
            // Doble confirmación: todo cambio de rol se confirma y conceder administrador exige una segunda confirmación
            previousRole != profile.role && (editing != null || profile.role != Role.BUYER) ->
                confirmRoleChange(profile, previousRole) { persist(profile, password) }
            else -> persist(profile, password)
        }
    }

    /** Primera confirmación del cambio de rol y, si el rol nuevo es administrador, una segunda. */
    private fun confirmRoleChange(profile: UserProfile, from: Role?, onConfirmed: () -> Unit) {
        val to = getString(profile.role.labelRes)
        val message = if (from == null) getString(R.string.confirm_role_new, profile.name, to)
        else getString(R.string.confirm_role_change, profile.name, getString(from.labelRes), to)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_role_title)
            .setMessage(message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_continue) { _, _ ->
                if (profile.role != Role.ADMIN) onConfirmed()
                else MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.confirm_admin_title)
                    .setMessage(getString(R.string.confirm_admin_body, profile.name))
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_grant_admin) { _, _ -> onConfirmed() }
                    .show()
            }
            .show()
    }

    private fun persist(profile: UserProfile, password: String) {
        launchSafely(::setBusy) {
            val before = editing
            if (before == null) UserRepository.createByAdmin(profile, password) else UserRepository.updateByAdmin(before, profile)
            toast(getString(R.string.user_saved))
            parentFragmentManager.popBackStack()
        }
    }

    private fun setBusy(busy: Boolean) {
        _binding ?: return
        binding.progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        binding.btnUfSave.isEnabled = !busy
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "user_id"
        private const val MIN_PASSWORD = 6

        fun newInstance(uid: String?) = UserFormFragment().apply {
            arguments = Bundle().apply { putString(ARG_ID, uid) }
        }
    }
}

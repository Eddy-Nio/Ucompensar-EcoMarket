package com.ucompensar.ecomarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.ucompensar.ecomarket.R
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
 * Al crear se define la contraseña inicial; al editar, el correo queda bloqueado.
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
        launchSafely(::setBusy) {
            if (editing == null) UserRepository.createByAdmin(profile, password) else UserRepository.updateByAdmin(profile)
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

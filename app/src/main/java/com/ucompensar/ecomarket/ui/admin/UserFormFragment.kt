package com.ucompensar.ecomarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.data.Role
import com.ucompensar.ecomarket.databinding.FragmentUserFormBinding
import com.ucompensar.ecomarket.util.isValidEmail
import com.ucompensar.ecomarket.util.toast

/** Creación y edición de usuarios por parte del administrador (RF-09, RF-10). */
class UserFormFragment : Fragment() {

    private var _binding: FragmentUserFormBinding? = null
    private val binding get() = _binding!!
    private var userId: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentUserFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        userId = arguments?.getInt(ARG_ID, NO_ID)?.takeIf { it != NO_ID }
        val user = userId?.let { id -> Repository.users.firstOrNull { it.id == id } }
        activity?.title = getString(if (user == null) R.string.user_new else R.string.user_edit)

        val roleLabels = Role.entries.map { getString(it.labelRes) }
        binding.acUfRole.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, roleLabels))
        binding.acUfRole.setText(getString((user?.role ?: Role.BUYER).labelRes), false)

        if (user != null) {
            binding.etUfName.setText(user.name)
            binding.etUfEmail.setText(user.email)
            binding.etUfPhone.setText(user.phone)
            binding.switchUfActive.isChecked = user.active
        }
        binding.btnUfSave.setOnClickListener { save() }
    }

    private fun save() {
        val name = binding.etUfName.text.toString().trim()
        val email = binding.etUfEmail.text.toString().trim()
        binding.tilUfName.error = if (name.isEmpty()) getString(R.string.error_required) else null
        binding.tilUfEmail.error = if (!email.isValidEmail()) getString(R.string.error_invalid_email) else null
        if (name.isEmpty() || !email.isValidEmail()) return

        val selected = binding.acUfRole.text.toString()
        val role = Role.entries.firstOrNull { getString(it.labelRes) == selected } ?: Role.BUYER
        Repository.saveUser(
            userId, name, email, binding.etUfPhone.text.toString().trim(), role, binding.switchUfActive.isChecked
        )
        toast(getString(R.string.user_saved))
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "user_id"
        private const val NO_ID = -1

        fun newInstance(id: Int?) = UserFormFragment().apply {
            arguments = Bundle().apply { putInt(ARG_ID, id ?: NO_ID) }
        }
    }
}

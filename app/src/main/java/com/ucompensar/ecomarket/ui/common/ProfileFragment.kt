package com.ucompensar.ecomarket.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentProfileBinding
import com.ucompensar.ecomarket.util.toast

/** Perfil compartido por los tres roles (RF-31). */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.profile_title)
        val user = Repository.currentUser
        binding.txtProfileInitial.text = user.name.first().toString()
        binding.txtProfileRole.text = getString(R.string.profile_role, getString(user.role.labelRes))
        binding.etProfileName.setText(user.name)
        binding.etProfileEmail.setText(user.email)
        binding.etProfilePhone.setText(user.phone)
        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etProfileName.text.toString().trim()
            if (name.isNotEmpty()) {
                Repository.updateProfile(name, binding.etProfilePhone.text.toString().trim())
                binding.txtProfileInitial.text = name.first().toString()
                toast(getString(R.string.profile_saved))
            } else {
                binding.etProfileName.error = getString(R.string.error_required)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

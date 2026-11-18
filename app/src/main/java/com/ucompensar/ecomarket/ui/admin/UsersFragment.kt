package com.ucompensar.ecomarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.UserProfile
import com.ucompensar.ecomarket.data.repository.UserRepository
import com.ucompensar.ecomarket.databinding.FragmentUsersBinding
import com.ucompensar.ecomarket.databinding.ItemUserBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.bindActive
import com.ucompensar.ecomarket.util.collectWhileVisible
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.toast

/** Gestión de usuarios (RF-08 y RF-09): listado en tiempo real, búsqueda, filtro y eliminación. */
class UsersFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!
    private var allUsers: List<UserProfile> = emptyList()

    private val adapter = SimpleAdapter<UserProfile, ItemUserBinding>(ItemUserBinding::inflate, UserProfile::uid) { b, user ->
        b.txtUserInitial.text = user.name.firstOrNull()?.uppercase() ?: "?"
        b.txtUName.text = user.name
        b.txtUEmail.text = user.email
        b.txtURole.text = if (user.business.isBlank()) getString(user.role.labelRes)
        else "${getString(user.role.labelRes)} · ${user.business}"
        b.txtUStatus.bindActive(user.active)
        b.root.setOnClickListener { (requireActivity() as MainActivity).open(UserFormFragment.newInstance(user.uid)) }
        b.btnUDelete.setOnClickListener { confirmDelete(user) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.users_title)
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter
        adapter.scrollToTopOnInsert(binding.rvUsers)
        binding.fabNewUser.setOnClickListener { (requireActivity() as MainActivity).open(UserFormFragment.newInstance(null)) }
        binding.etUserSearch.doAfterTextChanged { applyFilters() }
        binding.chipUserFilter.setOnCheckedStateChangeListener { _, _ -> applyFilters() }
        collectWhileVisible(UserRepository.observeAll()) {
            allUsers = it
            applyFilters()
        }
    }

    /** Filtra por texto (nombre o correo) y por estado de la cuenta. */
    private fun applyFilters() {
        val query = binding.etUserSearch.text.toString().trim()
        adapter.submitList(allUsers.filter { user ->
            val matchesStatus = when (binding.chipUserFilter.checkedChipId) {
                R.id.chipUActive -> user.active
                R.id.chipUSuspended -> !user.active
                else -> true
            }
            matchesStatus && (query.isEmpty() || user.name.contains(query, true) || user.email.contains(query, true))
        })
    }

    private fun confirmDelete(user: UserProfile) {
        if (user.uid == Session.current.uid) {
            toast(getString(R.string.error_delete_self))
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_user, user.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                launchSafely {
                    UserRepository.delete(user.uid)
                    toast(getString(R.string.user_deleted))
                }
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

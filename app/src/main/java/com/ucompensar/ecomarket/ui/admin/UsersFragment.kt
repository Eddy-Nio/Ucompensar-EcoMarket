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
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.data.User
import com.ucompensar.ecomarket.databinding.FragmentUsersBinding
import com.ucompensar.ecomarket.databinding.ItemUserBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.bindActive
import com.ucompensar.ecomarket.util.toast

/** Listado, búsqueda, filtro y eliminación de usuarios (RF-08, RF-09). */
class UsersFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!

    private val adapter = SimpleAdapter<User, ItemUserBinding>(ItemUserBinding::inflate) { b, user ->
        b.txtUserInitial.text = user.name.first().toString()
        b.txtUName.text = user.name
        b.txtUEmail.text = user.email
        b.txtURole.text = getString(user.role.labelRes)
        b.txtUStatus.bindActive(user.active)
        b.root.setOnClickListener { (requireActivity() as MainActivity).open(UserFormFragment.newInstance(user.id)) }
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
        binding.fabNewUser.setOnClickListener { (requireActivity() as MainActivity).open(UserFormFragment.newInstance(null)) }
        binding.etUserSearch.doAfterTextChanged { refresh() }
        binding.chipUserFilter.setOnCheckedStateChangeListener { _, _ -> refresh() }
        refresh()
    }

    private fun refresh() {
        val query = binding.etUserSearch.text.toString().trim()
        adapter.items = Repository.users.filter { user ->
            val matchesFilter = when (binding.chipUserFilter.checkedChipId) {
                R.id.chipUActive -> user.active
                R.id.chipUSuspended -> !user.active
                else -> true
            }
            matchesFilter && (query.isEmpty() || user.name.contains(query, true) || user.email.contains(query, true))
        }
    }

    private fun confirmDelete(user: User) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_user, user.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                Repository.deleteUser(user.id)
                toast(getString(R.string.user_deleted))
                refresh()
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

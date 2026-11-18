package com.ucompensar.ecomarket.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.Role
import com.ucompensar.ecomarket.data.repository.AuthRepository
import com.ucompensar.ecomarket.databinding.ActivityMainBinding
import com.ucompensar.ecomarket.ui.admin.AdminDashboardFragment
import com.ucompensar.ecomarket.ui.admin.AdminProductsFragment
import com.ucompensar.ecomarket.ui.admin.SalesReportFragment
import com.ucompensar.ecomarket.ui.admin.UsersFragment
import com.ucompensar.ecomarket.ui.auth.LoginActivity
import com.ucompensar.ecomarket.ui.buyer.BuyerOrdersFragment
import com.ucompensar.ecomarket.ui.buyer.CartFragment
import com.ucompensar.ecomarket.ui.buyer.CatalogFragment
import com.ucompensar.ecomarket.ui.buyer.HomeFragment
import com.ucompensar.ecomarket.ui.common.ProfileFragment
import com.ucompensar.ecomarket.ui.seller.SellerOrdersFragment
import com.ucompensar.ecomarket.ui.seller.SellerProductsFragment

/**
 * Contenedor principal: Drawer + Bottom Navigation cuyo menú depende del rol del usuario.
 * Cada pestaña es un Fragment; las pantallas internas (detalle, formularios, pago) se apilan con retroceso.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Si el sistema destruyó el proceso, no hay perfil en memoria: se vuelve al login
        val profile = Session.profile ?: run {
            goToLogin()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupMenus(profile.role)
        hideBottomNavWithKeyboard()
        setupHeader()
        setupBackHandling()

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = binding.bottomNav.menu.getItem(0).itemId
        }
    }

    /** Menú de cada rol para el Bottom Navigation y el Drawer. */
    private fun roleMenu(role: Role): Int = when (role) {
        Role.BUYER -> R.menu.menu_buyer
        Role.SELLER -> R.menu.menu_seller
        Role.ADMIN -> R.menu.menu_admin
    }

    private fun setupMenus(role: Role) {
        binding.bottomNav.inflateMenu(roleMenu(role))
        binding.navView.inflateMenu(roleMenu(role))
        binding.navView.inflateMenu(R.menu.menu_drawer_common)
        // Comprador y Vendedor ya tienen "Perfil" en el menú de su rol; solo el Administrador lo necesita aquí
        if (role != Role.ADMIN) binding.navView.menu.removeItem(R.id.drawer_profile)

        binding.bottomNav.setOnItemSelectedListener { item ->
            clearBackStack()
            replace(fragmentFor(item.itemId), addToBackStack = false)
            binding.navView.setCheckedItem(item.itemId)
            true
        }
        binding.navView.setNavigationItemSelectedListener { item -> onDrawerItem(item) }
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) onBackPressedDispatcher.onBackPressed()
            else binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val nested = supportFragmentManager.backStackEntryCount > 0
            binding.toolbar.setNavigationIcon(if (nested) R.drawable.ic_back else R.drawable.ic_menu)
            binding.toolbar.setNavigationContentDescription(if (nested) R.string.cd_back else R.string.drawer_open)
        }
    }

    /** Oculta la barra inferior mientras el teclado está abierto para no tapar los formularios. */
    private fun hideBottomNavWithKeyboard() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val imeVisible = ViewCompat.getRootWindowInsets(binding.root)?.isVisible(WindowInsetsCompat.Type.ime()) == true
            val target = if (imeVisible) android.view.View.GONE else android.view.View.VISIBLE
            if (binding.bottomNav.visibility != target) binding.bottomNav.visibility = target
        }
    }

    /** Muestra el nombre, la inicial y el rol en la cabecera del Drawer. */
    fun setupHeader() {
        val user = Session.profile ?: return
        val header = binding.navView.getHeaderView(0)
        header.findViewById<TextView>(R.id.txtInitial).text = user.name.firstOrNull()?.uppercase() ?: "?"
        header.findViewById<TextView>(R.id.txtUserName).text = user.name
        header.findViewById<TextView>(R.id.txtUserRole).text = getString(user.role.labelRes)
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> binding.drawerLayout.closeDrawer(GravityCompat.START)
                    supportFragmentManager.backStackEntryCount > 0 -> supportFragmentManager.popBackStack()
                    else -> finish()
                }
            }
        })
    }

    private fun onDrawerItem(item: MenuItem): Boolean {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.drawer_logout -> confirmLogout()
            R.id.drawer_profile -> {
                clearBackStack()
                replace(ProfileFragment(), addToBackStack = false)
            }
            else -> binding.bottomNav.selectedItemId = item.itemId
        }
        return true
    }

    /** Fragment raíz de cada opción del Bottom Navigation. */
    private fun fragmentFor(id: Int): Fragment = when (id) {
        R.id.nav_home -> HomeFragment()
        R.id.nav_catalog -> CatalogFragment()
        R.id.nav_cart -> CartFragment()
        R.id.nav_my_orders -> BuyerOrdersFragment()
        R.id.nav_profile -> ProfileFragment()
        R.id.nav_my_products -> SellerProductsFragment()
        R.id.nav_orders -> SellerOrdersFragment()
        R.id.nav_dashboard -> AdminDashboardFragment()
        R.id.nav_users -> UsersFragment()
        R.id.nav_products -> AdminProductsFragment()
        R.id.nav_report -> SalesReportFragment()
        else -> error("Destino de navegación desconocido: $id")
    }

    /** Abre una pantalla interna (detalle, formulario, checkout) que se puede cerrar con «atrás». */
    fun open(fragment: Fragment) = replace(fragment, addToBackStack = true)

    /** Cambia de pestaña desde un fragmento (por ejemplo, del inicio al catálogo). */
    fun selectTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    private fun replace(fragment: Fragment, addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_main, fragment)
            .apply { if (addToBackStack) addToBackStack(null) }
            .commit()
    }

    private fun clearBackStack() {
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_body)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.nav_logout) { _, _ ->
                AuthRepository.signOut()
                goToLogin()
            }
            .show()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}

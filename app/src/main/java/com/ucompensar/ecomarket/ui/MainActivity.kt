package com.ucompensar.ecomarket.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.GravityCompat
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.data.Role
import com.ucompensar.ecomarket.databinding.ActivityMainBinding
import com.ucompensar.ecomarket.ui.admin.AdminDashboardFragment
import com.ucompensar.ecomarket.ui.admin.AdminProductsFragment
import com.ucompensar.ecomarket.ui.admin.SalesReportFragment
import com.ucompensar.ecomarket.ui.admin.UsersFragment
import com.ucompensar.ecomarket.ui.buyer.CartFragment
import com.ucompensar.ecomarket.ui.buyer.CatalogFragment
import com.ucompensar.ecomarket.ui.buyer.HomeFragment
import com.ucompensar.ecomarket.ui.common.ProfileFragment
import com.ucompensar.ecomarket.ui.seller.SellerOrdersFragment
import com.ucompensar.ecomarket.ui.seller.SellerProductsFragment

/** Contenedor único: Drawer + Bottom Navigation cuyo menú depende del rol autenticado. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val user get() = Repository.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupMenus()
        setupHeader()
        setupBackHandling()

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = binding.bottomNav.menu.getItem(0).itemId
        }
    }

    private fun roleMenu(): Int = when (user.role) {
        Role.BUYER -> R.menu.menu_buyer
        Role.SELLER -> R.menu.menu_seller
        Role.ADMIN -> R.menu.menu_admin
    }

    private fun setupMenus() {
        binding.bottomNav.inflateMenu(roleMenu())
        binding.navView.inflateMenu(roleMenu())
        binding.navView.inflateMenu(R.menu.menu_drawer_common)

        binding.bottomNav.setOnItemSelectedListener { item ->
            clearBackStack()
            replace(fragmentFor(item.itemId), addToBackStack = false)
            binding.navView.setCheckedItem(item.itemId)
            true
        }
        binding.navView.setNavigationItemSelectedListener { item -> onDrawerItem(item) }
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                onBackPressedDispatcher.onBackPressed()
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val nested = supportFragmentManager.backStackEntryCount > 0
            binding.toolbar.setNavigationIcon(if (nested) R.drawable.ic_back else R.drawable.ic_menu)
            binding.toolbar.setNavigationContentDescription(if (nested) R.string.cd_back else R.string.drawer_open)
        }
    }

    private fun setupHeader() {
        val header = binding.navView.getHeaderView(0)
        header.findViewById<android.widget.TextView>(R.id.txtInitial).text = user.name.first().toString()
        header.findViewById<android.widget.TextView>(R.id.txtUserName).text = user.name
        header.findViewById<android.widget.TextView>(R.id.txtUserRole).text = getString(user.role.labelRes)
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.drawerLayout.isDrawerOpen(GravityCompat.START) ->
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    supportFragmentManager.backStackEntryCount > 0 ->
                        supportFragmentManager.popBackStack()
                    else -> finish()
                }
            }
        })
    }

    private fun onDrawerItem(item: MenuItem): Boolean {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.drawer_logout -> logout()
            R.id.drawer_profile -> {
                clearBackStack()
                replace(ProfileFragment(), addToBackStack = false)
            }
            else -> binding.bottomNav.selectedItemId = item.itemId
        }
        return true
    }

    private fun fragmentFor(id: Int): Fragment = when (id) {
        R.id.nav_home -> HomeFragment()
        R.id.nav_catalog -> CatalogFragment()
        R.id.nav_cart -> CartFragment()
        R.id.nav_profile -> ProfileFragment()
        R.id.nav_my_products -> SellerProductsFragment()
        R.id.nav_orders -> SellerOrdersFragment()
        R.id.nav_dashboard -> AdminDashboardFragment()
        R.id.nav_users -> UsersFragment()
        R.id.nav_products -> AdminProductsFragment()
        R.id.nav_report -> SalesReportFragment()
        else -> error("Destino de navegación desconocido: $id")
    }

    /** Navega a una pantalla interna (detalle, formulario, pago) con retroceso. */
    fun open(fragment: Fragment) = replace(fragment, addToBackStack = true)

    /** Salta a una pestaña del Bottom Navigation desde un fragmento. */
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
        supportFragmentManager.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun logout() {
        Repository.logout()
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}

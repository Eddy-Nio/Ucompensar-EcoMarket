package com.ucompensar.ecomarket.ui.buyer

import androidx.fragment.app.Fragment
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.Product
import com.ucompensar.ecomarket.data.repository.CartRepository
import com.ucompensar.ecomarket.databinding.ItemProductBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.loadProductImage
import com.ucompensar.ecomarket.util.toCop
import com.ucompensar.ecomarket.util.toast

/** Enlaza una tarjeta de producto del catálogo; compartida por Inicio y Catálogo. */
fun ItemProductBinding.bindProduct(product: Product, onOpen: (Product) -> Unit, onAdd: (Product) -> Unit) {
    imgProduct.loadProductImage(product.imageUrl)
    txtName.text = product.name
    txtProducer.text = product.sellerName
    txtPrice.text = product.price.toCop()
    val available = product.stock > 0
    btnAdd.isEnabled = available
    btnAdd.setText(if (available) R.string.action_add else R.string.action_sold_out)
    root.setOnClickListener { onOpen(product) }
    btnAdd.setOnClickListener { onAdd(product) }
}

/**
 * Adaptador de productos para el comprador: abrir detalle y agregar al carrito de Firestore.
 * Se define aquí para no duplicarlo en Inicio y Catálogo.
 */
fun Fragment.buyerProductAdapter() = SimpleAdapter<Product, ItemProductBinding>(ItemProductBinding::inflate, Product::id) { b, product ->
    b.bindProduct(
        product,
        onOpen = { (requireActivity() as MainActivity).open(ProductDetailFragment.newInstance(it.id)) },
        onAdd = {
            launchSafely {
                CartRepository.add(Session.current.uid, it)
                toast(getString(R.string.added_to_cart, it.name))
            }
        }
    )
}

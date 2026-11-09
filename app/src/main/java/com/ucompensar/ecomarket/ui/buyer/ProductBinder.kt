package com.ucompensar.ecomarket.ui.buyer

import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Product
import com.ucompensar.ecomarket.databinding.ItemProductBinding
import com.ucompensar.ecomarket.util.toCop

/** Enlaza una tarjeta de producto; compartido por Inicio y Catálogo. */
fun ItemProductBinding.bindProduct(product: Product, onOpen: (Product) -> Unit, onAdd: (Product) -> Unit) {
    txtName.text = product.name
    txtProducer.text = product.producer
    txtPrice.text = product.price.toCop()
    btnAdd.isEnabled = product.stock > 0
    btnAdd.setText(if (product.stock > 0) R.string.action_add else R.string.product_pending)
    root.setOnClickListener { onOpen(product) }
    btnAdd.setOnClickListener { onAdd(product) }
}

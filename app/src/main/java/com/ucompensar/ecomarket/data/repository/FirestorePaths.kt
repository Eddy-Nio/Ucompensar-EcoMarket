package com.ucompensar.ecomarket.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/** Nombres de las colecciones de Cloud Firestore, centralizados para no repetir cadenas. */
internal object FirestorePaths {
    const val USERS = "users"
    const val PRODUCTS = "products"
    const val CARTS = "carts"
    const val ITEMS = "items"
    const val ORDERS = "orders"

    val db get() = Firebase.firestore
    fun users() = db.collection(USERS)
    fun products() = db.collection(PRODUCTS)
    fun orders() = db.collection(ORDERS)
    fun cart(uid: String) = db.collection(CARTS).document(uid).collection(ITEMS)
}

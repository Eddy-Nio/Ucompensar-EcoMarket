package com.ucompensar.ecomarket.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.dataObjects
import com.ucompensar.ecomarket.data.model.Approval
import com.ucompensar.ecomarket.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** CRUD de productos sobre la colección `products` de Firestore, con imágenes en Storage. */
object ProductRepository {

    /** Productos aprobados para el catálogo del comprador, en tiempo real. */
    fun observeApproved(): Flow<List<Product>> =
        FirestorePaths.products().whereEqualTo("approval", Approval.APPROVED.name)
            .dataObjects<Product>().map { it.sortedBy { p -> p.name.lowercase() } }

    /** Productos de un vendedor (aprobados, pendientes y rechazados). */
    fun observeBySeller(sellerId: String): Flow<List<Product>> =
        FirestorePaths.products().whereEqualTo("sellerId", sellerId)
            .dataObjects<Product>().map { it.sortedByDescending { p -> p.createdAt } }

    /** Todos los productos, para la moderación del administrador (pendientes primero). */
    fun observeAll(): Flow<List<Product>> =
        FirestorePaths.products().dataObjects<Product>()
            .map { it.sortedWith(compareBy<Product> { p -> p.approval != Approval.PENDING }.thenBy { p -> p.name }) }

    /** Lee un producto una sola vez. */
    suspend fun get(id: String): Product? =
        FirestorePaths.products().document(id).get().await().toObject(Product::class.java)

    /**
     * Crea o actualiza un producto. Si llega [newImage] (foto de cámara o galería), la sube a
     * Storage, guarda su URL y borra la imagen anterior. Un producto nuevo queda pendiente de aprobación.
     */
    suspend fun save(context: Context, product: Product, newImage: Uri?): Product {
        val doc = if (product.id.isBlank()) FirestorePaths.products().document()
        else FirestorePaths.products().document(product.id)
        var toSave = product.copy(id = doc.id)

        if (newImage != null) {
            val uploaded = ImageStorage.uploadProductImage(context, newImage, doc.id)
            ImageStorage.delete(product.imagePath)
            toSave = toSave.copy(imageUrl = uploaded.downloadUrl, imagePath = uploaded.path)
        }
        if (product.id.isBlank()) {
            doc.set(toSave.copy(approval = Approval.PENDING)).await()
        } else {
            doc.update(
                mapOf(
                    "name" to toSave.name, "description" to toSave.description, "price" to toSave.price,
                    "stock" to toSave.stock, "category" to toSave.category,
                    "imageUrl" to toSave.imageUrl, "imagePath" to toSave.imagePath
                )
            ).await()
        }
        return toSave
    }

    /** Elimina el producto y su imagen. */
    suspend fun delete(product: Product) {
        FirestorePaths.products().document(product.id).delete().await()
        ImageStorage.delete(product.imagePath)
    }

    /** Aprueba o rechaza un producto (solo administrador). */
    suspend fun setApproval(productId: String, approval: Approval) {
        FirestorePaths.products().document(productId).update("approval", approval.name).await()
    }
}

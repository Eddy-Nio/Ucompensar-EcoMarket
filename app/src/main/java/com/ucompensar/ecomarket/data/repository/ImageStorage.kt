package com.ucompensar.ecomarket.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Sube y borra imágenes de productos en Firebase Storage.
 * Antes de subir, reduce la foto a [MAX_SIDE] píxeles y la comprime a JPEG para ahorrar datos.
 */
object ImageStorage {

    private const val MAX_SIDE = 1280
    private const val JPEG_QUALITY = 82

    /** Resultado de una carga: URL pública de descarga y ruta interna del archivo en el bucket. */
    data class Uploaded(val downloadUrl: String, val path: String)

    /** Comprime la imagen de [uri] (cámara o galería) y la sube a `products/{productId}/`. */
    suspend fun uploadProductImage(context: Context, uri: Uri, productId: String): Uploaded {
        val bytes = withContext(Dispatchers.IO) { compress(context, uri) }
        val path = "products/$productId/${System.currentTimeMillis()}.jpg"
        val ref = Firebase.storage.reference.child(path)
        ref.putBytes(bytes, storageMetadata { contentType = "image/jpeg" }).await()
        return Uploaded(ref.downloadUrl.await().toString(), path)
    }

    /** Borra un archivo del bucket; ignora el error si ya no existe. */
    suspend fun delete(path: String) {
        if (path.isBlank()) return
        runCatching { Firebase.storage.reference.child(path).delete().await() }
    }

    /** Decodifica la imagen con submuestreo, corrige la rotación EXIF y la convierte a JPEG. */
    private fun compress(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }

        // Submuestreo en potencias de 2 hasta acercarse al tamaño máximo
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("No se pudo leer la imagen")

        val rotated = rotateByExif(decoded, resolver.openInputStream(uri)?.use { ExifInterface(it) })
        val scale = MAX_SIDE.toFloat() / max(rotated.width, rotated.height)
        val finalBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(rotated, (rotated.width * scale).toInt(), (rotated.height * scale).toInt(), true)
        } else rotated

        return ByteArrayOutputStream().use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    /** Las cámaras guardan la orientación en EXIF; aquí se aplica para que la foto no quede girada. */
    private fun rotateByExif(bitmap: Bitmap, exif: ExifInterface?): Bitmap {
        val degrees = when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

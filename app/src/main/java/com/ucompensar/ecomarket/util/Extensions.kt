package com.ucompensar.ecomarket.util

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.ucompensar.ecomarket.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

fun Fragment.toast(message: String) = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun View.show(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

/** Carga la imagen de un producto desde Storage, con un marcador mientras llega o si no hay foto. */
fun ImageView.loadProductImage(url: String) {
    if (url.isBlank()) {
        setImageResource(R.drawable.ic_product_placeholder)
        return
    }
    load(url) {
        crossfade(true)
        placeholder(R.drawable.ic_product_placeholder)
        error(R.drawable.ic_product_placeholder)
    }
}

/**
 * Recoge un Flow (por ejemplo, un listener de Firestore) solo mientras la vista está visible.
 * Al salir de la pantalla el listener se cancela, lo que evita fugas y lecturas innecesarias.
 */
fun <T> Fragment.collectWhileVisible(flow: Flow<T>, onError: (Throwable) -> Unit = {}, action: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.catch { onError(it); context?.let { ctx -> toast(it.toUserMessage(ctx)) } }.collect(action)
        }
    }
}

/**
 * Ejecuta una operación de red en el ciclo de vida de la vista y muestra el error al usuario si
 * falla. [busy] permite deshabilitar botones o mostrar el indicador de progreso mientras tanto.
 */
fun Fragment.launchSafely(busy: (Boolean) -> Unit = {}, block: suspend CoroutineScope.() -> Unit): Job =
    viewLifecycleOwner.lifecycleScope.launch {
        busy(true)
        try {
            block()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            context?.let { toast(e.toUserMessage(it)) }
        } finally {
            busy(false)
        }
    }

/** Equivalente a [launchSafely] para Activities. */
fun AppCompatActivity.launchSafely(busy: (Boolean) -> Unit = {}, block: suspend CoroutineScope.() -> Unit): Job =
    lifecycleScope.launch {
        busy(true)
        try {
            block()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            toast(e.toUserMessage(this@launchSafely))
        } finally {
            busy(false)
        }
    }

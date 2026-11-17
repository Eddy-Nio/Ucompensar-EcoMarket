package com.ucompensar.ecomarket.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * Adaptador genérico de RecyclerView basado en ViewBinding y DiffUtil.
 *
 * Evita escribir un adaptador por cada lista: recibe cómo inflar la fila, cómo identificar cada
 * elemento ([key]) y cómo enlazar los datos. DiffUtil solo repinta las filas que cambiaron cuando
 * Firestore envía una actualización.
 */
class SimpleAdapter<T : Any, VB : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup, Boolean) -> VB,
    key: (T) -> Any,
    private val onBind: (VB, T) -> Unit
) : ListAdapter<T, SimpleAdapter.Holder<VB>>(KeyDiff(key)) {

    class Holder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)

    /** Compara por clave para saber si es el mismo elemento y por igualdad para saber si cambió. */
    private class KeyDiff<T : Any>(private val key: (T) -> Any) : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T) = key(oldItem) == key(newItem)
        @Suppress("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder<VB>, position: Int) = onBind(holder.binding, getItem(position))
}

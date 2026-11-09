package com.ucompensar.ecomarket.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/** Adaptador genérico basado en ViewBinding para evitar un adaptador por cada lista. */
class SimpleAdapter<T, VB : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup, Boolean) -> VB,
    private val onBind: (VB, T) -> Unit
) : RecyclerView.Adapter<SimpleAdapter.Holder<VB>>() {

    class Holder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)

    var items: List<T> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder<VB>, position: Int) =
        onBind(holder.binding, items[position])

    override fun getItemCount() = items.size
}

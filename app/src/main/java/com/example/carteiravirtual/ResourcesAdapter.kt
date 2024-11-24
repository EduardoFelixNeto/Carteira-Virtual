package com.example.carteiravirtual

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Modelo de dados (substitua pelo seu modelo real)
data class ResourceItem(val currency: String, val balance: Double)

class ResourcesAdapter(private val resourceList: List<ResourceItem>) :
    RecyclerView.Adapter<ResourcesAdapter.ResourceViewHolder>() {

    // ViewHolder: representa cada item da lista
    class ResourceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val currencyTextView: TextView = itemView.findViewById(R.id.tvCurrency)
        val balanceTextView: TextView = itemView.findViewById(R.id.tvBalance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resource, parent, false)
        return ResourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        val resourceItem = resourceList[position]
        holder.currencyTextView.text = resourceItem.currency
        holder.balanceTextView.text = "%.2f".format(resourceItem.balance)
    }

    override fun getItemCount(): Int = resourceList.size
}

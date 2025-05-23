package com.frkvrl.bitirme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DevamsizlikAdapter(
    private val devamsizlikListesi: List<Pair<String, Int>>
) : RecyclerView.Adapter<DevamsizlikAdapter.DevamsizlikViewHolder>() {

    inner class DevamsizlikViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ogrenciAdiText: TextView = itemView.findViewById(R.id.ogrenciAdiText)
        val devamsizlikSayisiText: TextView = itemView.findViewById(R.id.devamsizlikSayisiText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevamsizlikViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_devamsizlik, parent, false)
        return DevamsizlikViewHolder(view)
    }

    override fun onBindViewHolder(holder: DevamsizlikViewHolder, position: Int) {
        val (adSoyad, devamsizlikSayisi) = devamsizlikListesi[position]

        holder.ogrenciAdiText.text = "Öğrenci: $adSoyad"
        holder.devamsizlikSayisiText.text = "Devamsızlık: $devamsizlikSayisi gün"
    }

    override fun getItemCount(): Int {
        return devamsizlikListesi.size
    }
}

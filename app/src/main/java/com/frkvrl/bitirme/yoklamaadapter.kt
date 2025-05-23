package com.frkvrl.bitirme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class yoklamaadapter(private val items: List<OgrenciYoklama>) :
    RecyclerView.Adapter<yoklamaadapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val adSoyadText: TextView = view.findViewById(R.id.adSoyadText)
        val katilimDurumText: TextView = view.findViewById(R.id.katilimDurumText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ogrenci, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ogrenci = items[position]
        holder.adSoyadText.text = ogrenci.adSoyad
        holder.katilimDurumText.text = if (ogrenci.katildiMi) "Katıldı" else "Katılmadı"
    }
}

package com.frkvrl.bitirme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DevamsizlikAdapter(private val items: List<OgrenciYoklama>) :
    RecyclerView.Adapter<DevamsizlikAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val adSoyadText: TextView = view.findViewById(R.id.tvAdSoyad)
        val numaraText: TextView = view.findViewById(R.id.tvNumara)
        val devamsizlikSayisiText: TextView = view.findViewById(R.id.tvDurum) // "tvDurum" ID'sini devamsızlık için kullanacağız
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_yoklama, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ogrenci = items[position]
        holder.adSoyadText.text = ogrenci.adSoyad
        holder.numaraText.text = ogrenci.numara
        holder.devamsizlikSayisiText.text = "Devamsızlık: ${ogrenci.devamsizlikSayisi}"
    }
}
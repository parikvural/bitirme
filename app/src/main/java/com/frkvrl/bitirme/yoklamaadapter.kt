package com.frkvrl.bitirme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class yoklamaadapter(private val items: List<OgrenciYoklama>) :
    RecyclerView.Adapter<yoklamaadapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Bu TextView'ların item_yoklama.xml içinde doğru ID'lere sahip olduğundan emin olun.
        // Eğer findViewById null dönerse ve burada .text'e erişilirse crash olur.
        // Olası NullPointerException'ı engellemek için lateinit veya nullable TextView'lar kullanılabilir,
        // ancak en iyi çözüm layout dosyasının doğru olduğundan emin olmaktır.
        val adSoyadText: TextView = view.findViewById(R.id.tvAdSoyad)
        val numaraText: TextView = view.findViewById(R.id.tvNumara)
        val katilimDurumText: TextView = view.findViewById(R.id.tvDurum)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // item_yoklama.xml layout dosyasının doğru olduğundan emin olun.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_yoklama, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ogrenci = items[position]
        // TextView'ların null olmadığından emin olmak için güvenli çağrılar kullanabiliriz,
        // ancak bu, layout hatasını maskeleyebilir. En iyi yol, layout'un doğru olmasıdır.
        holder.adSoyadText.text = ogrenci.adSoyad
        holder.numaraText.text = ogrenci.numara
        holder.katilimDurumText.text = if (ogrenci.katildiMi) "Katıldı" else "Katılmadı"
    }
}

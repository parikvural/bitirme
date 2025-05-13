// com.frkvrl.bitirme içindeki bir dosya, örneğin UserAdapter.kt
package com.frkvrl.bitirme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
// Eğer Glide kullanıyorsanız, bu import'u ekleyin:
import com.bumptech.glide.Glide
// Eğer R.drawable'da default_profile_image veya error_profile_image varsa import edin
// import com.frkvrl.bitirme.R // Genellikle otomatik eklenir

class UserAdapter(private val userList: MutableList<User>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    // Her bir satırdaki view'ları tutar
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)

    }

    // Yeni satır view'ları oluşturulduğunda çağrılır
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_user, parent, false) // list_item_user.xml layout dosyanızın adı
        return UserViewHolder(itemView)
    }

    // Veriyi view'lara bağlar
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]

        holder.textViewName.text = "${currentUser.ad} ${currentUser.soyad}"

    }

    // Listedeki toplam öğe sayısını döner
    override fun getItemCount() = userList.size

    // Adapter'ın verisini güncellemek için yardımcı fonksiyon
    fun updateData(newList: List<User>) {
        userList.clear()
        userList.addAll(newList)
        notifyDataSetChanged()
    }
}

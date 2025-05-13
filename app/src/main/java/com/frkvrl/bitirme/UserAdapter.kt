package com.frkvrl.bitirme

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(private var userList: List<User>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_user, parent, false)
        return UserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        val ad = currentUser.ad ?: "Ad Yok"
        val soyad = currentUser.soyad ?: "Soyad Yok"

        holder.textViewName.text = "$ad $soyad"
        Log.d("FirebaseData", "onBindViewHolder: $ad $soyad")
    }

    override fun getItemCount() = userList.size

    fun updateData(newList: List<User>) {
        userList = newList // ÖNEMLİ: Referansı güncelle
        notifyDataSetChanged()
    }
}

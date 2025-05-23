package com.frkvrl.bitirme

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class yoklamalistesi : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yoklamalistesi)

        val listView: ListView = findViewById(R.id.listView)

        val lessonCode = intent.getStringExtra("dersID") ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val attendanceRef = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("attendances/$lessonCode/$date")
        val userRef = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("users")

        attendanceRef.get().addOnSuccessListener { snapshot ->
            val attendanceList = mutableListOf<String>()
            val uids = snapshot.children.map { it.key to it.getValue(Boolean::class.java) }

            if (uids.isEmpty()) {
                Toast.makeText(this, "Yoklama verisi bulunamadı.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            var fetchedCount = 0
            for ((uid, isPresent) in uids) {
                if (uid == null) continue

                userRef.child(uid).get().addOnSuccessListener { userSnapshot ->
                    val name = userSnapshot.child("ad").getValue(String::class.java) ?: "Bilinmeyen"
                    val surname = userSnapshot.child("soyad").getValue(String::class.java) ?: ""
                    val fullName = "$name $surname"
                    val status = if (isPresent == true) "Katıldı" else "Katılmadı"

                    attendanceList.add("$fullName - $status")

                    fetchedCount++
                    if (fetchedCount == uids.size) {
                        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, attendanceList.sorted())
                        listView.adapter = adapter
                    }
                }.addOnFailureListener {
                    attendanceList.add("Bilinmeyen - Katılma durumu alınamadı")
                    fetchedCount++
                    if (fetchedCount == uids.size) {
                        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, attendanceList.sorted())
                        listView.adapter = adapter
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Yoklama verileri alınamadı: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}

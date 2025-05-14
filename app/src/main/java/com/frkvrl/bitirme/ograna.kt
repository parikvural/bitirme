package com.frkvrl.bitirme

import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ograna : ogrnavbar() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private var userList = mutableListOf<User>()

    private val database = Firebase.database("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
    private val usersRef = database.getReference("users")

    // ⬇️ Listener sınıf seviyesinde tanımlı
    private val userValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d("FirebaseData", "Veri alındı. Mevcut: ${snapshot.exists()}")
            userList.clear()

            // Giriş yapan kullanıcının UID'sini al
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid

            // Eğer giriş yapan kullanıcı varsa
            if (currentUid != null) {
                var isUserFound = false  // Kullanıcı bulunup bulunmadığını kontrol etmek için değişken

                // Firebase veritabanındaki her bir kullanıcıyı kontrol et
                for (userSnap in snapshot.children) {
                    val user = userSnap.getValue(User::class.java)

                    // Eğer user objesi null değilse ve UID'ler eşleşiyorsa, listeye ekle
                    if (user != null && user.uid == currentUid) {
                        Log.d("FirebaseData", "Aktif Kullanıcı: ${user.ad} ${user.soyad}")
                        userList.add(user)
                        isUserFound = true  // Kullanıcı bulundu olarak işaretle
                        break  // Giriş yapan kullanıcı bulundu, daha fazla arama yapma
                    }
                }

                // Eğer giriş yapan kullanıcı bulunduysa, veriyi güncelle ve adapter'ı yenile
                if (isUserFound) {
                    userAdapter.updateData(userList)
                    Log.d("FirebaseData", "Aktif Kullanıcı bulundu ve Adapter güncellendi.")
                } else {
                    Log.w("FirebaseData", "Giriş yapan kullanıcı bulunamadı.")
                }
            } else {
                Log.w("FirebaseData", "Giriş yapan kullanıcı bulunamadı.")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.w("FirebaseData", "Veri alınamadı: ${error.toException()}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ograna)

        recyclerView = findViewById(R.id.recyclerViewUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(userList)
        recyclerView.adapter = userAdapter

        startFetchingUsers()
    }

    private fun startFetchingUsers() {
        // Kullanıcıları Firebase'den almak için listener ekleniyor
        usersRef.addValueEventListener(userValueEventListener)
        Log.d("FirebaseData", "ValueEventListener eklendi, veri bekleniyor...")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Aktivite yok edilirken dinleyiciyi kaldırıyoruz
        usersRef.removeEventListener(userValueEventListener)
        Log.d("FirebaseData", "ValueEventListener kaldırıldı.")
    }
}

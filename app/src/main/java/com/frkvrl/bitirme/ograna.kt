package com.frkvrl.bitirme

import android.os.Bundle
import android.util.Log // Loglama için
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.frkvrl.bitirme.UserAdapter
import com.bumptech.glide.Glide

// Eğer ogrnavbar'dan miras alıyorsanız
class ograna : ogrnavbar() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>() // Adapter'ın kullanacağı liste

    // Firebase veritabanı referansı (Global veya sınıf içinde tanımlanır)
    private val database = Firebase.database("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
    private val usersRef = database.getReference("users") // Veritabanınızdaki kullanıcıların bulunduğu ana yol

    // Veri dinleyicisi (Lifecycle'a göre yönetilmesi gerekir)
    private val userValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // Bu metod, veri geldiğinde veya değiştiğinde çağrılır.

            // Önce mevcut listeyi temizle
            userList.clear()

            // DataSnapshot içindeki her bir çocuğu (yani her bir kullanıcıyı) gez
            for (userSnapshot in dataSnapshot.children) {
                // Her çocuğu User veri modelimize dönüştürmeye çalış
                val user = userSnapshot.getValue(User::class.java)
                // Eğer dönüşüm başarılıysa ve user null değilse listeye ekle
                if (user != null) {
                    userList.add(user)
                }
            }

            // RecyclerView adapter'ını yeni verilerle güncelle
            userAdapter.updateData(userList)

            Log.d("FirebaseData", "Veri başarıyla çekildi. Toplam kullanıcı: ${userList.size}")
        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Veri okuma başarısız olursa bu metod çağrılır
            Log.w("FirebaseData", "Veri okuma hatası:", databaseError.toException())
            // Kullanıcıya bir hata mesajı gösterebilirsiniz
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Zaten yapılıyordu
        setContentView(R.layout.ograna)

        // RecyclerView'ı bulma
        recyclerView = findViewById(R.id.recyclerViewUsers) // Layout dosyanızdaki RecyclerView'ın ID'si

        // Adapter ve LayoutManager ayarlama
        // Başlangıçta boş bir liste ile adapter'ı oluştur
        userAdapter = UserAdapter(userList) // userList'i burada boş olarak veriyoruz, veri geldikçe güncellenecek
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = userAdapter

        // Edge-to-Edge için Insetleri RecyclerView'a uygulama (önceki örnekteki gibi)
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        // Firebase'den veriyi çekmeye başla ve listener'ı ekle
        startFetchingUsers()
    }

    private fun startFetchingUsers() {
        // usersRef yoluna ValueEventListener'ı ekle
        usersRef.addValueEventListener(userValueEventListener)
        Log.d("FirebaseData", "ValueEventListener eklendi, veri bekleniyor...")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Aktivite yok edilirken listener'ı kaldırmayı unutmayın!
        // Bu bellek sızıntılarını önler ve gereksiz dinlemeyi durdurur.
        usersRef.removeEventListener(userValueEventListener)
        Log.d("FirebaseData", "ValueEventListener kaldırıldı.")
    }
}

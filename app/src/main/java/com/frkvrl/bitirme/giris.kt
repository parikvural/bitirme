package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.SetOptions // SetOptions için import

class giris : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDb: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.giris)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDb = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")

        val emailEdit = findViewById<EditText>(R.id.edit)
        val passwordEdit = findViewById<EditText>(R.id.edit2)
        val btnKayit = findViewById<TextView>(R.id.button3)
        val btnSifremiUnuttum = findViewById<TextView>(R.id.textView8)

        btnKayit.setOnClickListener {
            startActivity(Intent(this, kayit::class.java))
        }

        btnSifremiUnuttum.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful && task.result.signInMethods?.isNotEmpty() == true) {
                    auth.sendPasswordResetEmail(email).addOnSuccessListener {
                        Toast.makeText(this, "Şifre sıfırlama bağlantısı gönderildi.", Toast.LENGTH_LONG).show()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Gönderim başarısız: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Bu e-posta ile kayıtlı kullanıcı bulunamadı.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Eğer kullanıcı zaten giriş yaptıysa direkt yönlendir
        auth.currentUser?.let { user ->
            val uid = user.uid
            // Cihaz kontrolü yapmadan direkt yönlendirme,
            // çünkü cihaz kontrolü her girişte yapılacak
            kontrolVeYonlendir(uid)
        }
    }

    fun giris(view: View) {
        val email = findViewById<EditText>(R.id.edit).text.toString().trim()
        val password = findViewById<EditText>(R.id.edit2).text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email ve şifre giriniz", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    // Yeni cihaz kontrol ve kayıt mantığı
                    cihazKaydet(uid)
                } else {
                    Toast.makeText(this, "Kullanıcı bilgisi alınamadı.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Giriş başarısız: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Yeni cihaz kayıt fonksiyonu
    private fun cihazKaydet(uid: String) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        // Adım 1: Bu cihazın başka bir kullanıcıya kayıtlı olup olmadığını kontrol et (mevcut mantık)
        val deviceLinkRef = firestore.collection("device_links").document(deviceId)
        deviceLinkRef.get().addOnSuccessListener { doc ->
            val kayitliUid = doc.getString("uid")

            if (kayitliUid != null && kayitliUid != uid) {
                // Bu cihaz başka bir kullanıcıya kayıtlı ve farklı bir kullanıcı giriş yapmaya çalışıyor
                Toast.makeText(this, "Bu cihaz başka kullanıcıya kayıtlı!", Toast.LENGTH_LONG).show()
                auth.signOut()
                return@addOnSuccessListener
            }

            // Adım 2: Kullanıcının başka bir cihazda oturum açıp açmadığını kontrol et (yeni mantık)
            val userActiveDeviceRef = realtimeDb.getReference("users").child(uid).child("active_device")
            userActiveDeviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val activeDeviceId = snapshot.getValue(String::class.java)

                    if (activeDeviceId != null && activeDeviceId != deviceId) {
                        // Kullanıcı başka bir cihazda oturum açmış
                        Toast.makeText(this@giris, "Bu hesap zaten başka bir cihazda açık!", Toast.LENGTH_LONG).show()
                        auth.signOut()
                        return // İşlemi sonlandır
                    }

                    // Adım 3: Tüm kontroller başarılı, cihazı kaydet ve yönlendir
                    // Cihazı bu kullanıcıya bağla (Firestore)
                    deviceLinkRef.set(mapOf("uid" to uid))
                        .addOnSuccessListener {
                            // Kullanıcının aktif cihazını güncelle (Realtime Database)
                            userActiveDeviceRef.setValue(deviceId)
                                .addOnSuccessListener {
                                    kontrolVeYonlendir(uid)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this@giris, "Aktif cihaz kaydı başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
                                    auth.signOut()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@giris, "Cihaz eşleştirme kaydı başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@giris, "Aktif cihaz kontrolü başarısız: ${error.message}", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            })

        }.addOnFailureListener { e ->
            Toast.makeText(this, "Firestore hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            auth.signOut()
        }
    }

    private fun kontrolVeYonlendir(uid: String) {
        val userRef = realtimeDb.getReference("users/$uid")

        userRef.get().addOnSuccessListener { snapshot ->
            val rol = snapshot.child("rol").getValue(String::class.java)

            when (rol) {
                "Öğrenci" -> {
                    startActivity(Intent(this, ograna::class.java))
                    finish()
                }
                "Öğretmen" -> {
                    startActivity(Intent(this, ogrtana::class.java))
                    finish()
                }
                else -> {
                    Toast.makeText(this, "Kullanıcı rolü tanımsız", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Kullanıcı bilgisi alınamadı: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

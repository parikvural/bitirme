package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private lateinit var auth: FirebaseAuth

class giris : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.giris)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Eğer kullanıcı zaten giriş yaptıysa doğrudan ana ekrana yönlendir
        auth.currentUser?.let { user ->
            val email = user.email ?: return@let
            when {
                email.endsWith("@ogr.edu.deu.tr") -> {
                    startActivity(Intent(this, ograna::class.java))
                    finish()
                }
                email.endsWith("@gmail.com") -> {
                    startActivity(Intent(this, ogrtana::class.java))
                    finish()
                }
            }
        }
    }

    fun giris(view: View) {
        val emailEditText = findViewById<EditText>(R.id.edit)
        val passwordEditText = findViewById<EditText>(R.id.edit2)
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Lütfen email ve şifrenizi girin.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userEmail = user?.email
                    val uid = user?.uid
                    val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

                    if (userEmail != null && uid != null) {
                        // Cihaz bu kullanıcıya ait mi?
                        firestore.collection("device_links")
                            .document(deviceId)
                            .get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    val savedUid = doc.getString("uid")
                                    if (savedUid == uid) {
                                        yonlendir(userEmail)
                                    } else {
                                        Toast.makeText(this, "Bu cihaz sadece ilk giriş yapan kullanıcıya özeldir.", Toast.LENGTH_LONG).show()
                                        auth.signOut()
                                    }
                                } else {
                                    // İlk kez giriş yapılıyor, eşleştirme kaydediliyor
                                    firestore.collection("device_links")
                                        .document(deviceId)
                                        .set(mapOf("uid" to uid))
                                        .addOnSuccessListener {
                                            yonlendir(userEmail)
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Cihaz eşleştirme başarısız.", Toast.LENGTH_SHORT).show()
                                            auth.signOut()
                                        }
                                }
                            }

                    } else {
                        Toast.makeText(this, "Kullanıcı bilgisi alınamadı.", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                    }

                } else {
                    Toast.makeText(this, "Giriş başarısız: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun yonlendir(userEmail: String) {
        when {
            userEmail.endsWith("@ogr.edu.deu.tr") -> {
                startActivity(Intent(this, ograna::class.java))
                finish()
            }
            userEmail.endsWith("@gmail.com") -> {
                startActivity(Intent(this, ogrtana::class.java))
                finish()
            }
            else -> {
                Toast.makeText(this, "E-posta adresi tanınmıyor.", Toast.LENGTH_LONG).show()
                auth.signOut()
            }
        }
    }
}

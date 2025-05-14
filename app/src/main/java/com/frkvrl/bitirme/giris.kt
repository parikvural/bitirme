package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

private lateinit var auth: FirebaseAuth

class giris : AppCompatActivity() {

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
                    Log.d("Giris", "signInWithEmail:success")
                    val user = auth.currentUser
                    val userEmail = user?.email

                    if (userEmail != null) {
                        when {
                            userEmail.endsWith("@ogr.edu.deu.tr") -> {
                                // Öğrenci
                                val intent = Intent(this, ograna::class.java)
                                startActivity(intent)
                                finish()
                            }
                            userEmail.endsWith("@gmail.com") -> {
                                // Öğretmen
                                val intent = Intent(this, ogrtana::class.java)
                                startActivity(intent)
                                finish()
                            }
                            else -> {
                                Toast.makeText(this, "E-posta adresi tanınmıyor.", Toast.LENGTH_LONG).show()
                                auth.signOut()
                            }
                        }
                    } else {
                        Toast.makeText(this, "E-posta alınamadı.", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                    }

                } else {
                    Log.w("Giris", "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Giriş başarısız: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }
}

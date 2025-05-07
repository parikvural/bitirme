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
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

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
        // EditText'lerden girilen email ve şifreyi al
        val emailEditText = findViewById<EditText>(R.id.edit)
        val passwordEditText = findViewById<EditText>(R.id.edit2)
        val email = emailEditText.text.toString().trim() // Boşlukları temizle
        val password = passwordEditText.text.toString().trim() // Boşlukları temizle

        // Email veya şifre boş mu kontrol et
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Lütfen email ve şifrenizi girin.", Toast.LENGTH_SHORT).show()
            return // Fonksiyonu burada sonlandır
        }

        // TODO: İsteğe bağlı olarak bir yükleme göstergesi (ProgressBar gibi) gösterebilirsiniz
        // showLoadingSpinner()

        // Firebase Authentication ile giriş yapma isteği gönder
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // TODO: Yükleme göstergesini gizle (hideLoadingSpinner())

                if (task.isSuccessful) {
                    // Giriş başarılı oldu!
                    Log.d("Giris", "signInWithEmail:success")
                    val user = auth.currentUser // Giriş yapmış kullanıcı nesnesini alabilirsiniz (isteğe bağlı)

                    // Kullanıcıyı bir sonraki ekrana yönlendir
                    val intent = Intent(this, ogrtana::class.java)
                    startActivity(intent)
                    finish() // Giriş Activity'sini kapat ki geri tuşuyla dönülmesin

                } else {
                    // Giriş başarısız oldu. task.exception hatanın nedenini içerir.
                    Log.w("Giris", "signInWithEmail:failure", task.exception)

                    // Kullanıcıya hata mesajını göster
                    // task.exception?.localizedMessage genellikle kullanıcı dostu bir hata mesajı içerir (örn: "Wrong password.", "There is no user record corresponding to this identifier.")
                    Toast.makeText(this, "Giriş başarısız: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG).show() // Uzun göster ki kullanıcı okuyabilsin
                }
            }
    }

    // TODO: İsteğe bağlı: Kullanıcı zaten giriş yapmış mı kontrolü (onStart metodunda yapılabilir)
    /*
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Kullanıcı daha önce giriş yapmış ve oturumu hala aktifse, direkt ana ekrana yönlendir
            val intent = Intent(this, ogrtana::class.java)
            startActivity(intent)
            finish()
        }
    }
    */

    // TODO: showLoadingSpinner() ve hideLoadingSpinner() fonksiyonlarını UI durumunu yönetmek için ekleyebilirsiniz.
}





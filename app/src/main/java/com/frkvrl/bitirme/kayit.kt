package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.frkvrl.bitirme.databinding.ActivityKayitBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class kayit : AppCompatActivity() {

    private lateinit var binding: ActivityKayitBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var dersCheckBoxMap: MutableMap<String, CheckBox> // Anahtar formatı: "SINIF_DERSKODU"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKayitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        dersCheckBoxMap = mutableMapOf()

        setupSpinners()

        binding.btnKayitOl.setOnClickListener { kayitOl() }
        binding.tvTeacherRegisterLink.setOnClickListener {
            val intent = Intent(this, ogrtkayit::class.java)
            startActivity(intent)
        }

        // Giriş Ekranına Dön butonuna tıklama dinleyicisi ekle
        binding.btnGoToLogin.setOnClickListener {
            val intent = Intent(this, giris::class.java) // giris.kt Activity'sine yönlendir
            startActivity(intent)
            finish() // Mevcut kayıt aktivitesini kapat
        }
    }

    private fun setupSpinners() {
        // Bölüm Spinner
        database.getReference("bolumler").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bolumList = snapshot.children.mapNotNull { it.key }
                val bolumAdapter = ArrayAdapter(this@kayit, R.layout.spinner_item, bolumList)
                bolumAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                binding.spinnerBolum.adapter = bolumAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@kayit, "Bölümler yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Sınıf Spinner
        database.getReference("dersler").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sinifList = snapshot.children.mapNotNull { it.key }
                val sinifAdapter = ArrayAdapter(this@kayit, R.layout.spinner_item, sinifList)
                sinifAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                binding.spinnerSinif.adapter = sinifAdapter

                binding.spinnerSinif.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val sinif = sinifList[position]
                        dersleriYukle(sinif)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@kayit, "Sınıflar yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun dersleriYukle(secilenSinif: String) {
        val dersRef = database.getReference("dersler/$secilenSinif")
        dersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.layoutDersler.removeAllViews()
                dersCheckBoxMap.clear()

                if (!snapshot.exists()) {
                    Toast.makeText(this@kayit, "Bu sınıfa ait ders bulunamadı.", Toast.LENGTH_SHORT).show()
                    return
                }

                for (ders in snapshot.children) {
                    val dersKodu = ders.key ?: continue
                    val dersAdi = ders.child("ad").getValue(String::class.java) ?: dersKodu
                    val checkBox = CheckBox(this@kayit).apply {
                        text = "$dersKodu - $dersAdi"
                        textSize = 16f
                        setTextColor(ContextCompat.getColor(this@kayit, R.color.onPrimary))
                    }
                    binding.layoutDersler.addView(checkBox)
                    // Anahtarı "SINIF_DERSKODU" formatında sakla
                    dersCheckBoxMap["${secilenSinif}_$dersKodu"] = checkBox
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@kayit, "Dersler yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun temizleVeKucult(metin: String): String {
        return metin.lowercase(Locale.getDefault())
            .replace("ç", "c").replace("ğ", "g").replace("ı", "i")
            .replace("ö", "o").replace("ş", "s").replace("ü", "u")
    }

    private fun kayitOl() {
        val adInput = binding.etAd.text.toString().trim()
        val soyadInput = binding.etSoyad.text.toString().trim()
        val numara = binding.etNumara.text.toString().trim()
        val sinif = binding.spinnerSinif.selectedItem?.toString() ?: ""
        val bolum = binding.spinnerBolum.selectedItem?.toString() ?: ""

        if (adInput.isEmpty() || soyadInput.isEmpty() || numara.isEmpty() || sinif.isEmpty() || bolum.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
            return
        }

        // İsim ve Soyismin ilk harflerini büyük yap
        val ad = temizleVeKucult(adInput).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val soyad = temizleVeKucult(soyadInput).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val mail = "${temizleVeKucult(adInput)}.${temizleVeKucult(soyadInput)}@ogr.deu.edu.tr"
        val sifre = numara

        // Seçilen dersleri sınıfa göre iç içe Map olarak oluştur
        val secilenDerslerByClass = mutableMapOf<String, MutableMap<String, Boolean>>()
        dersCheckBoxMap.filterValues { it.isChecked }.forEach { (key, _) ->
            val parts = key.split("_", limit = 2) // "SINIF_DERSKODU" formatını ayır
            if (parts.size == 2) {
                val currentSinif = parts[0]
                val dersKodu = parts[1]
                secilenDerslerByClass.getOrPut(currentSinif) { mutableMapOf() }[dersKodu] = true
            }
        }

        // Öğrencinin sadece bir sınıftan ders seçtiğini doğrula
        val distinctClasses = secilenDerslerByClass.keys.distinct()
        if (distinctClasses.size > 1) {
            Toast.makeText(this, "Öğrenciler sadece bir sınıftan ders seçebilir.", Toast.LENGTH_LONG).show()
            return
        }
        // Eğer ders seçilmişse ve tek bir sınıf varsa, o sınıfı sinif değişkenine ata
        val finalSinif = distinctClasses.firstOrNull() ?: sinif // Eğer ders seçilmemişse spinner'dan gelen sinif'i kullan

        // Hiç ders seçilip seçilmediğini kontrol et
        if (secilenDerslerByClass.isEmpty()) {
            Toast.makeText(this, "Lütfen en az bir ders seçin.", Toast.LENGTH_SHORT).show()
            return
        }


        auth.createUserWithEmailAndPassword(mail, sifre).addOnSuccessListener { authResult ->
            val uid = authResult.user?.uid ?: return@addOnSuccessListener
            val userMap = mapOf(
                "ad" to ad,
                "soyad" to soyad,
                "numara" to numara,
                "mail" to mail,
                "bolum" to bolum,
                "sinif" to finalSinif, // Sınıf bilgisini güncellenmiş ders seçiminden al
                "rol" to "Öğrenci",
                "dersler" to secilenDerslerByClass // İç içe Map olarak kaydet
            )

            val kullaniciRef = database.getReference("users").child(uid)
            kullaniciRef.setValue(userMap).addOnSuccessListener {
                val derslerRef = database.getReference("dersler")
                for ((sinifAdi, derslerMap) in secilenDerslerByClass) { // Sınıf bazında dersleri işle
                    for ((dersId, _) in derslerMap) {
                        derslerRef.child(sinifAdi).child(dersId).child("ogrenciler").child(uid).setValue(true)
                    }
                }

                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                firestore.collection("device_links").document(deviceId)
                    .set(mapOf("uid" to uid))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Kayıt başarılı ✅", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, ograna::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Cihaz eşleştirme başarısız: ${it.message}", Toast.LENGTH_LONG).show()
                    }

            }.addOnFailureListener {
                Toast.makeText(this, "Veri kaydedilemedi: ${it.message}", Toast.LENGTH_SHORT).show()
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Kayıt başarısız: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}

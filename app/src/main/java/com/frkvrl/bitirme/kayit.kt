package com.frkvrl.bitirme

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.frkvrl.bitirme.databinding.ActivityKayitBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class kayit : AppCompatActivity() {

    private lateinit var binding: ActivityKayitBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var dersCheckBoxMap: MutableMap<String, CheckBox>

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
    }

    private fun setupSpinners() {
        // Bölümler
        database.getReference("bolumler").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bolumList = snapshot.children.mapNotNull { it.key }
                val bolumAdapter = ArrayAdapter(this@kayit, R.layout.spinner_item, bolumList)
                bolumAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerBolum.adapter = bolumAdapter
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Sınıflar ve dersler
        database.getReference("dersler").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sinifList = snapshot.children.mapNotNull { it.key }
                val sinifAdapter = ArrayAdapter(this@kayit, R.layout.spinner_item, sinifList)
                sinifAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerSinif.adapter = sinifAdapter

                binding.spinnerSinif.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val sinif = sinifList[position]
                        dersleriYukle(sinif)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun dersleriYukle(secilenSinif: String) {
        val dersRef = database.getReference("dersler/$secilenSinif")
        dersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.layoutDersler.removeAllViews()
                dersCheckBoxMap.clear()

                if (!snapshot.exists()) {
                    Toast.makeText(this@kayit, "Ders bulunamadı.", Toast.LENGTH_SHORT).show()
                    return
                }

                for (ders in snapshot.children) {
                    val dersKodu = ders.key ?: continue
                    val dersAdi = ders.child("ad").getValue(String::class.java) ?: dersKodu
                    val checkBox = CheckBox(this@kayit).apply {
                        text = "$dersKodu - $dersAdi"
                        textSize = 16f // Yazı boyutu (sp cinsinden)
                        setTextColor(resources.getColor(android.R.color.white, theme))
                    }
                    binding.layoutDersler.addView(checkBox)
                    dersCheckBoxMap[dersKodu] = checkBox
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@kayit, "Veri alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun temizleVeKucult(metin: String): String {
        return metin.lowercase()
            .replace("ç", "c").replace("ğ", "g").replace("ı", "i")
            .replace("ö", "o").replace("ş", "s").replace("ü", "u")
    }

    private fun kayitOl() {
        val ad = binding.etAd.text.toString().trim()
        val soyad = binding.etSoyad.text.toString().trim()
        val numara = binding.etNumara.text.toString().trim()
        val sinif = binding.spinnerSinif.selectedItem?.toString() ?: ""
        val bolum = binding.spinnerBolum.selectedItem?.toString() ?: ""

        if (ad.isEmpty() || soyad.isEmpty() || numara.isEmpty() || sinif.isEmpty() || bolum.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
            return
        }

        val mail = "${temizleVeKucult(ad)}.${temizleVeKucult(soyad)}@ogr.deu.edu.tr"
        val sifre = numara

        val secilenDersler = dersCheckBoxMap.filterValues { it.isChecked }.mapValues { true }

        auth.createUserWithEmailAndPassword(mail, sifre).addOnSuccessListener { authResult ->
            val uid = authResult.user?.uid ?: return@addOnSuccessListener
            val userMap = mapOf(
                "ad" to ad,
                "soyad" to soyad,
                "numara" to numara,
                "mail" to mail,
                "bolum" to bolum,
                "sinif" to sinif,
                "rol" to "Öğrenci",
                "dersler" to secilenDersler
            )

            val kullaniciRef = database.getReference("users").child(uid)
            kullaniciRef.setValue(userMap).addOnSuccessListener {
                val derslerRef = database.getReference("dersler")
                for ((dersId, _) in secilenDersler) {
                    derslerRef.child(sinif).child(dersId).child("ogrenciler").child(uid).setValue(true)
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

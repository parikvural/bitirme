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
import com.frkvrl.bitirme.databinding.ActivityOgrtkayitBinding // Doğru binding sınıfını kullanın
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ogrtkayit : AppCompatActivity() {

    private lateinit var binding: ActivityOgrtkayitBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var dersCheckBoxMap: MutableMap<String, CheckBox> // Anahtar formatı: "SINIF_DERSKODU"

    // Yönetici kodu (Güvenlik için Firebase'den çekilmesi önerilir)
    private val ADMIN_CODE = "ozer_kestane" // Örnek kod, gerçek uygulamada daha güvenli bir yöntem kullanın

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOgrtkayitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase örneklerini başlat
        database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        dersCheckBoxMap = mutableMapOf()

        // Spinner'ları (sadece Bölüm) ayarla ve tüm dersleri yükle
        setupSpinnersAndLoadLessons()

        // Kayıt Ol butonuna tıklama dinleyicisi ekle
        binding.btnTeacherKayitOl.setOnClickListener { kayitOlTeacher() }

        // Öğrenci Kayıt Ekranına Dön butonuna tıklama dinleyicisi ekle
        binding.btnGoToStudentRegister.setOnClickListener {
            val intent = Intent(this, kayit::class.java)
            startActivity(intent)
            finish() // Mevcut öğretmen kayıt aktivitesini kapat
        }
    }

    private fun setupSpinnersAndLoadLessons() {
        // Bölümleri Firebase'den çek ve Spinner'a doldur
        database.getReference("bolumler").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bolumList = snapshot.children.mapNotNull { it.key }
                val bolumAdapter = ArrayAdapter(this@ogrtkayit, R.layout.spinner_item, bolumList)
                bolumAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item) // Özel açılır liste öğesini kullan
                binding.spinnerTeacherBolum.adapter = bolumAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ogrtkayit, "Bölümler yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Tüm dersleri doğrudan yükle, sınıf filtresi olmadan
        loadAllDersler()
    }

    // Tüm sınıflardaki dersleri yükle ve CheckBox'ları oluştur
    private fun loadAllDersler() {
        val derslerRef = database.getReference("dersler")
        derslerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.layoutTeacherDersler.removeAllViews() // Önceki dersleri temizle
                dersCheckBoxMap.clear() // Haritayı temizle

                if (!snapshot.exists()) {
                    Toast.makeText(this@ogrtkayit, "Hiç ders bulunamadı.", Toast.LENGTH_SHORT).show()
                    return
                }

                // "dersler" altındaki her sınıfı gez
                for (sinifSnapshot in snapshot.children) {
                    val sinifAdi = sinifSnapshot.key ?: continue // Sınıf adını al (örn: "1. Sınıf")

                    // Sınıf içindeki her dersi gez
                    for (dersSnapshot in sinifSnapshot.children) {
                        val dersKodu = dersSnapshot.key ?: continue
                        val dersAdi = dersSnapshot.child("ad").getValue(String::class.java) ?: dersKodu

                        val checkBox = CheckBox(this@ogrtkayit).apply {
                            text = "$dersKodu - $dersAdi (Sınıf: $sinifAdi)" // Ders kodu, adı ve sınıfını göster
                            textSize = 16f // Yazı boyutu
                            // Metin rengini temanızdaki onPrimary rengine ayarla
                            setTextColor(ContextCompat.getColor(this@ogrtkayit, R.color.onPrimary))
                        }
                        binding.layoutTeacherDersler.addView(checkBox) // CheckBox'ı layout'a ekle
                        dersCheckBoxMap["${sinifAdi}_$dersKodu"] = checkBox // Birleşik anahtarla haritaya kaydet
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ogrtkayit, "Dersler yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Metni küçük harfe çevir ve Türkçe karakterleri İngilizce karşılıklarına dönüştür
    private fun temizleVeKucult(metin: String): String {
        return metin.lowercase(Locale.getDefault())
            .replace("ç", "c").replace("ğ", "g").replace("ı", "i")
            .replace("ö", "o").replace("ş", "s").replace("ü", "u")
    }

    // Öğretmen kayıt işlemini gerçekleştir
    private fun kayitOlTeacher() {
        val adInput = binding.etTeacherAd.text.toString().trim()
        val soyadInput = binding.etTeacherSoyad.text.toString().trim()
        val sifre = binding.etTeacherSifre.text.toString().trim()
        val bolum = binding.spinnerTeacherBolum.selectedItem?.toString() ?: ""
        val adminCode = binding.etAdminCode.text.toString().trim() // Yönetici kodunu al

        // Alanların boş olup olmadığını kontrol et
        if (adInput.isEmpty() || soyadInput.isEmpty() || sifre.isEmpty() || bolum.isEmpty() || adminCode.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
            return
        }

        // Yönetici kodunu doğrula
        if (adminCode != ADMIN_CODE) {
            Toast.makeText(this, "Yönetici kodu hatalı.", Toast.LENGTH_SHORT).show()
            return
        }

        // Şifre uzunluğunu kontrol et (Firebase minimum 6 karakter ister)
        if (sifre.length < 6) {
            Toast.makeText(this, "Şifre en az 6 karakter olmalıdır.", Toast.LENGTH_SHORT).show()
            return
        }

        // İsim ve Soyismin ilk harflerini büyük yap
        val ad = temizleVeKucult(adInput).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val soyad = temizleVeKucult(soyadInput).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        // Öğretmen e-posta adresini oluştur
        val mail = "${temizleVeKucult(adInput)}.${temizleVeKucult(soyadInput)}@deu.edu.tr" // Mail adresi küçük harflerle kalsın

        // Seçilen dersleri sınıfa göre düzenle
        // Map<String, Map<String, Boolean>> -> SınıfAdı -> DersKodu -> true
        val secilenDerslerByClass = mutableMapOf<String, MutableMap<String, Boolean>>()
        dersCheckBoxMap.filterValues { it.isChecked }.forEach { (key, _) ->
            val parts = key.split("_", limit = 2) // "SINIF_DERSKODU" ifadesini ayır
            if (parts.size == 2) {
                val sinifAdi = parts[0]
                val dersKodu = parts[1]
                secilenDerslerByClass.getOrPut(sinifAdi) { mutableMapOf() }[dersKodu] = true
            }
        }

        // Hiç ders seçilip seçilmediğini kontrol et
        if (secilenDerslerByClass.isEmpty()) {
            Toast.makeText(this, "Lütfen en az bir ders seçin.", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase Authentication ile kullanıcı oluştur
        auth.createUserWithEmailAndPassword(mail, sifre).addOnSuccessListener { authResult ->
            val uid = authResult.user?.uid ?: return@addOnSuccessListener // Kullanıcı UID'sini al

            // Kullanıcı verilerini Realtime Database'e kaydet
            val userMap = mapOf(
                "ad" to ad, // Baş harfi büyük ad
                "soyad" to soyad, // Baş harfi büyük soyad
                "mail" to mail,
                "bolum" to bolum,
                "rol" to "Öğretmen", // Rolü "Öğretmen" olarak ayarla
                "dersler" to secilenDerslerByClass // Seçilen dersleri sınıfa göre kaydet
            )

            val kullaniciRef = database.getReference("users").child(uid)
            kullaniciRef.setValue(userMap).addOnSuccessListener {
                // Seçilen dersleri öğretmene bağla
                val derslerRef = database.getReference("dersler")
                for ((sinifAdi, derslerMap) in secilenDerslerByClass) {
                    for ((dersId, _) in derslerMap) {
                        // Burada değişiklik yapıldı: ogretmen_uid olarak kaydediliyor
                        derslerRef.child(sinifAdi).child(dersId).child("ogretmen_uid").setValue(uid)
                    }
                }

                // Cihaz eşleştirmesini Firestore'a kaydet (isteğe bağlı, cihaz başına bir kullanıcı için)
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                firestore.collection("device_links").document(deviceId)
                    .set(mapOf("uid" to uid, "rol" to "Öğretmen")) // Rolü de kaydet
                    .addOnSuccessListener {
                        Toast.makeText(this, "Öğretmen kaydı başarılı ✅", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, ogrtana::class.java)) // Öğretmen ana ekranına yönlendir
                        finish() // Mevcut aktiviteyi kapat
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

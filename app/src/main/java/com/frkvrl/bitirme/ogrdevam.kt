package com.frkvrl.bitirme

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ogrdevam : ogrnavbar() {

    // Yeni TextView'lar ve Button için değişkenler
    private lateinit var tvDersAdi: TextView
    private lateinit var tvToplamDevamsizlik: TextView
    private lateinit var btnGeriDon: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrdevam) // ogrdevam.xml layout dosyasını yüklüyoruz

        // Layout'taki View'ları ID'leri ile bağlıyoruz
        tvDersAdi = findViewById(R.id.tvDersAdi)
        tvToplamDevamsizlik = findViewById(R.id.tvToplamDevamsizlik)
        btnGeriDon = findViewById(R.id.btnGeriDon)

        // Intent'ten ders ID ve sınıf bilgisini alıyoruz
        val dersId = intent.getStringExtra("dersId")
        // ÖNEMLİ: sinif bilgisini ogrbilgi.kt'den doğru almalıyız
        val sinif = intent.getStringExtra("sinif")
        val uid = FirebaseAuth.getInstance().currentUser?.uid // Mevcut kullanıcının UID'sini alıyoruz

        // Geri Dön butonuna tıklama dinleyicisi ekliyoruz
        btnGeriDon.setOnClickListener {
            finish() // Mevcut aktiviteyi kapatıp bir önceki aktiviteye dönüyoruz
        }

        // Debugging için loglama ekleyelim
        Log.d("OgrDevam", "Alınan dersId: $dersId")
        Log.d("OgrDevam", "Alınan sinif: $sinif")
        Log.d("OgrDevam", "Kullanıcı UID: $uid")

        // Gerekli bilgiler null değilse Firebase işlemlerini başlatıyoruz
        if (dersId != null && sinif != null && uid != null) {
            // Sınıf bilgisinin Firebase yapısına uygun olup olmadığını kontrol edelim
            if (sinif == "bilinmiyor" || sinif == "0") {
                Toast.makeText(this, "Hata: Geçersiz sınıf bilgisi ($sinif). Ders adı yüklenemeyebilir.", Toast.LENGTH_LONG).show()
                Log.e("OgrDevam", "Geçersiz sınıf bilgisi: $sinif")
                tvDersAdi.text = "Geçersiz Sınıf"
                tvToplamDevamsizlik.text = "N/A"
                return // İşlemi burada sonlandır
            }

            val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")

            // Ders adını almak için dersler referansına gidiyoruz
            val dersRef = database.getReference("dersler").child(sinif).child(dersId)
            dersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dersAdi = snapshot.child("ad").getValue(String::class.java)
                    Log.d("OgrDevam", "Firebase'den çekilen ders adı: $dersAdi")
                    tvDersAdi.text = dersAdi ?: "Bilinmeyen Ders" // Ders adını ekrana yazdırıyoruz
                }

                override fun onCancelled(error: DatabaseError) {
                    tvDersAdi.text = "Ders Adı Yüklenemedi"
                    Toast.makeText(this@ogrdevam, "Ders adı alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e("OgrDevam", "Ders adı Firebase hatası: ${error.message}")
                }
            })

            // Devamsızlık verilerini almak için attendances referansına gidiyoruz
            val attendanceRef = database.getReference("attendances").child(sinif).child(dersId)

            attendanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var katildigiGunSayisi = 0
                    var toplamDersGunu = 0

                    // Her bir tarih snapshot'ı üzerinden dönerek devamsızlıkları hesaplıyoruz
                    for (dateSnapshot in snapshot.children) {
                        toplamDersGunu++ // Toplam ders gün sayısını artırıyoruz
                        // Kullanıcının o gün derse katılıp katılmadığını kontrol ediyoruz
                        val katilimDurumu = dateSnapshot.child(uid).getValue(Boolean::class.java) ?: false
                        if (katilimDurumu) {
                            katildigiGunSayisi++ // Katıldıysa sayıyı artırıyoruz
                        }
                    }

                    val devamsizlikGunSayisi = toplamDersGunu - katildigiGunSayisi

                    // Hesaplanan değerleri TextView'lara yerleştiriyoruz
                    tvToplamDevamsizlik.text = "$devamsizlikGunSayisi Gün"
                }

                override fun onCancelled(error: DatabaseError) {
                    // Veri alınamadığında hata mesajlarını gösteriyoruz
                    tvToplamDevamsizlik.text = "Yüklenemedi"
                    Toast.makeText(this@ogrdevam, "Devamsızlık verisi alınamadı: ${error.message}", Toast.LENGTH_LONG).show()
                    Log.e("OgrDevam", "Devamsızlık verisi Firebase hatası: ${error.message}")
                }
            })
        } else {
            // Ders ID, sınıf veya UID null ise hata mesajı gösteriyoruz
            tvDersAdi.text = "Bilgi Eksik"
            tvToplamDevamsizlik.text = "N/A"
            Toast.makeText(this, "Ders, sınıf veya kullanıcı bilgisi eksik.", Toast.LENGTH_LONG).show()
            Log.e("OgrDevam", "Ders ID, sınıf veya UID null geldi. dersId: $dersId, sinif: $sinif, uid: $uid")
        }
    }
}

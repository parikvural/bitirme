package com.frkvrl.bitirme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ogrtders : ogrtnavbar() {

    private lateinit var dersListLayout: LinearLayout
    private lateinit var emptyView: TextView
    private val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app")
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ogrtders)

        dersListLayout = findViewById(R.id.dersListLayout)
        emptyView = findViewById(R.id.emptyView)
        auth = FirebaseAuth.getInstance()

        loadTeacherCourses()
    }

    private fun loadTeacherCourses() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            Toast.makeText(this, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            emptyView.text = "Kullanıcı oturumu açık değil."
            emptyView.visibility = View.VISIBLE
            return
        }

        val derslerRef = database.getReference("dersler")
        derslerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dersListLayout.removeAllViews()
                var coursesFound = false

                for (sinifSnapshot in snapshot.children) {
                    val sinifKey = sinifSnapshot.key ?: continue
                    val sinif = sinifKey.trim()

                    Log.d("OgrtDers", "Sınıf: $sinif")

                    for (dersSnapshot in sinifSnapshot.children) {
                        val dersID = dersSnapshot.key ?: continue
                        val ogretmenUid = dersSnapshot.child("ogretmen_uid").getValue(String::class.java)

                        Log.d("OgrtDers", "Ders ID: $dersID, Öğretmen UID: $ogretmenUid")

                        if (ogretmenUid == currentUid) {
                            val dersAdi = dersSnapshot.child("ad").getValue(String::class.java) ?: dersID
                            Log.d("OgrtDers", "Öğretmenin dersi bulundu: $dersAdi ($dersID) Sınıf: $sinif")
                            addCourseToLayout(dersAdi, dersID, sinif)
                            coursesFound = true
                        }
                    }
                }

                if (coursesFound) {
                    emptyView.visibility = View.GONE
                } else {
                    emptyView.text = "Size atanmış ders bulunamadı."
                    emptyView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ogrtders, "Veritabanı hatası: ${error.message}", Toast.LENGTH_SHORT).show()
                emptyView.text = "Dersler yüklenirken bir hata oluştu: ${error.message}"
                emptyView.visibility = View.VISIBLE
                Log.e("OgrtDers", "Dersler yüklenirken Firebase hatası: ${error.message}")
            }
        })
    }

    private fun addCourseToLayout(dersAdi: String, dersId: String, sinif: String) {
        val inflater = LayoutInflater.from(this)
        val dersItemView = inflater.inflate(R.layout.ders_listesi_item, dersListLayout, false)

        val dersAdiTextView: TextView = dersItemView.findViewById(R.id.dersAdiTextView)
        dersAdiTextView.text = "$dersAdi ($sinif. Sınıf)"

        dersItemView.setOnClickListener {
            val intent = Intent(this, ogrtbilgi::class.java)
            intent.putExtra("dersID", dersId)
            intent.putExtra("sinif", sinif)
            startActivity(intent)
        }

        dersListLayout.addView(dersItemView)
    }
}

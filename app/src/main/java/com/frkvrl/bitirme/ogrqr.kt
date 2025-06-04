package com.frkvrl.bitirme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.integration.android.IntentIntegrator

class ogrqr : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var scannedToken: String? = null
    private var scannedTimestamp: Long? = null
    private var scannedDate: String? = null

    private val TARGET_LATITUDE = 38.3875139
    private val TARGET_LONGITUDE = 27.1638606
    private val MAX_DISTANCE_METERS = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startQrScan()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startQrScan()
            } else {
                Toast.makeText(this, "QR kod okumak için kamera izni gerekli.", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    private fun startQrScan() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("QR kodu okutunuz")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val parts = result.contents.split("|")
                if (parts.size == 3) {
                    scannedToken = parts[0]
                    scannedTimestamp = parts[1].toLongOrNull()
                    scannedDate = parts[2]

                    if (scannedTimestamp == null || scannedDate == null) {
                        Toast.makeText(this, "QR kod formatı hatalı.", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }

                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        getLocationAndProceed()
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                } else {
                    Toast.makeText(this, "QR kod formatı hatalı. (Token|Timestamp|Date bekleniyor)", Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "QR kod okuma iptal edildi veya başarısız oldu.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "QR kod tarayıcı başlatılamadı veya bir hata oluştu.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getLocationAndProceed()
            } else {
                Toast.makeText(this, "Konum izni yoklama için gerekli.", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    @SuppressLint("MissingPermission")
    private fun getLocationAndProceed() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val distance = FloatArray(1)
                    Location.distanceBetween(
                        location.latitude, location.longitude,
                        TARGET_LATITUDE, TARGET_LONGITUDE,
                        distance
                    )
                    if (distance[0] <= MAX_DISTANCE_METERS) {
                        continueWithFirebase()
                    } else {
                        Toast.makeText(this, "Belirlenen alanda değilsiniz ❌ (Mesafe: ${String.format("%.2f", distance[0])}m)", Toast.LENGTH_LONG).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Konum bilgisi alınamadı. Lütfen konum servislerini kontrol edin.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Konum alınırken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun continueWithFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "Kullanıcı oturumu bulunamadı.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val currentTime = System.currentTimeMillis()
        val token = scannedToken ?: return
        val timestamp = scannedTimestamp ?: return
        val date = scannedDate ?: return

        val db = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
        val qrRef = db.getReference("qrCodes/current")

        qrRef.get().addOnSuccessListener { snapshot ->
            val firebaseToken = snapshot.child("value").getValue(String::class.java)
            val firebaseTimestamp = snapshot.child("timestamp").getValue(Long::class.java)
            val lessonCode = snapshot.child("lessonCode").getValue(String::class.java)
            val sinif = snapshot.child("sinif").getValue(String::class.java)

            if (token == firebaseToken && firebaseTimestamp != null && lessonCode != null && sinif != null) {
                val isValid = (currentTime - firebaseTimestamp) <= 15000
                if (isValid) {
                    val attendanceRef = db.getReference("attendances/$sinif/$lessonCode/$date/$uid")
                    attendanceRef.get().addOnSuccessListener { attSnap ->
                        val alreadyPresent = attSnap.getValue(Boolean::class.java)
                        if (alreadyPresent == true) {
                            Toast.makeText(this, "Zaten yoklama alınmış ✅", Toast.LENGTH_SHORT).show()
                        } else {
                            attendanceRef.setValue(true).addOnSuccessListener {
                                Toast.makeText(this, "Yoklama başarıyla alındı ✅", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener {
                                Toast.makeText(this, "Yoklama kaydı başarısız: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        finish()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Yoklama durumu kontrol edilemedi: ${e.message}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "QR kodun süresi dolmuş ⏰ (15 saniye geçerli).", Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "Geçersiz veya eşleşmeyen QR kod ❌", Toast.LENGTH_LONG).show()
                finish()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Firebase'den QR verisi alınamadı: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}

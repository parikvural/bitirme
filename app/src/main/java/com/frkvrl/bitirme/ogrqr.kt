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
import java.text.SimpleDateFormat
import java.util.*

class ogrqr : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var scannedToken: String? = null
    private var scannedTimestamp: Long? = null

    private val TARGET_LATITUDE = 38.3875139
    private val TARGET_LONGITUDE = 27.1638606
    private val MAX_DISTANCE_METERS = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
        if (result != null && result.contents != null) {
            val parts = result.contents.split("|")
            if (parts.size == 2) {
                scannedToken = parts[0]
                scannedTimestamp = parts[1].toLongOrNull()

                if (scannedTimestamp == null) {
                    Toast.makeText(this, "Zaman damgası hatalı", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Hatalı QR kod formatı", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "QR kod okunamadı", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getLocationAndProceed()
            } else {
                Toast.makeText(this, "Konum izni gerekli", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this, "Belirlenen alanda değilsiniz ❌", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Konum alınamadı", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Konum hatası", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun continueWithFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val currentTime = System.currentTimeMillis()
        val scannedToken = this.scannedToken ?: return
        val scannedTimestamp = this.scannedTimestamp ?: return

        val database = FirebaseDatabase.getInstance("https://bitirme-cfd2e-default-rtdb.europe-west1.firebasedatabase.app/")
        val qrRef = database.getReference("qrCodes/current")

        qrRef.get().addOnSuccessListener { snapshot ->
            val value = snapshot.child("value").getValue(String::class.java)
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java)
            val lessonCode = snapshot.child("lessonCode").getValue(String::class.java)
            val sinif = snapshot.child("sinif").getValue(String::class.java)

            if (value == scannedToken && timestamp != null && lessonCode != null && sinif != null) {
                val isValid = (currentTime - timestamp) <= 5000
                if (isValid) {
                    val attendanceRef = database.getReference("attendances/$sinif/$lessonCode/${getCurrentDate()}/$uid")
                    attendanceRef.get().addOnSuccessListener { attendanceSnapshot ->
                        val alreadyPresent = attendanceSnapshot.getValue(Boolean::class.java)
                        if (alreadyPresent == true) {
                            Toast.makeText(this, "Zaten yoklama alınmış ✅", Toast.LENGTH_SHORT).show()
                        } else {
                            attendanceRef.setValue(true).addOnSuccessListener {
                                Toast.makeText(this, "Yoklama alındı ✅", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener {
                                Toast.makeText(this, "Yoklama kaydı başarısız", Toast.LENGTH_SHORT).show()
                            }
                        }
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Yoklama kontrolü başarısız", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Kod süresi dolmuş ⏰", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "Geçersiz QR kod ❌", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Firebase hatası", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}

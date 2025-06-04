package com.frkvrl.bitirme

data class OgrenciDevam(
    val adSoyad: String,
    val numara: String,
    val katildiMi: Boolean,
    val devamsizlikSayisi: Int = 0
)

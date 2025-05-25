package com.frkvrl.bitirme

data class User(
    var ad: String? = null,
    var soyad: String? = null,
    var numara: String? = null,
    var mail: String? = null,
    var bolum: String? = null,
    var sinif: String? = null,
    val rol: String? = null,
    val verdigi_dersler : Any? = null,
    val dersler: Map<String, Boolean>? = null,
    val devamsizliklar: Map<String, Boolean>? = null
)



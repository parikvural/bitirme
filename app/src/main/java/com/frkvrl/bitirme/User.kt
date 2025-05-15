package com.frkvrl.bitirme

data class User(
    val ad: String? = null,
    val soyad: String? = null,
    val numara: Long? = null,
    val bolum: String? = null,
    val sinif: Int? = null,
    val rol: String? = null,
    val verdigi_dersler: Map<String, Boolean>? = null,
    val dersler: Map<String, Boolean>? = null,
)



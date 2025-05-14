// com.frkvrl.bitirme içindeki bir dosya, örneğin User.kt
package com.frkvrl.bitirme

data class User(
    val uid: String? = null,
    var ad: String? = null,
    var soyad: String? = null,
) {
    // Boş constructor, Firebase'in veri eşlemesi için gereklidir.
    constructor() : this(null, null)
}

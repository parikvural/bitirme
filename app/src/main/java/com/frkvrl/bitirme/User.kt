// com.frkvrl.bitirme içindeki bir dosya, örneğin User.kt
package com.frkvrl.bitirme

data class User(
    var uid: String? = null,
    var ad: String? = null,
    var soyad: String? = null,
    var numara: Long? = null,
    var bolum: String? = null,
    var sinif: Int? = null
)
 {
    // Boş constructor, Firebase'in veri eşlemesi için gereklidir.
    constructor() : this(null, null,null,null,null,null)
}

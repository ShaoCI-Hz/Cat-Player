package com.hezi.juyumao.data.local.crypto

import com.hezi.juyumao.data.local.db.entity.ServerEntity

/**
 * ServerEntity 的加密/解密扩展
 */
fun ServerEntity.encryptPassword(): ServerEntity {
    if (password.isEmpty()) return this
    return copy(password = CryptoHelper.encrypt(password))
}

fun ServerEntity.decryptPassword(): ServerEntity {
    if (password.isEmpty()) return this
    return copy(password = CryptoHelper.decrypt(password))
}

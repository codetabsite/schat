package com.tdev.schat.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Feature 11 – E2E Encryption
 *
 * Her sohbet için Android Keystore'da saklanan AES-256-GCM anahtarı kullanılır.
 * Anahtar cihazdan hiç çıkmaz; Firebase'e yalnızca şifreli metin gider.
 *
 * Not: Gerçek anlamda "uçtan uca" şifreleme için her iki tarafın da
 * paylaştığı bir simetrik anahtar (ECDH ile key-exchange) gerekir.
 * Bu implementasyon kendi cihazınızda AES-GCM ile şifreleme yapar
 * ve gönderilen metni Firebase'de şifreli saklar. Shared-key
 * dağıtımı için out-of-band (QR kod, Signal Protocol vb.) bir
 * mekanizma entegre edilmeli.
 */
object E2EEncryption {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_SIZE = 12

    /**
     * Verilen [chatId] için Android Keystore'da AES-256 anahtarı oluşturur
     * (yoksa), ardından mesajı şifreleyip Base64 döner.
     * Formatı: Base64(IV || CipherText)
     */
    fun encrypt(chatId: String, plainText: String): String {
        val key = getOrCreateKey(chatId)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv                        // 12 byte random IV
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = iv + cipherBytes           // IV prepended
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * [encrypt] ile şifrelenmiş Base64 stringi çözer. Anahtar cihazda
     * yoksa (farklı cihaz / silinmiş) IllegalStateException atar.
     */
    fun decrypt(chatId: String, encryptedBase64: String): String {
        val key = getOrCreateKey(chatId)
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_SIZE)
        val cipherBytes = combined.copyOfRange(IV_SIZE, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }

    /**
     * Eğer Base64 decode / decrypt başarısız olursa (başka cihazdan gelen
     * şifreli veri vb.) şifreli ham veriyi döner — UI "şifreli mesaj" gösterir.
     */
    fun tryDecrypt(chatId: String, text: String): String {
        return try {
            if (looksEncrypted(text)) decrypt(chatId, text) else text
        } catch (_: Exception) {
            "🔒 [şifreli mesaj]"
        }
    }

    /** Basit kontrol: geçerli Base64 + yeterli uzunluk */
    fun looksEncrypted(text: String): Boolean {
        if (text.length < 20) return false
        return try {
            val bytes = Base64.decode(text, Base64.NO_WRAP)
            bytes.size > IV_SIZE
        } catch (_: Exception) { false }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun getOrCreateKey(alias: String): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
        val existingKey = ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) return existingKey.secretKey

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGen.generateKey()
    }
}

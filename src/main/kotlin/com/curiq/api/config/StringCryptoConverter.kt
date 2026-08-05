package com.curiq.api.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Converter
class StringCryptoConverter : AttributeConverter<String?, String?> {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12

        private val secretKey: SecretKeySpec by lazy {
            val keyString = System.getenv("ENCRYPTION_KEY") ?: "default_insecure_dev_key"
            // Hash the string to ensure we always have exactly 32 bytes for AES-256
            val digest = MessageDigest.getInstance("SHA-256")
            val keyBytes = digest.digest(keyString.toByteArray(Charsets.UTF_8))
            SecretKeySpec(keyBytes, "AES")
        }
    }

    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute.isNullOrEmpty()) return attribute

        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
            val encryptedBytes = cipher.doFinal(attribute.toByteArray(Charsets.UTF_8))
            
            // Combine IV + Encrypted Data and Base64 encode
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            throw RuntimeException("Error encrypting attribute", e)
        }
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData.isNullOrEmpty()) return dbData

        return try {
            val decoded = Base64.getDecoder().decode(dbData)
            
            // Extract IV
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(decoded, 0, iv, 0, iv.size)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            
            // Extract encrypted data
            val encryptedSize = decoded.size - GCM_IV_LENGTH
            val encryptedBytes = ByteArray(encryptedSize)
            System.arraycopy(decoded, GCM_IV_LENGTH, encryptedBytes, 0, encryptedSize)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // If decryption fails (e.g., old unencrypted data), return raw string
            dbData
        }
    }
}

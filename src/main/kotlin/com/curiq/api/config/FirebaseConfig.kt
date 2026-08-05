package com.curiq.api.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.InputStream
import javax.annotation.PostConstruct

@Configuration
class FirebaseConfig {
    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @PostConstruct
    fun initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                val credentialPath = System.getenv("FIREBASE_CREDENTIALS")
                val serviceAccount: InputStream? = if (!credentialPath.isNullOrEmpty()) {
                    try {
                        java.io.FileInputStream(credentialPath)
                    } catch (e: Exception) {
                        logger.error("Could not read Firebase credentials from path: $credentialPath")
                        null
                    }
                } else {
                    this::class.java.getResourceAsStream("/firebase-service-account.json")
                }
                
                if (serviceAccount == null) {
                    logger.warn("Firebase credentials not found! Firebase Auth will fail.")
                    return
                }

                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build()

                FirebaseApp.initializeApp(options)
                logger.info("Firebase Admin SDK initialized successfully.")
            }
        } catch (e: Exception) {
            logger.error("Error initializing Firebase Admin SDK", e)
        }
    }
}

package com.curiq.api.controller

import com.curiq.api.model.RevenueCatWebhookEventEntity
import com.curiq.api.repository.RevenueCatWebhookEventRepository
import com.curiq.api.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.transaction.annotation.Transactional
import java.util.Map

@RestController
@RequestMapping("/api/webhooks/revenuecat")
class RevenueCatWebhookController(
    private val webhookRepo: RevenueCatWebhookEventRepository,
    private val userRepository: UserRepository
) {

    // The Authorization header must match this secret configured in RevenueCat
    @org.springframework.beans.factory.annotation.Value("\${revenuecat.webhook.secret}")
    private lateinit var webhookSecret: String 

    @PostMapping
    @Transactional
    fun handleWebhook(
        @RequestHeader("Authorization", required = false) authHeader: String?,
        @RequestBody payload: Map<String, Any>
    ): ResponseEntity<String> {
        
        if (authHeader != "Bearer " + webhookSecret) {
            return ResponseEntity.status(401).body("Unauthorized")
        }

        val event = payload["event"] as? Map<String, Any> ?: return ResponseEntity.badRequest().build()
        val eventId = event["id"] as? String ?: return ResponseEntity.badRequest().build()
        val eventType = event["type"] as? String ?: return ResponseEntity.badRequest().build()
        val appUserId = event["app_user_id"] as? String ?: return ResponseEntity.badRequest().build()
        
        // Idempotency check: if event already processed, return 200 immediately
        if (webhookRepo.existsById(eventId)) {
            return ResponseEntity.ok("Already processed")
        }

        val userOpt = userRepository.findByFirebaseUidForUpdate(appUserId)
        if (userOpt.isPresent) {
            val user = userOpt.get()
            
            when (eventType) {
                "INITIAL_PURCHASE", "RENEWAL" -> {
                    user.subscriptionStatus = "ACTIVE"
                    val expirationAtMs = event["expiration_at_ms"] as? Long
                    user.subscriptionExpiresAt = expirationAtMs
                }
                "CANCELLATION" -> {
                    user.subscriptionStatus = "CANCELLED"
                    // expiration_at_ms should still be valid, so we leave it.
                    // The user will lose access only when expirationAtMs passes.
                }
                "EXPIRATION" -> {
                    user.subscriptionStatus = "EXPIRED"
                }
            }
            userRepository.save(user)
        }

        val webhookEvent = RevenueCatWebhookEventEntity(
            eventId = eventId,
            eventType = eventType
        )
        webhookRepo.save(webhookEvent)

        return ResponseEntity.ok("Processed")
    }
}


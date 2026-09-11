package com.curiq.api.service

import com.curiq.api.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
class AiLimitService(
    private val userRepository: UserRepository
) {

    @Transactional
    fun consumeQuota(firebaseUid: String) {
        val userOpt = userRepository.findByFirebaseUidForUpdate(firebaseUid)
        if (userOpt.isEmpty) {
            throw RuntimeException("User not found")
        }
        val user = userOpt.get()

        // Check if premium
        if (user.subscriptionStatus == "ACTIVE" || user.subscriptionStatus == "CANCELLED") {
            if (user.subscriptionStatus == "CANCELLED" && user.subscriptionExpiresAt != null) {
                if (System.currentTimeMillis() <= user.subscriptionExpiresAt!!) {
                    return // Still active
                }
            } else {
                return // Active
            }
        }

        val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))

        if (user.lastAiQueryDate != today) {
            user.aiQueryCount = 0
            user.lastAiQueryDate = today
        }

        if (user.aiQueryCount >= 3) {
            throw AiLimitReachedException("You've used your 3 free Ask Curiq questions for today.", 3, user.aiQueryCount)
        }

        user.aiQueryCount += 1
        userRepository.save(user)
    }

    @Transactional
    fun refundAiQuota(firebaseUid: String) {
        val userOpt = userRepository.findByFirebaseUidForUpdate(firebaseUid)
        if (userOpt.isEmpty) return
        val user = userOpt.get()

        val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
        if (user.lastAiQueryDate == today && user.aiQueryCount > 0) {
            user.aiQueryCount -= 1
            userRepository.save(user)
        }
    }
}

class AiLimitReachedException(
    message: String,
    val limit: Int,
    val used: Int
) : RuntimeException(message)

package com.curiq.api.service

import com.curiq.api.repository.SavedItemRepository
import com.curiq.api.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserLimitService(
    private val userRepository: UserRepository,
    private val savedItemRepository: SavedItemRepository
) {
    @Transactional
    fun checkSaveLimit(firebaseUid: String) {
        val userOpt = userRepository.findByFirebaseUidForUpdate(firebaseUid)
        if (userOpt.isEmpty) {
            throw RuntimeException("User not found")
        }
        val user = userOpt.get()

        if (user.subscriptionStatus == "ACTIVE" || user.subscriptionStatus == "CANCELLED") {
            // Check expiration if CANCELLED
            if (user.subscriptionStatus == "CANCELLED" && user.subscriptionExpiresAt != null) {
                if (System.currentTimeMillis() > user.subscriptionExpiresAt!!) {
                    // Expired
                } else {
                    return // Still active
                }
            } else {
                return // Active
            }
        }

        val count = savedItemRepository.countByUserIdAndDeletedFalse(user.id!!)
        if (count >= 5) {
            throw SaveLimitReachedException("You've reached your 5 saved posts.")
        }
    }
}

class SaveLimitReachedException(message: String) : RuntimeException(message)

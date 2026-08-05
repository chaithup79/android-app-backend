package com.curiq.api.service

import com.curiq.api.model.UserContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CurrentUserService {
    fun getUserContext(): UserContext {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated) {
            throw IllegalStateException("No authenticated user found in SecurityContext")
        }
        val principal = authentication.principal
        if (principal !is UserContext) {
            throw IllegalStateException("Principal is not of type UserContext")
        }
        return principal
    }

    fun userId(): Long {
        return getUserContext().internalId
    }
}

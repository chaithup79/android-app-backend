package com.curiq.api.config

import com.curiq.api.model.UserContext
import com.curiq.api.model.UserEntity
import com.curiq.api.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class FirebaseTokenFilter(
    private val userRepository: UserRepository
) : OncePerRequestFilter() {
    
    private val logger = LoggerFactory.getLogger(FirebaseTokenFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header.isNullOrBlank() || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = header.substring(7)
        try {
            val decodedToken: FirebaseToken = FirebaseAuth.getInstance().verifyIdToken(token)
            
            // Sync user to PostgreSQL
            val userOpt = userRepository.findByFirebaseUid(decodedToken.uid)
            val user = if (userOpt.isPresent) {
                val existingUser = userOpt.get()
                existingUser.lastLoginAt = System.currentTimeMillis()
                // Update basic info if it changed
                if (decodedToken.email != null && existingUser.email != decodedToken.email) {
                    existingUser.email = decodedToken.email
                }
                if (decodedToken.name != null && existingUser.displayName != decodedToken.name) {
                    existingUser.displayName = decodedToken.name
                }
                if (decodedToken.picture != null && existingUser.photoUrl != decodedToken.picture) {
                    existingUser.photoUrl = decodedToken.picture
                }
                userRepository.save(existingUser)
            } else {
                val newUser = UserEntity(
                    firebaseUid = decodedToken.uid,
                    email = decodedToken.email,
                    displayName = decodedToken.name,
                    photoUrl = decodedToken.picture
                )
                userRepository.save(newUser)
            }

            val isAnonymous = decodedToken.claims["provider_id"] == "anonymous"

            val userContext = UserContext(
                internalId = user.id!!,
                uid = decodedToken.uid,
                email = decodedToken.email,
                displayName = decodedToken.name,
                anonymous = isAnonymous
            )

            val authentication = UsernamePasswordAuthenticationToken(
                userContext, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
            )
            SecurityContextHolder.getContext().authentication = authentication
            
        } catch (e: Exception) {
            logger.error("Firebase token verification failed", e)
            SecurityContextHolder.clearContext()
        }

        filterChain.doFilter(request, response)
    }
}

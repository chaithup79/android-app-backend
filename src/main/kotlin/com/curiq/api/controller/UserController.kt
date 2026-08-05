package com.curiq.api.controller

import com.curiq.api.model.UserEntity
import com.curiq.api.repository.UserRepository
import com.curiq.api.service.CurrentUserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userRepository: UserRepository,
    private val currentUserService: CurrentUserService
) {

    @GetMapping("/me")
    fun getMyProfile(): ResponseEntity<UserEntity> {
        val userId = currentUserService.userId()
        val userOpt = userRepository.findById(userId)
        
        return if (userOpt.isPresent) {
            ResponseEntity.ok(userOpt.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/me")
    fun updateMyProfile(@RequestBody updates: Map<String, String>): ResponseEntity<UserEntity> {
        val userId = currentUserService.userId()
        val userOpt = userRepository.findById(userId)
        
        if (userOpt.isEmpty) {
            return ResponseEntity.notFound().build()
        }
        
        val user = userOpt.get()
        
        // Allow updating specific preferences
        if (updates.containsKey("displayName")) {
            user.displayName = updates["displayName"]
        }
        
        // Later we can add Preferences JSON or devices list here
        
        val saved = userRepository.save(user)
        return ResponseEntity.ok(saved)
    }
}

package com.curiq.api.controller

import com.curiq.api.dto.ChatRequest
import com.curiq.api.dto.ChatResponse
import com.curiq.api.service.ChatService
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import com.curiq.api.model.UserEntity

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping
    fun chat(
        @RequestBody request: ChatRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<ChatResponse> {
        val response = chatService.processChat(request, user.id!!)
        return ResponseEntity.ok(response)
    }
}

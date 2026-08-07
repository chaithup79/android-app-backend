package com.curiq.api.controller

import com.curiq.api.dto.ChatRequest
import com.curiq.api.dto.ChatResponse
import com.curiq.api.service.ChatService
import com.curiq.api.service.CurrentUserService
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatService: ChatService,
    private val currentUserService: CurrentUserService
) {

    @PostMapping
    fun chat(
        @RequestBody request: ChatRequest
    ): ResponseEntity<ChatResponse> {
        val userId = currentUserService.userId()
        val response = chatService.processChat(request, userId)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/stream", produces = [org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE])
    fun chatStream(
        @RequestBody request: ChatRequest
    ): reactor.core.publisher.Flux<String> {
        val userId = currentUserService.userId()
        return chatService.processChatStream(request, userId)
    }
}

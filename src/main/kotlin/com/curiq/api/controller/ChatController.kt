package com.curiq.api.controller

import com.curiq.api.dto.ChatRequest
import com.curiq.api.dto.ChatResponse
import com.curiq.api.service.ChatService
import com.curiq.api.service.CurrentUserService
import com.curiq.api.service.AiLimitService
import com.curiq.api.service.AiLimitReachedException
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatService: ChatService,
    private val currentUserService: CurrentUserService,
    private val aiLimitService: AiLimitService
) {

    private fun consumeQuota() {
        val firebaseUid = SecurityContextHolder.getContext().authentication.principal as String
        aiLimitService.consumeQuota(firebaseUid)
    }

    private fun refundQuota() {
        val firebaseUid = SecurityContextHolder.getContext().authentication.principal as String
        aiLimitService.refundAiQuota(firebaseUid)
    }

    @PostMapping
    fun chat(
        @RequestBody request: ChatRequest
    ): ResponseEntity<ChatResponse> {
        val userId = currentUserService.userId()
        consumeQuota()
        try {
            val response = chatService.processChat(request, userId)
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            refundQuota()
            throw e
        }
    }

    @PostMapping("/stream", produces = [org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE])
    fun chatStream(
        @RequestBody request: ChatRequest
    ): reactor.core.publisher.Flux<String> {
        val userId = currentUserService.userId()
        consumeQuota()
        try {
            return chatService.processChatStream(request, userId).doOnError {
                refundQuota()
            }
        } catch (e: Exception) {
            refundQuota()
            throw e
        }
    }

    @ExceptionHandler(AiLimitReachedException::class)
    fun handleAiLimitReachedException(e: AiLimitReachedException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(403).body(mapOf(
            "code" to "AI_LIMIT_REACHED",
            "message" to e.message!!,
            "limit" to e.limit,
            "used" to e.used
        ))
    }
}

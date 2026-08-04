package com.curiq.api.dto

data class ChatRequest(
    val conversationId: String? = null,
    val question: String,
    val context: List<ChatContextItem>
)

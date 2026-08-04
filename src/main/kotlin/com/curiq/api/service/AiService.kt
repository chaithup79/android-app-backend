package com.curiq.api.service

import com.curiq.api.dto.AiProcessRequest
import com.curiq.api.dto.AiProcessResponse
import com.curiq.api.prompt.PromptBuilder
import com.curiq.api.provider.AiProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

import com.curiq.api.dto.ChatRequest
import com.curiq.api.dto.ChatResponse

@Service
class AiService(
    @Qualifier("ollamaProvider") private val aiProvider: AiProvider,
    private val promptBuilder: PromptBuilder
) {
    fun process(request: AiProcessRequest): AiProcessResponse {
        val prompt = promptBuilder.buildCategorizePrompt(request.sharedText)
        return aiProvider.process(prompt)
    }

    fun chat(request: ChatRequest): ChatResponse {
        val prompt = promptBuilder.buildChatPrompt(request)
        val answer = aiProvider.chat(prompt)
        return ChatResponse(answer = answer, bookmarkCount = request.context.size)
    }
}

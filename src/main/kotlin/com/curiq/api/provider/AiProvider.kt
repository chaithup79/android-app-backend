package com.curiq.api.provider

import com.curiq.api.dto.AiProcessResponse

interface AiProvider {
    fun process(prompt: String): AiProcessResponse
    fun chat(prompt: String): String
    fun chatStream(prompt: String): reactor.core.publisher.Flux<String>
}

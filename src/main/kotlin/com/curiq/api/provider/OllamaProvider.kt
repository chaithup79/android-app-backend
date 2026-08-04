package com.curiq.api.provider

import com.curiq.api.dto.AiProcessResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("ollamaProvider")
class OllamaProvider(
    @Value("\${ollama.base-url}") private val baseUrl: String,
    @Value("\${ollama.model:gemma3:4b}") private val modelName: String,
    private val objectMapper: ObjectMapper
) : AiProvider {

    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    override fun process(prompt: String): AiProcessResponse {
        val requestBody = mapOf(
            "model" to modelName,
            "prompt" to prompt,
            "stream" to false,
            "format" to "json"
        )

        val response = webClient.post()
            .uri("/api/generate")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()

        val jsonResponse = response?.get("response") as? String
            ?: throw RuntimeException("Empty response from Ollama")

        return try {
            val cleanedJson = extractJson(jsonResponse)
            objectMapper.readValue(cleanedJson, AiProcessResponse::class.java)
        } catch (e: Exception) {
            // Fallback if AI fails to return valid JSON
            AiProcessResponse(
                summary = "Failed to parse summary.",
                category = "Other",
                subcategory = "",
                tags = emptyList()
            )
        }
    }

    override fun chat(prompt: String): String {
        val requestBody = mapOf(
            "model" to modelName,
            "prompt" to prompt,
            "stream" to false
        )

        val response = webClient.post()
            .uri("/api/generate")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()

        return response?.get("response") as? String
            ?: throw RuntimeException("Empty response from Ollama")
    }

    private fun extractJson(text: String): String {
        val cleaned = text
            .replace("```json", "")
            .replace("```", "")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) {
            throw IllegalArgumentException("No JSON object found.")
        }
        return cleaned.substring(start, end + 1)
    }
}

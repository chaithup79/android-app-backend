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

        val response = try {
            webClient.post()
                .uri("/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map::class.java)
                .timeout(java.time.Duration.ofSeconds(60))
                .block()
        } catch (e: Exception) {
            // Re-throw to allow JobScheduler to handle RETRY
            throw RuntimeException("Ollama connection/timeout error: ${e.message}", e)
        }

        val jsonResponse = response?.get("response") as? String
            ?: throw IllegalArgumentException("Empty response from Ollama")

        return try {
            val cleanedJson = extractJson(jsonResponse)
            objectMapper.readValue(cleanedJson, AiProcessResponse::class.java)
        } catch (e: Exception) {
            throw IllegalArgumentException("Unrecoverable error: Failed to parse Ollama JSON response", e)
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

    override fun chatStream(prompt: String): reactor.core.publisher.Flux<String> {
        val requestBody = mapOf(
            "model" to modelName,
            "prompt" to prompt,
            "stream" to true
        )

        return webClient.post()
            .uri("/api/generate")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(Map::class.java)
            .map { responseMap ->
                val chunk = responseMap["response"] as? String ?: ""
                val done = responseMap["done"] as? Boolean ?: false
                
                val map = mapOf("token" to chunk, "done" to done)
                objectMapper.writeValueAsString(map)
            }
            .onErrorResume { e ->
                val errorMap = mapOf("token" to "[ERROR] ${e.message}", "done" to true)
                reactor.core.publisher.Flux.just(objectMapper.writeValueAsString(errorMap))
            }
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

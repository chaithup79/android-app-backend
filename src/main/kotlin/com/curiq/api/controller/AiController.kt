package com.curiq.api.controller

import com.curiq.api.dto.AiProcessRequest
import com.curiq.api.dto.AiProcessResponse
import com.curiq.api.model.AiJob
import com.curiq.api.model.JobStatus
import com.curiq.api.model.SavedItem
import com.curiq.api.queue.RedisStreamPublisher
import com.curiq.api.repository.AiJobRepository
import com.curiq.api.repository.SavedItemRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

import com.curiq.api.dto.ChatRequest
import com.curiq.api.dto.ChatResponse
import com.curiq.api.service.AiService

@RestController
@RequestMapping("/api/v1")
class AiController(
    private val savedItemRepository: SavedItemRepository,
    private val aiJobRepository: AiJobRepository,
    private val redisStreamPublisher: RedisStreamPublisher,
    private val aiService: AiService
) {

    @PostMapping("/chat")
    fun chat(@RequestBody request: ChatRequest): ChatResponse {
        return aiService.chat(request)
    }

    @PostMapping("/ai/jobs")
    fun submitJob(@RequestBody request: AiProcessRequest): JobResponse {
        val correlationId = request.correlationId ?: UUID.randomUUID().toString()

        // 1. Create SavedItem stub
        val savedItem = SavedItem(
            correlationId = correlationId,
            originalText = request.sharedText,
            category = request.forcedCategory
        )
        val persistedItem = savedItemRepository.save(savedItem)

        // 2. Create AiJob
        val aiJob = AiJob(
            savedItemId = persistedItem.id,
            correlationId = correlationId
        )
        val persistedJob = aiJobRepository.save(aiJob)

        // 3. Publish to Redis Stream
        redisStreamPublisher.publishJob(persistedJob.id.toString())

        return JobResponse(
            jobId = correlationId,
            status = persistedJob.status.name,
            estimatedWaitSeconds = 5 // Simple estimate for now
        )
    }

    @Transactional(readOnly = true)
    @GetMapping("/ai/jobs/{correlationId}")
    fun getJobStatus(@PathVariable correlationId: String): ResponseEntity<JobResultResponse> {
        val jobOpt = aiJobRepository.findByCorrelationId(correlationId)
        if (jobOpt.isEmpty) return ResponseEntity.notFound().build()
        val job = jobOpt.get()

        val itemOpt = savedItemRepository.findByCorrelationId(correlationId)
        if (itemOpt.isEmpty) return ResponseEntity.notFound().build()
        val item = itemOpt.get()

        val result = if (job.status == JobStatus.COMPLETED) {
            AiProcessResponse(
                summary = item.summary ?: "",
                category = item.category ?: "Other",
                subcategory = item.subcategory ?: "",
                tags = item.tags
            )
        } else null

        return ResponseEntity.ok(
            JobResultResponse(
                jobId = correlationId,
                status = job.status.name,
                result = result,
                error = job.error
            )
        )
    }
}

data class JobResponse(
    val jobId: String,
    val status: String,
    val estimatedWaitSeconds: Int
)

data class JobResultResponse(
    val jobId: String,
    val status: String,
    val result: AiProcessResponse?,
    val error: String?
)

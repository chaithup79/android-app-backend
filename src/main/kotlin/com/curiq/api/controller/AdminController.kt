package com.curiq.api.controller

import com.curiq.api.model.LegacyJobStatus
import com.curiq.api.repository.AiJobRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val aiJobRepository: AiJobRepository
) {

    @GetMapping("/jobs")
    fun getMetrics(): Map<String, Any> {
        val allJobs = aiJobRepository.findAll()
        val todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS)

        val queued = allJobs.count { it.status == LegacyJobStatus.QUEUED }
        val processing = allJobs.count { it.status == LegacyJobStatus.PROCESSING }
        val failed = allJobs.count { it.status == LegacyJobStatus.FAILED }
        
        val completedToday = allJobs.count { 
            it.status == LegacyJobStatus.COMPLETED && 
            it.completedAt != null && 
            it.completedAt!!.isAfter(todayStart) 
        }

        val completedJobs = allJobs.filter { it.status == LegacyJobStatus.COMPLETED && it.processingTimeMs != null }
        val avgProcessingTimeMs = if (completedJobs.isNotEmpty()) {
            completedJobs.map { it.processingTimeMs!! }.average().toLong()
        } else 0L

        return mapOf(
            "queued" to queued,
            "processing" to processing,
            "failed" to failed,
            "completedToday" to completedToday,
            "averageProcessingTimeMs" to avgProcessingTimeMs
        )
    }
}

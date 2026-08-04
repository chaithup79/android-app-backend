package com.curiq.api.service

import com.curiq.api.dto.AiProcessResponse
import com.curiq.api.model.AiJob
import com.curiq.api.model.JobStatus
import com.curiq.api.model.ProcessingStatus
import com.curiq.api.model.SavedItem
import com.curiq.api.repository.AiJobRepository
import com.curiq.api.repository.SavedItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ResultPersister(
    private val savedItemRepository: SavedItemRepository,
    private val aiJobRepository: AiJobRepository
) {
    @Transactional
    fun persistResult(job: AiJob, item: SavedItem, result: AiProcessResponse, metrics: Map<String, Long>) {
        // Update SavedItem. Use AI category only if a manual category wasn't forced.
        item.summary = result.summary
        item.category = item.category ?: result.category
        item.subcategory = result.subcategory
        item.tags = result.tags.toMutableList()
        item.processingStatus = ProcessingStatus.COMPLETED
        savedItemRepository.save(item)

        // Update Job
        job.status = JobStatus.COMPLETED
        job.completedAt = Instant.now()
        job.queueWaitMs = metrics["queue_wait_ms"]
        job.metadataTimeMs = metrics["metadata_time_ms"]
        job.aiTimeMs = metrics["ai_time_ms"]
        job.processingTimeMs = metrics["processing_time_ms"]
        aiJobRepository.save(job)
    }

    @Transactional
    fun persistError(job: AiJob, item: SavedItem, errorMessage: String, metrics: Map<String, Long>) {
        item.processingStatus = ProcessingStatus.FAILED
        savedItemRepository.save(item)

        job.status = JobStatus.FAILED
        job.error = errorMessage
        job.completedAt = Instant.now()
        job.processingTimeMs = metrics["processing_time_ms"]
        aiJobRepository.save(job)
    }
}

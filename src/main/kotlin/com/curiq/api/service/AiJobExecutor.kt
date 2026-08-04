package com.curiq.api.service

import com.curiq.api.prompt.PromptBuilder
import com.curiq.api.provider.AiProvider
import com.curiq.api.repository.AiJobRepository
import com.curiq.api.repository.SavedItemRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.Duration

@Service
class AiJobExecutor(
    private val aiJobRepository: AiJobRepository,
    private val savedItemRepository: SavedItemRepository,
    private val metadataService: MetadataService,
    private val promptBuilder: PromptBuilder,
    @Qualifier("ollamaProvider") private val aiProvider: AiProvider,
    private val resultPersister: ResultPersister
) {
    private val logger = LoggerFactory.getLogger(AiJobExecutor::class.java)

    fun executeJob(jobId: Long) {
        val totalStart = Instant.now()
        
        val jobOptional = aiJobRepository.findById(jobId)
        if (jobOptional.isEmpty) {
            logger.error("Job $jobId not found")
            return
        }
        val job = jobOptional.get()
        
        job.startedAt = Instant.now()
        val queueWaitMs = Duration.between(job.createdAt, job.startedAt).toMillis()

        val itemOptional = savedItemRepository.findById(job.savedItemId)
        if (itemOptional.isEmpty) {
            logger.error("SavedItem ${job.savedItemId} not found")
            job.status = com.curiq.api.model.JobStatus.FAILED
            job.error = "SavedItem not found"
            job.completedAt = Instant.now()
            aiJobRepository.save(job)
            return
        }
        val item = itemOptional.get()

        try {
            // 1. Metadata
            val metadataStart = Instant.now()
            val contextText = metadataService.extractMetadata(item.originalText)
            val metadataTimeMs = Duration.between(metadataStart, Instant.now()).toMillis()

            // 2. Prompt
            val prompt = promptBuilder.buildCategorizePrompt(contextText)

            // 3. AI Provider
            val aiStart = Instant.now()
            val result = aiProvider.process(prompt)
            val aiTimeMs = Duration.between(aiStart, Instant.now()).toMillis()

            // 4. Persist
            val totalTimeMs = Duration.between(totalStart, Instant.now()).toMillis()
            val metrics = mapOf(
                "queue_wait_ms" to queueWaitMs,
                "metadata_time_ms" to metadataTimeMs,
                "ai_time_ms" to aiTimeMs,
                "processing_time_ms" to totalTimeMs
            )
            resultPersister.persistResult(job, item, result, metrics)
            logger.info("Successfully processed job $jobId in ${totalTimeMs}ms")
            
        } catch (e: Exception) {
            logger.error("Failed to process job $jobId", e)
            val totalTimeMs = Duration.between(totalStart, Instant.now()).toMillis()
            resultPersister.persistError(job, item, e.message ?: "Unknown error", mapOf("processing_time_ms" to totalTimeMs))
        }
    }
}

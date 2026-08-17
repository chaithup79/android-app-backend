package com.curiq.api.service

import com.curiq.api.model.Job
import com.curiq.api.model.JobType
import com.curiq.api.repository.SavedItemRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MetadataJobWorker(
    private val orchestrator: MetadataOrchestrator,
    private val repository: SavedItemRepository,
    private val jobService: JobService
) : JobWorker {

    private val logger = LoggerFactory.getLogger(MetadataJobWorker::class.java)

    override fun canHandle(job: Job): Boolean {
        return job.type == JobType.METADATA
    }

    override fun process(job: Job) {
        val bookmarkId = job.payload.toLong()
        val itemOpt = repository.findById(bookmarkId)
        
        if (itemOpt.isEmpty) {
            logger.warn("MetadataJob failed: Bookmark $bookmarkId not found")
            return
        }
        
        val item = itemOpt.get()
        logger.info("Processing metadata for bookmark: ${item.url}")
        
        val enrichedItem = orchestrator.processMetadata(item)
        
        logger.info("""
            --- Final Extracted Metadata for ${enrichedItem.url} ---
            Title: ${enrichedItem.title}
            Author: ${enrichedItem.author}
            Domain: ${enrichedItem.sourceDomain}
            Summary: ${enrichedItem.summary?.take(50)?.plus("...")}
            ------------------------------------------------
        """.trimIndent())
        
        var shouldEnqueueAi = false
        if (enrichedItem.summary != null || enrichedItem.imageUrl != null || enrichedItem.category != null || enrichedItem.faviconUrl != null || enrichedItem.sourceDomain != null) {
            enrichedItem.metadataStatus = "READY"
            shouldEnqueueAi = true
        } else {
            enrichedItem.metadataStatus = "FAILED_METADATA"
        }
        
        enrichedItem.updatedAt = System.currentTimeMillis()
        val savedItem = repository.save(enrichedItem)
        logger.info("MetadataJob completed for bookmark: ${item.url}")

        if (shouldEnqueueAi && savedItem.aiStatus == "PENDING") {
            logger.info("Metadata extraction successful. Enqueuing AI Job for bookmark: ${savedItem.id}")
            jobService.enqueue(JobType.AI, savedItem.id.toString())
        } else {
            savedItem.status = "COMPLETED"
            repository.save(savedItem)
        }
    }
}

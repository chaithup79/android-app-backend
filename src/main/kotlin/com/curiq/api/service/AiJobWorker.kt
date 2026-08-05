package com.curiq.api.service

import com.curiq.api.model.Job
import com.curiq.api.model.JobType
import com.curiq.api.provider.AiProvider
import com.curiq.api.repository.SavedItemRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AiJobWorker(
    private val repository: SavedItemRepository,
    private val aiProvider: AiProvider,
    private val objectMapper: ObjectMapper
) : JobWorker {
    private val logger = LoggerFactory.getLogger(AiJobWorker::class.java)

    override fun canHandle(job: Job): Boolean {
        return job.type == JobType.AI
    }

    override fun process(job: Job) {
        val bookmarkId = job.payload.toLong()
        val itemOpt = repository.findById(bookmarkId)
        
        if (itemOpt.isEmpty) {
            logger.warn("AiJob failed: Bookmark \$bookmarkId not found")
            return
        }
        
        val item = itemOpt.get()
        logger.info("Processing AI for bookmark: ${item.url}")
        
        // Construct Maximum Context Prompt
        val prompt = """
            Analyze the following saved bookmark.
            
            URL: ${item.url}
            Source Domain: ${item.sourceDomain ?: "Unknown"}
            Title: ${item.title ?: "Unknown"}
            Scraped Summary: ${item.summary ?: "Unknown"}
            Category: ${item.category ?: "Unknown"}
            
            Based on this information, provide a JSON response with the following exact fields:
            - "ai_summary": A high-quality, concise 2-3 sentence summary.
            - "ai_category": A broad category (e.g., Programming, Finance, Entertainment).
            - "ai_confidence": A number from 0-100 indicating how confident you are in this analysis.
            - "tags": A JSON array of 3-5 specific keyword tags.
            - "readingTime": Estimated reading time in minutes (number).
            - "difficulty": "Beginner", "Intermediate", or "Advanced".
            
            Return ONLY valid JSON.
        """.trimIndent()

        try {
            val aiData = aiProvider.process(prompt)

            item.aiSummary = aiData.ai_summary
            item.aiCategory = aiData.ai_category
            item.aiConfidence = aiData.ai_confidence
            
            val aiMetadataMap = mutableMapOf<String, Any>()
            aiData.tags?.let { aiMetadataMap["tags"] = it }
            aiData.readingTime?.let { aiMetadataMap["readingTime"] = it }
            aiData.difficulty?.let { aiMetadataMap["difficulty"] = it }
            
            item.aiMetadata = aiMetadataMap
            item.aiStatus = "READY"
            item.status = "COMPLETED"
            item.updatedAt = System.currentTimeMillis()
            
            repository.save(item)
            logger.info("AiJob completed for bookmark: ${item.url}")
            
        } catch (e: Exception) {
            logger.error("AiJob failed for bookmark: ${item.url}", e)
            item.aiStatus = "FAILED"
            item.status = "COMPLETED"
            item.updatedAt = System.currentTimeMillis()
            repository.save(item)
            throw e // Let the Job framework handle retries
        }
    }
}

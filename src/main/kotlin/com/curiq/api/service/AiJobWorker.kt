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
        
        val existingCategories = item.userId?.let { repository.findDistinctCategories(it) } ?: emptyList()
        val categoriesStr = if (existingCategories.isNotEmpty()) existingCategories.joinToString(", ") else "None yet"
        
        // Construct Maximum Context Prompt
        val prompt = """
            You are Curiq, an AI assistant that organizes saved content into a personal knowledge library.

            Analyze the information below and understand what the content is mainly about.

            URL:
            ${item.url}

            Source:
            ${item.sourceDomain ?: "Unknown"}

            Title:
            ${item.title ?: "Unknown"}

            Summary:
            ${item.summary ?: "Unknown"}

            Current Category:
            ${item.category ?: "Unknown"}

            Existing Collections:
            $categoriesStr

            Return ONLY valid JSON in this format:

            {
              "ai_summary": "",
              "ai_category": "",
              "ai_confidence": 0,
              "tags": [],
              "readingTime": 0,
              "difficulty": ""
            }

            Rules

            Summary
            - Write a clear summary in 2–3 sentences.
            - Explain the main idea in simple language.
            - Don't mention Instagram, YouTube, LinkedIn, Reddit or any platform unless it is directly relevant.
            - Don't start with phrases like "This post", "This video", or "This article".
            - If the provided summary is useful, improve it instead of copying it.
            - If there isn't enough information, use the title and source to create the best possible short summary without inventing facts.

            Category
            - Create ONE short, meaningful collection name that best describes the main topic.
            - Prefer 1–3 words (maximum 4 words).
            - Think like a person organizing their personal library, not a technical taxonomy.
            - Choose a collection name that someone would naturally create for themselves.
            
            Good examples:
            - Healthy Recipes
            - Budget Travel
            - Home Decor
            - Career Advice
            - Interview Tips
            - Personal Finance
            - Investment Ideas
            - Workout Plans
            - Parenting Tips
            - Study Notes
            - Book Recommendations
            - Gardening
            - Photography Tips
            - Cooking Basics
            - Productivity
            - Mental Wellness
            - Relationship Advice
            - Fashion Ideas
            - Business Ideas
            - Marketing
            - Artificial Intelligence
            - Android Development
            - System Design
            - Machine Learning
            - Sports
            
            Bad examples:
            - Other
            - General
            - Miscellaneous
            - Random
            - Entertainment
            - Leisure and Recreation
            - Technology
            - Programming
            - Lifestyle
            - Education
            
            - EXTREMELY IMPORTANT: You MUST reuse one of the user's Existing Collections if the content fits into it, even broadly (e.g. put a travel photo into "Travel", do NOT create "Travel Photography").
            - Only create a new collection if the content is completely unrelated to ALL Existing Collections.
            - Never force unrelated content into an existing collection.
            - Avoid categories that are too broad or too narrow.
            - Return only the collection name.

            Tags
            - Return 3 to 5 useful tags.
            - Lowercase only.
            - No hashtags.
            - No duplicates.

            Confidence
            - 90-100: Very confident
            - 70-89: Good confidence
            - Below 70: Limited information

            Reading Time
            - Estimate how many minutes it would take to read the content.
            - If it is a video or unknown, estimate the time to understand the summary.

            Difficulty
            Choose one:
            - Beginner
            - Intermediate
            - Advanced

            Return ONLY JSON.
        """.trimIndent()

        var currentItem = item
        currentItem.aiStatus = "PROCESSING"
        currentItem.lastAiAttemptAt = System.currentTimeMillis()
        currentItem = repository.save(currentItem)

        try {
            val aiData = aiProvider.process(prompt)

            // Re-fetch to ensure fresh @Version in case client synced while AI was processing
            currentItem = repository.findById(bookmarkId).orElse(currentItem)

            currentItem.aiSummary = aiData.ai_summary
            val categoryResult = if (aiData.ai_category.isNullOrBlank() || aiData.ai_category.equals("Unknown", ignoreCase = true)) "Other" else aiData.ai_category
            currentItem.aiCategory = categoryResult
            currentItem.aiConfidence = aiData.ai_confidence
            
            if (currentItem.category.isNullOrBlank() || currentItem.category.equals("Unknown", ignoreCase = true)) {
                currentItem.category = categoryResult
            }
            
            val aiMetadataMap = mutableMapOf<String, Any>()
            aiData.tags?.let { aiMetadataMap["tags"] = it }
            aiData.readingTime?.let { aiMetadataMap["readingTime"] = it }
            aiData.difficulty?.let { aiMetadataMap["difficulty"] = it }
            
            currentItem.aiMetadata = aiMetadataMap
            currentItem.aiStatus = "READY"
            currentItem.aiModel = "ollama"
            currentItem.aiGeneratedAt = System.currentTimeMillis()
            currentItem.status = "COMPLETED"
            currentItem.updatedAt = System.currentTimeMillis()
            currentItem.lastAiError = null
            
            repository.save(currentItem)
            logger.info("AiJob completed for bookmark: ${currentItem.url}")
            
        } catch (e: IllegalArgumentException) {
            logger.error("[AI] Unrecoverable AiJob failure for bookmark: ${currentItem.url}", e)
            currentItem = repository.findById(bookmarkId).orElse(currentItem)
            currentItem.aiStatus = "FAILED"
            currentItem.lastAiError = e.message
            currentItem.status = "COMPLETED"
            currentItem.updatedAt = System.currentTimeMillis()
            repository.save(currentItem)
            // Do not re-throw, so JobScheduler marks this job as COMPLETED, not RETRYING
        } catch (e: Exception) {
            logger.error("[AI] Recoverable AiJob failure for bookmark: ${currentItem.url}", e)
            currentItem = repository.findById(bookmarkId).orElse(currentItem)
            currentItem.aiStatus = "PENDING"
            currentItem.lastAiError = e.message
            currentItem.updatedAt = System.currentTimeMillis()
            repository.save(currentItem)
            throw e // Let the Job framework handle retries
        }
    }
}

package com.curiq.api.service

import com.curiq.api.dto.ChatContextItem
import com.curiq.api.dto.ChatRequest
import com.curiq.api.dto.ChatResponse
import com.curiq.api.provider.AiProvider
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class ChatService(
    private val aiProvider: AiProvider,
    private val savedItemRepository: com.curiq.api.repository.SavedItemRepository
) {
    private val logger = LoggerFactory.getLogger(ChatService::class.java)

    fun processChat(request: ChatRequest, userId: Long): ChatResponse {
        logger.info("Processing chat request for user $userId with question: ${request.question}")
        
        // Stage 1: PostgreSQL Full-Text Search
        val dbResults = savedItemRepository.searchByQuestionKeywords(userId, request.question)
        
        val uniqueContextItems = mutableMapOf<Long, ChatContextItem>()
        
        dbResults.forEach { item ->
            uniqueContextItems[item.id!!] = ChatContextItem(
                id = item.id!!,
                title = item.title ?: item.sourceDomain,
                summary = item.aiSummary ?: item.summary ?: "",
                category = item.aiCategory ?: item.category,
                tags = emptyList(), // Not passing tags for now
                url = item.url
            )
        }
        
        // Stage 2: Merge Android Context (if DB results are insufficient or empty)
        if (uniqueContextItems.size < 3) {
            logger.info("FTS results insufficient, merging Android context")
            request.context.forEach { item ->
                if (!uniqueContextItems.containsKey(item.id)) {
                    uniqueContextItems[item.id] = item
                }
            }
        }
        
        // Stage 3: Fallback to Recent + High Confidence if still empty
        if (uniqueContextItems.isEmpty()) {
            logger.info("Context still empty, fetching recent and high confidence bookmarks")
            val recent = savedItemRepository.findTop5ByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)
            val confident = savedItemRepository.findTop5ByUserIdAndDeletedFalseOrderByAiConfidenceDesc(userId)
            
            (recent + confident).distinctBy { it.id }.forEach { item ->
                uniqueContextItems[item.id!!] = ChatContextItem(
                    id = item.id!!,
                    title = item.title ?: item.sourceDomain,
                    summary = item.aiSummary ?: item.summary ?: "",
                    category = item.aiCategory ?: item.category,
                    tags = emptyList(),
                    url = item.url
                )
            }
        }
        
        // Construct the prompt with the provided context
        val contextBuilder = StringBuilder()
        contextBuilder.append("User Context Bookmarks:\n")
        
        // Limit to 20 bookmarks to avoid context window overflow
        val limitedContext = uniqueContextItems.values.take(20)
        
        if (limitedContext.isEmpty()) {
            contextBuilder.append("(No bookmarks found)\n")
        } else {
            limitedContext.forEach { item ->
                contextBuilder.append("- Title: ${item.title ?: "Unknown"}\n")
                if (item.summary.isNotBlank()) contextBuilder.append("  Summary: ${item.summary}\n")
                if (!item.category.isNullOrBlank()) contextBuilder.append("  Category: ${item.category}\n")
                contextBuilder.append("\n")
            }
        }
        
        val prompt = """
            You are Curiq, an intelligent, helpful, and friendly AI assistant.
            You are integrated into an app where users save bookmarks (articles, links, social media posts).
            Below is a subset of the user's saved bookmarks to give you context. 
            Use this context to accurately answer the user's question. If the answer is not in the context, you can use your general knowledge, but prioritize their saved bookmarks if relevant.
            
            $contextBuilder
            
            User Question: ${request.question}
        """.trimIndent()
        
        val answer = aiProvider.chat(prompt)
        
        return ChatResponse(
            answer = answer,
            bookmarkCount = limitedContext.size
        )
    }
}

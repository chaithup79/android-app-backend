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
    private val savedItemRepository: com.curiq.api.repository.SavedItemRepository,
    @org.springframework.beans.factory.annotation.Value("\${curiq.ai.max-context-bookmarks:7}") private val maxContextBookmarks: Int
) {
    private val logger = LoggerFactory.getLogger(ChatService::class.java)

    fun processChat(request: ChatRequest, userId: Long): ChatResponse {
        val (prompt, count) = buildPrompt(request, userId)
        val answer = aiProvider.chat(prompt)
        return ChatResponse(answer = answer, bookmarkCount = count)
    }

    fun processChatStream(request: ChatRequest, userId: Long): reactor.core.publisher.Flux<String> {
        val (prompt, _) = buildPrompt(request, userId)
        return aiProvider.chatStream(prompt)
    }

    private fun buildPrompt(request: ChatRequest, userId: Long): Pair<String, Int> {
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
        
        // Limit bookmarks to avoid context window overflow
        val limitedContext = uniqueContextItems.values.take(maxContextBookmarks)
        
        if (limitedContext.isEmpty()) {
            contextBuilder.append("(No bookmarks found)\n")
        } else {
            limitedContext.forEach { item ->
                contextBuilder.append("- Title: ${item.title ?: "Unknown"}\n")
                if (item.summary.isNotBlank()) contextBuilder.append("  Summary: ${item.summary}\n")
                if (!item.category.isNullOrBlank()) contextBuilder.append("  Category: ${item.category}\n")
                if (!item.url.isNullOrBlank()) contextBuilder.append("  URL: ${item.url}\n")
                contextBuilder.append("\n")
            }
        }
        
        val prompt = """
            You are Curiq.

            You are the user's personal knowledge companion.

            The user has saved articles, videos, repositories, posts and webpages because they found them useful. Your job is to help them rediscover, connect and understand their own knowledge.

            Below are the bookmarks that are most relevant to the user's question.

            -------------------------
            USER'S SAVED BOOKMARKS
            -------------------------
            $contextBuilder
            -------------------------
            USER QUESTION
            -------------------------
            ${request.question}

            Instructions:

            1. Always look at the user's saved bookmarks first.
            2. If one or more bookmarks answer the question, use them as the primary source.
            3. If several bookmarks are related, combine their information into one clear answer.
            4. Explain things in simple, natural language.
            5. Never mention internal fields like "Summary", "Category" or "Collection".
            6. When referring to a bookmark, naturally include its link in Markdown format:
               [Bookmark Title](URL)
            7. If the bookmarks only partially answer the question, say what they cover and then briefly fill the remaining gap using general knowledge.
            8. If none of the bookmarks are relevant, clearly say:
               "I couldn't find anything relevant in your saved knowledge."
               Then answer using your general knowledge.
            9. Never invent bookmarks or URLs.
            10. Be concise. Most answers should be under 300 words.

            Your goal is to make the user feel like you're helping them remember what they've already saved, not acting like a generic chatbot.
        """.trimIndent()
        
        return Pair(prompt, limitedContext.size)
    }
}

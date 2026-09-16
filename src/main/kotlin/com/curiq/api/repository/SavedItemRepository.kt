package com.curiq.api.repository

import com.curiq.api.model.SavedItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SavedItemRepository : JpaRepository<SavedItem, Long> {
    fun findByUuid(uuid: String): Optional<SavedItem>
    fun findByUserId(userId: Long): List<SavedItem>
    fun findByUserIdAndUpdatedAtGreaterThan(userId: Long, updatedAt: Long): List<SavedItem>
    fun findByUuidAndUserId(uuid: String, userId: Long): Optional<SavedItem>
    fun findByUrlHashAndUserId(urlHash: String, userId: Long): Optional<SavedItem>

    fun countByUserIdAndDeletedFalse(userId: Long): Long

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s.category FROM SavedItem s WHERE s.userId = :userId AND s.deleted = false AND s.category IS NOT NULL AND s.category != ''")
    fun findDistinctCategories(userId: Long): List<String>

    @org.springframework.data.jpa.repository.Query(
        value = """
            SELECT * FROM saved_items 
            WHERE user_id = :userId 
            AND deleted = false
            AND search_vector @@ websearch_to_tsquery('english', :question)
            ORDER BY ts_rank(search_vector, websearch_to_tsquery('english', :question)) DESC
            LIMIT 10
        """, nativeQuery = true
    )
    fun searchByQuestionKeywords(userId: Long, question: String): List<SavedItem>

    fun findTop5ByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId: Long): List<SavedItem>
    fun findTop5ByUserIdAndDeletedFalseOrderByAiConfidenceDesc(userId: Long): List<SavedItem>
}

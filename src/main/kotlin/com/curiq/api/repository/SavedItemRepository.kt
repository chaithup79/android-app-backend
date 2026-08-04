package com.curiq.api.repository

import com.curiq.api.model.SavedItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SavedItemRepository : JpaRepository<SavedItem, Long> {
    fun findByCorrelationId(correlationId: String): Optional<SavedItem>
}

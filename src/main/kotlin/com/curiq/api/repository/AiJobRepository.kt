package com.curiq.api.repository

import com.curiq.api.model.AiJob
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AiJobRepository : JpaRepository<AiJob, Long> {
    fun findByCorrelationId(correlationId: String): Optional<AiJob>
}

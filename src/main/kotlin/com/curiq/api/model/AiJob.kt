package com.curiq.api.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "ai_jobs")
data class AiJob(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "saved_item_id", nullable = false)
    val savedItemId: Long = 0,

    @Column(nullable = false, unique = true)
    val correlationId: String = "",

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.QUEUED,

    var retryCount: Int = 0,
    
    var priority: Int = 0,

    var createdAt: Instant = Instant.now(),
    var startedAt: Instant? = null,
    var completedAt: Instant? = null,

    var processingTimeMs: Long? = null,
    var queueWaitMs: Long? = null,
    var metadataTimeMs: Long? = null,
    var aiTimeMs: Long? = null,

    var provider: String = "LOCAL",
    var model: String = "gemma3:4b",

    @Column(columnDefinition = "TEXT")
    var error: String? = null
)

enum class JobStatus {
    QUEUED, PROCESSING, COMPLETED, FAILED
}

package com.curiq.api.model

import jakarta.persistence.*

@Entity
@Table(name = "jobs")
data class Job(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Enumerated(EnumType.STRING)
    val type: JobType = JobType.METADATA,
    
    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.PENDING,
    
    var priority: Int = 0,
    
    var retryCount: Int = 0,
    
    val payload: String = "", // E.g., bookmark ID
    
    val createdAt: Long = System.currentTimeMillis(),
    
    var updatedAt: Long = System.currentTimeMillis(),
    
    @Column(name = "next_retry_at")
    var nextRetryAt: Long = 0
)

enum class JobType {
    METADATA,
    AI,
    IMAGE
}

enum class JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

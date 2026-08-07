package com.curiq.api.repository

import com.curiq.api.model.Job
import com.curiq.api.model.JobStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface JobRepository : JpaRepository<Job, Long> {
    
    // Custom query to poll jobs (we'll keep it simple for now, relying on basic SELECT ... LIMIT)
    // Spring Data JPA doesn't have a clean LIMIT in @Query without Pageable, so we'll use a derived query or native query.
    
    @Query(value = "SELECT * FROM jobs WHERE status = 'PENDING' AND next_retry_at <= :now ORDER BY priority DESC, created_at ASC LIMIT :limit", nativeQuery = true)
    fun findNextJobsToProcess(limit: Int, now: Long = System.currentTimeMillis()): List<Job>

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Job j SET j.status = com.curiq.api.model.JobStatus.PROCESSING WHERE j.id = :id AND j.status = com.curiq.api.model.JobStatus.PENDING AND j.nextRetryAt <= :now")
    fun claimJob(id: Long, now: Long = System.currentTimeMillis()): Int
}

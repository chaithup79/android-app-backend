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
    
    @Query(value = "SELECT * FROM jobs WHERE status = 'PENDING' OR status = 'RETRYING' ORDER BY priority DESC, created_at ASC LIMIT :limit", nativeQuery = true)
    fun findNextJobsToProcess(limit: Int): List<Job>

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Job j SET j.status = 'PROCESSING' WHERE j.id = :id AND (j.status = 'PENDING' OR j.status = 'RETRYING')")
    fun claimJob(id: Long): Int
}

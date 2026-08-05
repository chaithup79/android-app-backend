package com.curiq.api.service

import com.curiq.api.model.Job
import com.curiq.api.model.JobType
import com.curiq.api.repository.JobRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class JobService(
    private val jobRepository: JobRepository
) {
    @Transactional
    fun enqueue(type: JobType, payload: String, priority: Int = 0) {
        val job = Job(
            type = type,
            payload = payload,
            priority = priority
        )
        jobRepository.save(job)
    }
}

package com.curiq.api.service

import com.curiq.api.model.JobStatus
import com.curiq.api.repository.JobRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class JobScheduler(
    private val jobRepository: JobRepository,
    private val workers: List<JobWorker>
) {
    private val logger = LoggerFactory.getLogger(JobScheduler::class.java)

    @Scheduled(fixedDelay = 5000)
    fun pollJobs() {
        val jobs = jobRepository.findNextJobsToProcess(10)
        
        for (job in jobs) {
            val updated = jobRepository.claimJob(job.id)
            if (updated == 0) {
                // Another node claimed this job before we could!
                continue
            }
            
            logger.info("Processing job: ${job.id} of type ${job.type}")
            job.status = JobStatus.PROCESSING
            job.updatedAt = System.currentTimeMillis()
            
            try {
                val worker = workers.firstOrNull { it.canHandle(job) }
                if (worker != null) {
                    worker.process(job)
                    job.status = JobStatus.COMPLETED
                    logger.info("Successfully completed job: ${job.id}")
                } else {
                    logger.warn("No worker found for job: ${job.id}")
                    job.status = JobStatus.FAILED
                }
            } catch (e: Exception) {
                logger.error("Error processing job: ${job.id}", e)
                job.retryCount++
                if (job.retryCount >= 3) {
                    job.status = JobStatus.FAILED
                } else {
                    job.status = JobStatus.RETRYING
                }
            } finally {
                job.updatedAt = System.currentTimeMillis()
                jobRepository.save(job)
            }
        }
    }
}

package com.curiq.api.queue

import com.curiq.api.service.AiJobExecutor
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.stream.StreamListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("worker")
@Component
class RedisListener(private val aiJobExecutor: AiJobExecutor) : StreamListener<String, MapRecord<String, String, String>> {
    
    private val logger = LoggerFactory.getLogger(RedisListener::class.java)

    override fun onMessage(message: MapRecord<String, String, String>) {
        val jobIdStr = message.value["jobId"]
        if (jobIdStr != null) {
            try {
                val jobId = jobIdStr.toLong()
                logger.info("Received job $jobId from Redis Stream")
                aiJobExecutor.executeJob(jobId)
            } catch (e: Exception) {
                logger.error("Failed to process job $jobIdStr", e)
            }
        }
    }
}

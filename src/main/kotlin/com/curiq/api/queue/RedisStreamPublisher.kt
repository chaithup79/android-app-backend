package com.curiq.api.queue

import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisStreamPublisher(private val redisTemplate: StringRedisTemplate) {

    private val streamKey = "curiq.ai.jobs"

    fun publishJob(jobId: String) {
        val record: MapRecord<String, String, String> = StreamRecords.newRecord()
            .ofMap(mapOf("jobId" to jobId))
            .withStreamKey(streamKey)

        redisTemplate.opsForStream<String, String>().add(record)
    }
}

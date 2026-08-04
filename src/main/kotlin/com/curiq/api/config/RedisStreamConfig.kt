package com.curiq.api.config

import com.curiq.api.queue.RedisListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions
import java.time.Duration

@Profile("worker")
@Configuration
class RedisStreamConfig(
    private val redisListener: RedisListener,
    private val redisConnectionFactory: RedisConnectionFactory,
    private val stringRedisTemplate: StringRedisTemplate
) {
    val streamKey = "curiq.ai.jobs"
    val consumerGroup = "curiq-ai-workers"
    val consumerName = "worker-1"

    @Bean
    fun streamMessageListenerContainer(): StreamMessageListenerContainer<String, MapRecord<String, String, String>> {
        createConsumerGroupIfNotExists()
        
        val options = StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ofSeconds(1))
            .errorHandler { error ->
                println("Redis Stream Error: \${error.message}")
                if (error.message?.contains("NOGROUP") == true || error.cause?.message?.contains("NOGROUP") == true) {
                    println("Auto-healing: Recreating Redis stream and group...")
                    createConsumerGroupIfNotExists()
                }
            }
            .build()
            
        val container = StreamMessageListenerContainer.create(redisConnectionFactory, options)
        
        container.receiveAutoAck(
            Consumer.from(consumerGroup, consumerName),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            redisListener
        )
        
        container.start()
        return container
    }
    
    private fun createConsumerGroupIfNotExists() {
        try {
            // The safest way to ensure the stream exists in Spring Data Redis 
            // before creating a group is to unconditionally add a dummy record.
            stringRedisTemplate.opsForStream<String, String>().add(streamKey, mapOf("init" to "init"))
        } catch (e: Exception) {
            // Ignore
        }

        try {
            stringRedisTemplate.opsForStream<String, String>().createGroup(streamKey, ReadOffset.from("0-0"), consumerGroup)
        } catch (e: Exception) {
            // Group already exists or other error
            println("Redis Group Creation Info: ${e.message}")
        }
    }
}

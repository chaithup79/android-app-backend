package com.curiq.api.provider

import com.curiq.api.model.SavedItem
import com.curiq.api.service.MetadataOrchestrator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProviderRegressionTestSuite {

    @Autowired
    lateinit var orchestrator: MetadataOrchestrator

    private val testCases = listOf(
        // Instagram
        "https://www.instagram.com/reel/C8abc123/?igsh=xyz",
        "https://www.instagram.com/p/C7def456/",
        "https://instagram.com/stories/username/123456789/",
        
        // YouTube
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "https://youtu.be/dQw4w9WgXcQ?si=abc",
        "https://www.youtube.com/@androiddevelopers",
        "https://www.youtube.com/c/Google",
        
        // LinkedIn
        "https://www.linkedin.com/posts/richard-branson_the-beanstalk-challenge-activity-71234567890-abcd",
        "https://www.linkedin.com/in/prasanth",
        
        // GitHub
        "https://github.com/torvalds/linux",
        
        // Medium
        "https://medium.com/@username/my-awesome-post-12345",
        
        // X / Twitter
        "https://x.com/elonmusk/status/1234567890",
        
        // Facebook
        "https://www.facebook.com/profile.php?id=10000123456789",
        "https://www.facebook.com/zuck",
        
        // Reddit
        "https://www.reddit.com/r/androiddev/comments/12345/example_post/",
        "https://www.reddit.com/user/thisisbillgates/"
    )

    @Test
    fun `test all platform extractors`() {
        for (url in testCases) {
            val item = SavedItem(url = url, userId = 1L)
            
            // Just run it through the orchestrator to ensure it doesn't crash 
            // and successfully completes the pipeline.
            val result = orchestrator.processMetadata(item)
            
            assertNotNull(result)
            // Normalized URL should not contain tracking parameters
            assertFalse(result.url.contains("igsh="))
            assertFalse(result.url.contains("si="))
        }
    }
}

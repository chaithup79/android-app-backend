package com.curiq.api.provider

import com.curiq.api.model.SavedItem
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod

@Component
class RedditProvider : SpecializedProvider {
    private val logger = LoggerFactory.getLogger(RedditProvider::class.java)
    override val priority: Int = 1

    override fun supports(): List<MetadataCapability> = listOf(
        MetadataCapability.TITLE,
        MetadataCapability.SUMMARY,
        MetadataCapability.IMAGE,
        MetadataCapability.AUTHOR
    )

    override fun canHandle(url: String): Boolean {
        return url.contains("reddit.com/r/")
    }

    override fun extractMetadata(item: SavedItem): ProviderResult? {
        val startTime = System.currentTimeMillis()
        return try {
            // Ensure no trailing slash, then append .json
            val cleanUrl = item.url.removeSuffix("/")
            val apiUrl = "$cleanUrl.json"
            
            val restTemplate = RestTemplate()
            // Reddit requires a User-Agent or it might 429
            val headers = HttpHeaders()
            headers.set("User-Agent", "CuriqBot/1.0")
            val entity = HttpEntity<String>(headers)
            
            val response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, List::class.java)
            
            val body = response.body
            if (body != null && body.isNotEmpty()) {
                // Reddit .json returns a list of listings. First element is the post.
                val firstListing = body[0] as? Map<*, *>
                val data = firstListing?.get("data") as? Map<*, *>
                val children = data?.get("children") as? List<*>
                
                if (children != null && children.isNotEmpty()) {
                    val postObj = children[0] as? Map<*, *>
                    val postData = postObj?.get("data") as? Map<*, *>
                    
                    if (postData != null) {
                        val metadata = ExtractedMetadata(
                            title = postData["title"] as? String,
                            summary = postData["selftext"] as? String,
                            imageUrl = postData["url_overridden_by_dest"] as? String ?: postData["thumbnail"] as? String,
                            author = postData["author"] as? String,
                            sourceDomain = "reddit.com",
                            category = "Community"
                        )
                        
                        return ProviderResult(
                            providerName = "RedditProvider",
                            confidence = 100,
                            metadata = metadata,
                            durationMs = System.currentTimeMillis() - startTime
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            logger.warn("RedditProvider failed for URL \${item.url}: \${e.message}")
            null
        }
    }
}


package com.curiq.api.provider

import com.curiq.api.model.SavedItem
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.slf4j.LoggerFactory
import java.net.URLEncoder

@Component
class YoutubeProvider : SpecializedProvider {
    private val logger = LoggerFactory.getLogger(YoutubeProvider::class.java)
    override val priority: Int = 1

    override fun supports(): List<MetadataCapability> = listOf(
        MetadataCapability.TITLE,
        MetadataCapability.IMAGE,
        MetadataCapability.AUTHOR
    )

    override fun canHandle(url: String): Boolean {
        return url.contains("youtube.com/watch") || url.contains("youtu.be/")
    }

    override fun extractMetadata(item: SavedItem): ProviderResult? {
        val startTime = System.currentTimeMillis()
        return try {
            val encodedUrl = URLEncoder.encode(item.url, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encodedUrl&format=json"
            
            val restTemplate = RestTemplate()
            val response = restTemplate.getForObject(oembedUrl, Map::class.java)
            
            if (response != null) {
                val metadata = ExtractedMetadata(
                    title = response["title"] as? String,
                    author = response["author_name"] as? String,
                    imageUrl = response["thumbnail_url"] as? String,
                    sourceDomain = "youtube.com",
                    category = "Video"
                )
                
                ProviderResult(
                    providerName = "YoutubeProvider",
                    confidence = 100,
                    metadata = metadata,
                    durationMs = System.currentTimeMillis() - startTime
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("YoutubeProvider failed for URL \${item.url}: \${e.message}")
            null
        }
    }
}


package com.curiq.api.provider

import com.curiq.api.model.SavedItem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class MicrolinkProvider : GenericProvider {
    private val logger = LoggerFactory.getLogger(MicrolinkProvider::class.java)

    override val priority: Int = 4 // As requested by user, Microlink is a last resort

    override fun supports(): List<MetadataCapability> = listOf(
        MetadataCapability.TITLE,
        MetadataCapability.IMAGE,
        MetadataCapability.SUMMARY,
        MetadataCapability.SOURCE_DOMAIN,
        MetadataCapability.FAVICON
    )

    override fun canHandle(url: String): Boolean {
        return true // Generic fallback
    }

    override fun extractMetadata(item: SavedItem): ProviderResult? {
        val startTime = System.currentTimeMillis()
        return try {
            val apiUrl = "https://api.microlink.io?url=${item.url}"
            val restTemplate = RestTemplate()
            val response = restTemplate.getForObject(apiUrl, Map::class.java)

            if (response != null && response["status"] == "success") {
                val data = response["data"] as? Map<*, *>
                
                if (data != null) {
                    val imageObj = data["image"] as? Map<*, *>
                    val logoObj = data["logo"] as? Map<*, *>
                    
                    val metadata = ExtractedMetadata(
                        title = data["title"] as? String,
                        summary = data["description"] as? String,
                        imageUrl = imageObj?.get("url") as? String,
                        faviconUrl = logoObj?.get("url") as? String,
                        sourceDomain = data["publisher"] as? String,
                        author = data["author"] as? String
                    )

                    return ProviderResult(
                        providerName = "MicrolinkProvider",
                        confidence = 70,
                        metadata = metadata,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                }
            }
            null
        } catch (e: Exception) {
            logger.warn("Microlink extraction failed for URL \${item.url}: \${e.message}")
            null
        }
    }
}


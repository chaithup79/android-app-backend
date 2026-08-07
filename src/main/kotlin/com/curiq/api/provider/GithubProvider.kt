package com.curiq.api.provider

import com.curiq.api.model.SavedItem
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.slf4j.LoggerFactory

@Component
class GithubProvider : SpecializedProvider {
    private val logger = LoggerFactory.getLogger(GithubProvider::class.java)
    override val priority: Int = 1

    override fun supports(): List<MetadataCapability> = listOf(
        MetadataCapability.TITLE,
        MetadataCapability.SUMMARY,
        MetadataCapability.IMAGE,
        MetadataCapability.AUTHOR
    )

    override fun canHandle(url: String): Boolean {
        return url.contains("github.com/")
    }

    override fun extractMetadata(item: SavedItem): ProviderResult? {
        val startTime = System.currentTimeMillis()
        return try {
            // Very simple regex for Owner/Repo
            val regex = Regex("github\\.com/([^/]+)/([^/]+)")
            val match = regex.find(item.url)
            
            if (match != null) {
                val owner = match.groupValues[1]
                val repo = match.groupValues[2]
                
                val apiUrl = "https://api.github.com/repos/$owner/$repo"
                
                val restTemplate = RestTemplate()
                val response = restTemplate.getForObject(apiUrl, Map::class.java)
                
                if (response != null) {
                    val ownerObj = response["owner"] as? Map<*, *>
                    
                    val metadata = ExtractedMetadata(
                        title = response["full_name"] as? String,
                        summary = response["description"] as? String,
                        imageUrl = ownerObj?.get("avatar_url") as? String,
                        author = ownerObj?.get("login") as? String,
                        sourceDomain = "github.com",
                        category = "Programming"
                    )
                    
                    ProviderResult(
                        providerName = "GithubProvider",
                        source = ProviderSource.GITHUB_API, confidence = 100, // Native API is extremely confident
                        metadata = metadata,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("GithubProvider failed for URL \${item.url}: \${e.message}")
            null
        }
    }
}


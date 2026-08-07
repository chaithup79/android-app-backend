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
        MetadataCapability.AUTHOR,
        MetadataCapability.SUMMARY
    )

    override fun canHandle(url: String): Boolean {
        return url.contains("youtube.com/watch") || 
               url.contains("youtu.be/") ||
               url.contains("youtube.com/shorts/") ||
               url.contains("youtube.com/live/")
    }

    override fun extractMetadata(item: SavedItem): ProviderResult? {
        val startTime = System.currentTimeMillis()
        return try {
            var normalizedUrl = item.url
            if (normalizedUrl.contains("/shorts/")) {
                val videoId = normalizedUrl.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
                normalizedUrl = "https://www.youtube.com/watch?v=$videoId"
            } else if (normalizedUrl.contains("/live/")) {
                val videoId = normalizedUrl.substringAfter("/live/").substringBefore("?").substringBefore("/")
                normalizedUrl = "https://www.youtube.com/watch?v=$videoId"
            }

            val encodedUrl = URLEncoder.encode(normalizedUrl, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encodedUrl&format=json"
            
            val restTemplate = RestTemplate()
            val response = restTemplate.getForObject(oembedUrl, Map::class.java)
            
            var fullDescription: String? = null
            try {
                val doc = org.jsoup.Jsoup.connect(normalizedUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(5000)
                    .get()
                val scripts = doc.select("script")
                for (script in scripts) {
                    val html = script.html()
                    
                    // Try to find the shortDescription (used in normal videos)
                    if (html.contains("shortDescription")) {
                        val regex = "\"shortDescription\":\"(.*?)\"".toRegex()
                        val matchResult = regex.find(html)
                        if (matchResult != null) {
                            fullDescription = matchResult.groupValues[1]
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\u0026", "&")
                            break
                        }
                    }
                    
                    // Try to find the attributedDescriptionBodyText (new format)
                    if (fullDescription == null && html.contains("attributedDescriptionBodyText")) {
                        val regex = "\"attributedDescriptionBodyText\":\\{\"content\":\"(.*?)\"\\}".toRegex()
                        val matchResult = regex.find(html)
                        if (matchResult != null) {
                            fullDescription = matchResult.groupValues[1]
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\u0026", "&")
                            break
                        }
                    }
                    
                    // Try to find the simpleText description (used in shorts)
                    if (fullDescription == null && html.contains("\"description\":{\"simpleText\"")) {
                        val regex = "\"description\":\\{\"simpleText\":\"(.*?)\"\\}".toRegex()
                        val matchResult = regex.find(html)
                        if (matchResult != null) {
                            fullDescription = matchResult.groupValues[1]
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\u0026", "&")
                            break
                        }
                    }
                }
                
                // Fallback to og:description if both JSON regexes fail
                if (fullDescription.isNullOrBlank()) {
                    val metaDesc = doc.select("meta[name=description], meta[property=og:description]").firstOrNull()?.attr("content")
                    if (!metaDesc.isNullOrBlank()) {
                        fullDescription = metaDesc
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to scrape full YouTube description: \${e.message}")
            }

            if (response != null) {
                val metadata = ExtractedMetadata(
                    title = response["title"] as? String,
                    author = response["author_name"] as? String,
                    authorUrl = response["author_url"] as? String,
                    imageUrl = response["thumbnail_url"] as? String,
                    summary = fullDescription,
                    sourceDomain = "youtube.com",
                    category = "Video"
                )
                
                ProviderResult(
                    providerName = "YoutubeProvider",
                    source = ProviderSource.YOUTUBE_API, confidence = 100,
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


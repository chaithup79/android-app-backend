package com.curiq.api.provider

import com.curiq.api.model.SavedItem
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OpenGraphProvider : GenericProvider {
    private val logger = LoggerFactory.getLogger(OpenGraphProvider::class.java)

    override val priority: Int = 3

    override fun supports(): List<MetadataCapability> = listOf(
        MetadataCapability.TITLE,
        MetadataCapability.IMAGE,
        MetadataCapability.SUMMARY,
        MetadataCapability.SOURCE_DOMAIN
    )

    override fun canHandle(url: String): Boolean {
        return true // Generic fallback
    }

    override fun extractMetadata(item: SavedItem): ProviderResult? {
        val startTime = System.currentTimeMillis()
        return try {
            val doc = Jsoup.connect(item.url)
                .userAgent("Mozilla/5.0")
                .timeout(5000)
                .get()

            var title: String? = null
            var description: String? = null
            var image: String? = null
            var siteName: String? = null

            val metaTags = doc.select("meta")
            for (tag in metaTags) {
                val property = tag.attr("property")
                val content = tag.attr("content")

                when (property) {
                    "og:title" -> title = content
                    "og:description" -> description = content
                    "og:image" -> image = content
                    "og:site_name" -> siteName = content
                }
            }

            if (title == null && description == null && image == null) {
                return null
            }
            
            val metadata = ExtractedMetadata(
                title = title ?: doc.title(),
                summary = description,
                imageUrl = image,
                sourceDomain = siteName
            )

            ProviderResult(
                providerName = "OpenGraphProvider",
                source = ProviderSource.OPENGRAPH, confidence = 50,
                metadata = metadata,
                durationMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            logger.warn("OpenGraph extraction failed for URL \${item.url}: \${e.message}")
            null
        }
    }
}


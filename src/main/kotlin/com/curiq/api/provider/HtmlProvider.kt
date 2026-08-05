package com.curiq.api.provider

import com.curiq.api.model.SavedItem
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HtmlProvider : GenericProvider {
    private val logger = LoggerFactory.getLogger(HtmlProvider::class.java)

    override val priority: Int = 2

    override fun supports(): List<MetadataCapability> = listOf(
        MetadataCapability.TITLE,
        MetadataCapability.SUMMARY,
        MetadataCapability.FAVICON
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

            val title = doc.title()
            val description = doc.select("meta[name=description]").attr("content")
            
            var favicon = doc.select("link[rel~=(?i)^(shortcut )?icon]").attr("href")
            if (favicon.isNotBlank() && !favicon.startsWith("http")) {
                val baseUri = java.net.URI(item.url)
                val baseUrl = "${baseUri.scheme}://${baseUri.authority}"
                favicon = if (favicon.startsWith("/")) {
                    "$baseUrl$favicon"
                } else {
                    "$baseUrl/$favicon"
                }
            }
            
            if (title.isBlank() && description.isBlank() && favicon.isBlank()) {
                return null
            }
            
            val metadata = ExtractedMetadata(
                title = title.takeIf { it.isNotBlank() },
                summary = description.takeIf { it.isNotBlank() },
                faviconUrl = favicon.takeIf { it.isNotBlank() }
            )

            ProviderResult(
                providerName = "HtmlProvider",
                confidence = 30,
                metadata = metadata,
                durationMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            logger.warn("Html extraction failed for URL \${item.url}: \${e.message}")
            null
        }
    }
}


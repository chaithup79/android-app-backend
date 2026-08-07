package com.curiq.api.provider

import com.curiq.api.model.SavedItem
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class InstagramProvider(
    private val openGraphProvider: OpenGraphProvider,
    private val htmlProvider: HtmlProvider,
    private val microlinkProvider: MicrolinkProvider
) : SpecializedProvider {
    private val logger = LoggerFactory.getLogger(InstagramProvider::class.java)
    
    override val priority: Int = 1

    override fun supports(): List<MetadataCapability> = listOf(
        MetadataCapability.TITLE,
        MetadataCapability.SUMMARY,
        MetadataCapability.IMAGE,
        MetadataCapability.FAVICON
    )

    override fun canHandle(url: String): Boolean {
        return url.contains("instagram.com")
    }

    override fun extractMetadata(item: SavedItem): ProviderResult? {
        // 1. Normalize URL
        val cleanUrl = normalizeUrl(item.url)
        val tempItem = item.copy(url = cleanUrl)

        logger.info("InstagramProvider executing internal strategy for: \$cleanUrl")

        // 2. Try Microlink (Prioritized for better AI summaries)
        var result = microlinkProvider.extractMetadata(tempItem)
        if (isSufficient(result)) {
            logger.info("InstagramProvider succeeded using Microlink")
            return result?.copy(providerName = "InstagramProvider (Microlink)", source = ProviderSource.MICROLINK, confidence = 95)
        }

        // 3. Try OpenGraph (Fallback)
        result = openGraphProvider.extractMetadata(tempItem)
        if (isSufficient(result)) {
            logger.info("InstagramProvider succeeded using OpenGraph")
            return result?.copy(providerName = "InstagramProvider (OpenGraph)", source = ProviderSource.HTML, confidence = 80)
        }

        // 4. Try HTML (Last Resort)
        result = htmlProvider.extractMetadata(tempItem)
        if (isSufficient(result)) {
            logger.info("InstagramProvider succeeded using HTML")
            return result?.copy(providerName = "InstagramProvider (HTML)", source = ProviderSource.MICROLINK, confidence = 70)
        }

        // 5. Fallback
        logger.info("InstagramProvider falling back to partial result")
        val categoryType = if (cleanUrl.contains("/reel/")) "Reel" else "Post"
        
        return ProviderResult(
            providerName = "InstagramProvider (Fallback)",
            source = ProviderSource.FALLBACK, confidence = 20,
            metadata = ExtractedMetadata(
                category = categoryType,
                sourceDomain = "instagram.com"
            )
        )
    }

    private fun normalizeUrl(url: String): String {
        var cleanUrl = url
        // Strip tracking parameters
        cleanUrl = cleanUrl.substringBefore("?igsh=")
        cleanUrl = cleanUrl.substringBefore("?utm_source=")
        cleanUrl = cleanUrl.substringBefore("?fbclid=")
        return cleanUrl
    }

    private fun isSufficient(result: ProviderResult?): Boolean {
        if (result == null) return false
        val meta = result.metadata
        return !meta.title.isNullOrBlank() || !meta.imageUrl.isNullOrBlank() || !meta.summary.isNullOrBlank()
    }
}

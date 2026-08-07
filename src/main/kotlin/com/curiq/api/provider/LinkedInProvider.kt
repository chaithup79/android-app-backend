package com.curiq.api.provider

import com.curiq.api.model.SavedItem
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class LinkedInProvider(
    private val openGraphProvider: OpenGraphProvider,
    private val htmlProvider: HtmlProvider,
    private val microlinkProvider: MicrolinkProvider
) : SpecializedProvider {
    
    private val logger = LoggerFactory.getLogger(LinkedInProvider::class.java)
    
    override val priority: Int = 1

    override fun supports(): List<MetadataCapability> = listOf(
        MetadataCapability.TITLE,
        MetadataCapability.SUMMARY,
        MetadataCapability.IMAGE,
        MetadataCapability.AUTHOR
    )

    override fun canHandle(url: String): Boolean {
        val regex = Regex("linkedin.com")
        return regex.containsMatchIn(url)
    }

    override fun extractMetadata(item: SavedItem): ProviderResult? {
        logger.info("LinkedInProvider executing internal strategy for: \${item.url}")

        // 1. Try OpenGraph first
        var result = openGraphProvider.extractMetadata(item)
        if (isSufficient(result)) {
            logger.info("LinkedInProvider succeeded using OpenGraph")
            return result?.copy(providerName = "LinkedInProvider (OpenGraph)", source = ProviderSource.OPENGRAPH, confidence = 90)
        }

        // 2. Try HTML
        result = htmlProvider.extractMetadata(item)
        if (isSufficient(result)) {
            logger.info("LinkedInProvider succeeded using HTML")
            return result?.copy(providerName = "LinkedInProvider (HTML)", source = ProviderSource.HTML, confidence = 80)
        }

        // 3. Fallback to Microlink (As requested, not first)
        logger.info("LinkedInProvider falling back to Microlink")
        result = microlinkProvider.extractMetadata(item)
        if (result != null) {
            return result.copy(providerName = "LinkedInProvider (Microlink)", source = ProviderSource.MICROLINK, confidence = 70)
        }
        
        return null
    }
    
    private fun isSufficient(result: ProviderResult?): Boolean {
        if (result == null) return false
        val meta = result.metadata
        return !meta.title.isNullOrBlank() || !meta.summary.isNullOrBlank()
    }
}


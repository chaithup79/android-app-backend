package com.curiq.api.provider

import com.curiq.api.model.SavedItem
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class ThreadsProvider(
    private val openGraphProvider: OpenGraphProvider,
    private val htmlProvider: HtmlProvider,
    private val microlinkProvider: MicrolinkProvider
) : SpecializedProvider {
    
    private val logger = LoggerFactory.getLogger(ThreadsProvider::class.java)
    
    override val priority: Int = 1

    override fun supports(): List<MetadataCapability> = listOf(
        MetadataCapability.TITLE,
        MetadataCapability.SUMMARY,
        MetadataCapability.IMAGE,
        MetadataCapability.AUTHOR
    )

    override fun canHandle(url: String): Boolean {
        val domain = when("Threads") {
            "Facebook" -> "facebook.com"
            "X" -> "twitter.com|x.com"
            "Threads" -> "threads.net"
            "Medium" -> "medium.com"
            "StackOverflow" -> "stackoverflow.com"
            else -> ".com"
        }
        return Regex(domain).containsMatchIn(url)
    }

    override fun extractMetadata(item: SavedItem): ProviderResult? {
        val startTime = System.currentTimeMillis()
        logger.info("ThreadsProvider executing internal strategy for: ${item.url}")

        // 1. Try OpenGraph first
        var result = openGraphProvider.extractMetadata(item)
        if (isSufficient(result)) {
            logger.info("ThreadsProvider succeeded using OpenGraph")
            return result?.copy(
                providerName = "ThreadsProvider", 
                source = ProviderSource.OPENGRAPH, 
                confidence = 90,
                durationMs = System.currentTimeMillis() - startTime
            )
        }

        // 2. Try HTML
        result = htmlProvider.extractMetadata(item)
        if (isSufficient(result)) {
            logger.info("ThreadsProvider succeeded using HTML")
            return result?.copy(
                providerName = "ThreadsProvider", 
                source = ProviderSource.HTML, 
                confidence = 80,
                durationMs = System.currentTimeMillis() - startTime
            )
        }

        // 3. Fallback to Microlink
        logger.info("ThreadsProvider falling back to Microlink")
        result = microlinkProvider.extractMetadata(item)
        if (result != null) {
            return result.copy(
                providerName = "ThreadsProvider", 
                source = ProviderSource.MICROLINK, 
                confidence = 70,
                durationMs = System.currentTimeMillis() - startTime
            )
        }
        
        return null
    }
    
    private fun isSufficient(result: ProviderResult?): Boolean {
        if (result == null) return false
        val meta = result.metadata
        return !meta.title.isNullOrBlank() || !meta.summary.isNullOrBlank()
    }
}

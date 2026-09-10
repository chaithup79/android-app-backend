package com.curiq.api.service

import com.curiq.api.model.SavedItem
import com.curiq.api.provider.MetadataProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.curiq.api.provider.ProviderResult
import com.curiq.api.provider.SpecializedProvider
import com.curiq.api.provider.GenericProvider

@Service
class MetadataOrchestrator(
    private val specializedProviders: List<SpecializedProvider>,
    private val genericProviders: List<GenericProvider>,
    private val metadataMerger: MetadataMerger,
    private val imageDownloaderService: ImageDownloaderService
) {
    private val logger = LoggerFactory.getLogger(MetadataOrchestrator::class.java)
    
    private val sortedGenericProviders = genericProviders.sortedBy { it.priority }
    private val sortedSpecializedProviders = specializedProviders.sortedBy { it.priority }

    fun processMetadata(item: SavedItem): SavedItem {
        val cleanUrl = normalizeUrl(item.url)
        val normalizedItem = item.copy(url = cleanUrl)
        
        logger.info("Orchestrating metadata extraction for: $cleanUrl (Original: ${item.url})")
        
        val results = mutableListOf<ProviderResult>()
        var specializedSucceededWithoutSummary = false

        // 1. Check Specialized Providers first
        for (provider in sortedSpecializedProviders) {
            if (provider.canHandle(cleanUrl)) {
                logger.info("Specialized Provider ${provider::class.simpleName} taking ownership of $cleanUrl")
                var result = provider.extractMetadata(normalizedItem)
                if (result != null) {
                    // Log metrics
                    logger.info(
                        "Provider Metrics -> Provider: ${result.providerName} | " +
                        "Source: ${result.source?.name ?: "UNKNOWN"} | " +
                        "Time: ${result.durationMs}ms | " +
                        "Confidence: ${result.confidence}"
                    )
                    
                    // Check if YouTube gave us their generic useless description
                    if (result.metadata.summary?.startsWith("Enjoy the videos", ignoreCase = true) == true) {
                        result = result.copy(metadata = result.metadata.copy(summary = null))
                    }
                    
                    results.add(result)
                    
                    if (!result.metadata.summary.isNullOrBlank()) {
                        // We have a summary, we can stop here
                        item.summary = result.metadata.summary ?: item.summary
                        item.imageUrl = getPermanentUrl(result.metadata.imageUrl ?: item.imageUrl)
                        item.faviconUrl = result.metadata.faviconUrl ?: item.faviconUrl
                        item.sourceDomain = result.metadata.sourceDomain ?: item.sourceDomain
                        item.author = result.metadata.author ?: item.author
                        item.authorUrl = result.metadata.authorUrl ?: item.authorUrl
                        item.authorAvatar = result.metadata.authorAvatar ?: item.authorAvatar
                        item.url = cleanUrl // Save the normalized URL
                        
                        if (item.category.isNullOrBlank()) {
                            item.category = result.metadata.category
                        }
                        return item
                    } else {
                        logger.info("Specialized Provider ${provider::class.simpleName} missing summary. Proceeding to generics to fill gaps.")
                        specializedSucceededWithoutSummary = true
                        break // Break to run generics
                    }
                } else {
                    logger.warn("Specialized Provider ${provider::class.simpleName} failed for $cleanUrl. Falling back to generics.")
                    continue
                }
            }
        }
        
        // 2. Generic Pipeline Fallback
        if (!specializedSucceededWithoutSummary) {
            logger.info("No specialized provider found or fully succeeded, running generic pipeline")
        }
        
        val dynamicGenericProviders = genericProviders.sortedBy { provider ->
            if (provider is com.curiq.api.provider.MicrolinkProvider && cleanUrl.contains("instagram.com")) {
                0 // Microlink is first for Instagram
            } else {
                provider.priority
            }
        }
        
        for (provider in dynamicGenericProviders) {
            if (provider.canHandle(cleanUrl)) {
                logger.debug("Generic Provider ${provider::class.simpleName} is handling URL: $cleanUrl")
                val result = provider.extractMetadata(normalizedItem)
                
                if (result != null) {
                    results.add(result)
                    logger.info(
                        "Provider Metrics -> Provider: ${result.providerName} | " +
                        "Source: ${result.source?.name ?: "UNKNOWN"} | " +
                        "Time: ${result.durationMs}ms | " +
                        "Confidence: ${result.confidence}"
                    )
                }
            }
        }
        
        if (results.isEmpty()) {
            logger.warn("All generic metadata providers failed for URL: $cleanUrl")
            item.url = cleanUrl
            return item
        }

        val finalMetadata = metadataMerger.merge(results)
        
        item.title = finalMetadata.title ?: item.title
        item.summary = finalMetadata.summary ?: item.summary
        item.imageUrl = getPermanentUrl(finalMetadata.imageUrl ?: item.imageUrl)
        item.faviconUrl = finalMetadata.faviconUrl ?: item.faviconUrl
        item.sourceDomain = finalMetadata.sourceDomain ?: item.sourceDomain
        item.author = finalMetadata.author ?: item.author
        item.authorUrl = finalMetadata.authorUrl ?: item.authorUrl
        item.authorAvatar = finalMetadata.authorAvatar ?: item.authorAvatar
        item.url = cleanUrl
        
        if (item.category.isNullOrBlank()) {
            item.category = finalMetadata.category
        }
        
        return item
    }

    private fun getPermanentUrl(tempImageUrl: String?): String? {
        if (tempImageUrl.isNullOrBlank()) return tempImageUrl
        try {
            val url = java.net.URI(tempImageUrl).toURL()
            if (url.host != "curiq.in" && !url.host.endsWith(".curiq.in")) {
                val filename = java.util.UUID.randomUUID().toString()
                return imageDownloaderService.downloadAndCompressImage(tempImageUrl, filename) ?: tempImageUrl
            }
            return tempImageUrl
        } catch (e: Exception) {
            logger.warn("Failed to parse or download image URL: $tempImageUrl", e)
            return tempImageUrl
        }
    }

    private fun normalizeUrl(url: String): String {
        var cleanUrl = url
        val queryParamsToRemove = listOf("igsh", "utm_source", "fbclid", "feature", "si")
        
        for (param in queryParamsToRemove) {
            cleanUrl = cleanUrl.substringBefore("?$param=")
            cleanUrl = cleanUrl.substringBefore("&$param=")
        }
        return cleanUrl
    }

    // Temporary fallback for legacy AiJobExecutor
    fun extractMetadata(url: String): String {
        return url
    }
}

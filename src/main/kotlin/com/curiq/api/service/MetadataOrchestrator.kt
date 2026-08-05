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
    private val metadataMerger: MetadataMerger
) {
    private val logger = LoggerFactory.getLogger(MetadataOrchestrator::class.java)
    
    private val sortedGenericProviders = genericProviders.sortedBy { it.priority }
    private val sortedSpecializedProviders = specializedProviders.sortedBy { it.priority }

    fun processMetadata(item: SavedItem): SavedItem {
        logger.info("Orchestrating metadata extraction for: ${item.url}")
        
        // 1. Check Specialized Providers first
        for (provider in sortedSpecializedProviders) {
            if (provider.canHandle(item.url)) {
                logger.info("Specialized Provider ${provider::class.simpleName} taking ownership of ${item.url}")
                val result = provider.extractMetadata(item)
                if (result != null) {
                    item.summary = result.metadata.summary ?: item.summary
                    item.imageUrl = result.metadata.imageUrl ?: item.imageUrl
                    item.faviconUrl = result.metadata.faviconUrl ?: item.faviconUrl
                    item.sourceDomain = result.metadata.sourceDomain ?: item.sourceDomain
                    
                    // NEVER overwrite a user's manually chosen category with a scraped one
                    if (item.category.isNullOrBlank()) {
                        item.category = result.metadata.category
                    }
                    
                    return item
                } else {
                    logger.warn("Specialized Provider ${provider::class.simpleName} failed for ${item.url}")
                    return item // Do not fall back to generics if specialized took ownership
                }
            }
        }
        
        // 2. Generic Pipeline Fallback
        logger.info("No specialized provider found, running generic pipeline")
        val results = mutableListOf<ProviderResult>()
        
        for (provider in sortedGenericProviders) {
            if (provider.canHandle(item.url)) {
                logger.debug("Generic Provider ${provider::class.simpleName} is handling URL: ${item.url}")
                val result = provider.extractMetadata(item)
                
                if (result != null) {
                    results.add(result)
                    logger.info("Provider ${provider::class.simpleName} succeeded with confidence ${result.confidence}")
                }
            }
        }
        
        if (results.isEmpty()) {
            logger.warn("All generic metadata providers failed for URL: ${item.url}")
            return item
        }

        val finalMetadata = metadataMerger.merge(results)
        
        item.title = finalMetadata.title ?: item.title
        item.summary = finalMetadata.summary ?: item.summary
        item.imageUrl = finalMetadata.imageUrl ?: item.imageUrl
        item.faviconUrl = finalMetadata.faviconUrl ?: item.faviconUrl
        item.sourceDomain = finalMetadata.sourceDomain ?: item.sourceDomain
        
        if (item.category.isNullOrBlank()) {
            item.category = finalMetadata.category
        }
        
        return item
    }

    // Temporary fallback for legacy AiJobExecutor
    fun extractMetadata(url: String): String {
        return url
    }
}

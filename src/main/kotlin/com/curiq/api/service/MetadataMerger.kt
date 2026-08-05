package com.curiq.api.service

import com.curiq.api.provider.ProviderResult
import com.curiq.api.provider.ExtractedMetadata
import org.springframework.stereotype.Component

@Component
class MetadataMerger {

    fun merge(results: List<ProviderResult>): ExtractedMetadata {
        var mergedSummary: String? = null
        var summaryConfidence = -1
        
        var mergedImageUrl: String? = null
        var imageConfidence = -1

        var mergedFaviconUrl: String? = null
        var faviconConfidence = -1

        var mergedCategory: String? = null
        var categoryConfidence = -1

        var mergedSourceDomain: String? = null
        var domainConfidence = -1

        var mergedAuthor: String? = null
        var authorConfidence = -1

        var mergedTitle: String? = null
        var titleConfidence = -1

        var mergedVideoUrl: String? = null
        var videoConfidence = -1

        var mergedTags: List<String>? = null
        var tagsConfidence = -1

        // Sort by confidence just in case, though we check > below
        val sortedResults = results.sortedByDescending { it.confidence }

        for (result in sortedResults) {
            val conf = result.confidence
            val meta = result.metadata

            if (meta.summary != null && conf > summaryConfidence) {
                mergedSummary = meta.summary
                summaryConfidence = conf
            }
            if (meta.imageUrl != null && conf > imageConfidence) {
                mergedImageUrl = meta.imageUrl
                imageConfidence = conf
            }
            if (meta.faviconUrl != null && conf > faviconConfidence) {
                mergedFaviconUrl = meta.faviconUrl
                faviconConfidence = conf
            }
            if (meta.category != null && conf > categoryConfidence) {
                mergedCategory = meta.category
                categoryConfidence = conf
            }
            if (meta.sourceDomain != null && conf > domainConfidence) {
                mergedSourceDomain = meta.sourceDomain
                domainConfidence = conf
            }
            if (meta.author != null && conf > authorConfidence) {
                mergedAuthor = meta.author
                authorConfidence = conf
            }
            if (meta.title != null && conf > titleConfidence) {
                mergedTitle = meta.title
                titleConfidence = conf
            }
            if (meta.videoUrl != null && conf > videoConfidence) {
                mergedVideoUrl = meta.videoUrl
                videoConfidence = conf
            }
            if (meta.tags != null && conf > tagsConfidence) {
                mergedTags = meta.tags
                tagsConfidence = conf
            }
        }

        return ExtractedMetadata(
            summary = mergedSummary,
            imageUrl = mergedImageUrl,
            faviconUrl = mergedFaviconUrl,
            category = mergedCategory,
            sourceDomain = mergedSourceDomain,
            author = mergedAuthor,
            title = mergedTitle,
            videoUrl = mergedVideoUrl,
            tags = mergedTags
        )
    }
}

package com.curiq.api.provider

import com.curiq.api.model.SavedItem

data class ProviderResult(
    val providerName: String,
    val confidence: Int,
    val metadata: ExtractedMetadata,
    val durationMs: Long = 0L,
    val cached: Boolean = false
)

enum class MetadataCapability {
    TITLE,
    IMAGE,
    SUMMARY,
    AUTHOR,
    VIDEO,
    TAGS,
    FAVICON,
    SOURCE_DOMAIN
}

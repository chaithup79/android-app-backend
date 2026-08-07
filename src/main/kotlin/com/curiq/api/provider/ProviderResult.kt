package com.curiq.api.provider

import com.curiq.api.model.SavedItem

data class ProviderResult(
    val providerName: String,
    val confidence: Int,
    val metadata: ExtractedMetadata,
    val durationMs: Long = 0L,
    val cached: Boolean = false,
    val source: ProviderSource? = null
)

enum class ProviderSource {
    OPENGRAPH,
    HTML,
    MICROLINK,
    YOUTUBE_API,
    GITHUB_API,
    REDDIT_API,
    NATIVE,
    OLLAMA,
    FALLBACK
}

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

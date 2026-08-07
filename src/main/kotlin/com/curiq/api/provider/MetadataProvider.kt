package com.curiq.api.provider

import com.curiq.api.model.SavedItem

interface MetadataProvider {
    fun canHandle(url: String): Boolean
    fun extractMetadata(item: SavedItem): ProviderResult?
    val priority: Int
    fun supports(): List<MetadataCapability>
}

interface SpecializedProvider : MetadataProvider
interface GenericProvider : MetadataProvider

data class ExtractedMetadata(
    val summary: String? = null,
    val imageUrl: String? = null,
    val faviconUrl: String? = null,
    val category: String? = null,
    val sourceDomain: String? = null,
    val author: String? = null,
    val authorUrl: String? = null,
    val authorAvatar: String? = null,
    val title: String? = null,
    val videoUrl: String? = null,
    val tags: List<String>? = null
)

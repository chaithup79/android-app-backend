package com.curiq.api.provider

import com.curiq.api.model.SavedItem
import org.springframework.stereotype.Component

@Component
class FacebookProvider(
    private val microlinkProvider: MicrolinkProvider
) : SpecializedProvider {
    
    override val priority: Int = 1

    override fun supports(): List<MetadataCapability> = microlinkProvider.supports()

    override fun canHandle(url: String): Boolean {
        val regex = Regex("facebook.com")
        return regex.containsMatchIn(url)
    }

    override fun extractMetadata(item: SavedItem): ProviderResult? {
        // Temporarily delegate to Microlink until Native implementation is built
        val result = microlinkProvider.extractMetadata(item)
        return result?.copy(providerName = "FacebookProvider")
    }
}


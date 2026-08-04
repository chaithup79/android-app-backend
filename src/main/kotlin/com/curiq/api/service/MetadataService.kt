package com.curiq.api.service

import org.springframework.stereotype.Service

@Service
class MetadataService {
    // In a real implementation, this would use Jsoup to fetch OpenGraph tags
    fun extractMetadata(text: String): String {
        return text
    }
}

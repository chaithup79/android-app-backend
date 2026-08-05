package com.curiq.api.dto

data class AiProcessResponse(
    val ai_summary: String,
    val ai_category: String,
    val ai_confidence: Short?,
    val tags: List<String>?,
    val readingTime: Int?,
    val difficulty: String?
)

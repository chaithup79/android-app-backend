package com.curiq.api.dto

data class AiProcessRequest(
    val sharedText: String,
    val source: String? = null,
    val locale: String? = null,
    val correlationId: String? = null,
    val forcedCategory: String? = null
)

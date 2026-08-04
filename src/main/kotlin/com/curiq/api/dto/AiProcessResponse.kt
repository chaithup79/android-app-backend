package com.curiq.api.dto

data class AiProcessResponse(
    val summary: String,
    val category: String,
    val subcategory: String,
    val tags: List<String>
)

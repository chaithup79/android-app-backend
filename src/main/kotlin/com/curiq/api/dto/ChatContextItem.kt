package com.curiq.api.dto

data class ChatContextItem(
    val id: Long,
    val title: String?,
    val summary: String,
    val category: String?,
    val tags: List<String>,
    val url: String?
)

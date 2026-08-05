package com.curiq.api.model

data class SyncRequest(
    val syncToken: String?,
    val changes: List<SavedItem>
)

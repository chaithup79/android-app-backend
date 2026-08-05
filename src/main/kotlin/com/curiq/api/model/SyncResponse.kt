package com.curiq.api.model

data class SyncResponse(
    val serverChanges: List<SavedItem>,
    val nextSyncToken: String,
    val serverTime: Long
)

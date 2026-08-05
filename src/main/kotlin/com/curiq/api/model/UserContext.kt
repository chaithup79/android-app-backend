package com.curiq.api.model

data class UserContext(
    val internalId: Long,
    val uid: String,
    val email: String?,
    val displayName: String?,
    val anonymous: Boolean
)

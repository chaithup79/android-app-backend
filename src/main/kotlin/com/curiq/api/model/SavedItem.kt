package com.curiq.api.model

import jakarta.persistence.*

@Entity
@Table(name = "saved_items")
data class SavedItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 36)
    var uuid: String = "",

    @Column(name = "user_id")
    var userId: Long? = null,

    @Convert(converter = com.curiq.api.config.StringCryptoConverter::class)
    @Column(nullable = false, columnDefinition = "TEXT")
    var url: String = "",

    @Column(name = "url_hash", length = 64)
    var urlHash: String? = null,

    @Convert(converter = com.curiq.api.config.StringCryptoConverter::class)
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(columnDefinition = "TEXT")
    var title: String? = null,

    @Column(nullable = false)
    var source: String = "",

    @Column(nullable = false)
    var status: String = "",

    @Column
    var category: String? = null,

    @Column(columnDefinition = "TEXT")
    var summary: String? = null,

    @Column(name = "image_url", columnDefinition = "TEXT")
    var imageUrl: String? = null,

    @Column(name = "favicon_url", columnDefinition = "TEXT")
    var faviconUrl: String? = null,

    @Column(name = "source_domain")
    var sourceDomain: String? = null,

    @Column(name = "metadata_status", nullable = false)
    var metadataStatus: String = "READY",

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0L,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0L,

    @Version
    @Column(nullable = false)
    var version: Long = 1,

    @Column(name = "sync_status", nullable = false)
    var syncStatus: String = "SYNCED",

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    var aiSummary: String? = null,

    @Column(name = "ai_category", length = 100)
    var aiCategory: String? = null,

    @Column(name = "ai_confidence")
    var aiConfidence: Short? = null,

    @Column(name = "ai_status", nullable = false, length = 30)
    var aiStatus: String = "PENDING",

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "ai_metadata", columnDefinition = "jsonb")
    var aiMetadata: Map<String, Any>? = null,

    @Column(nullable = false)
    var deleted: Boolean = false
)

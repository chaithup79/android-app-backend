package com.curiq.api.model

import jakarta.persistence.*

@Entity
@Table(name = "saved_items")
data class SavedItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val correlationId: String = "",

    @Column(columnDefinition = "TEXT")
    var summary: String? = null,

    var category: String? = null,
    
    var subcategory: String? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saved_item_tags", joinColumns = [JoinColumn(name = "saved_item_id")])
    @Column(name = "tag")
    var tags: MutableList<String> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    var processingStatus: ProcessingStatus = ProcessingStatus.QUEUED,

    @Column(columnDefinition = "TEXT", nullable = false)
    val originalText: String = ""
)

enum class ProcessingStatus {
    QUEUED, PROCESSING, COMPLETED, FAILED
}

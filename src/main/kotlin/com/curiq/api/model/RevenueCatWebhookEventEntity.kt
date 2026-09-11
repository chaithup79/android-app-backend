package com.curiq.api.model

import jakarta.persistence.*

@Entity
@Table(name = "revenuecat_webhook_events")
data class RevenueCatWebhookEventEntity(
    @Id
    @Column(name = "event_id", nullable = false, unique = true, length = 128)
    var eventId: String = "",

    @Column(name = "event_type", nullable = false)
    var eventType: String = "",

    @Column(name = "processed_at", nullable = false)
    var processedAt: Long = System.currentTimeMillis()
)

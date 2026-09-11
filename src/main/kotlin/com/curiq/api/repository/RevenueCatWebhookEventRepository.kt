package com.curiq.api.repository

import com.curiq.api.model.RevenueCatWebhookEventEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RevenueCatWebhookEventRepository : JpaRepository<RevenueCatWebhookEventEntity, String>

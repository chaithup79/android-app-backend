package com.curiq.api.controller

import com.curiq.api.model.SavedItem
import com.curiq.api.repository.SavedItemRepository
import com.curiq.api.service.CurrentUserService
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

import com.curiq.api.model.SyncRequest
import com.curiq.api.model.SyncResponse

@RestController
@RequestMapping("/api/v1")
class SavedItemController(
    private val repository: SavedItemRepository,
    private val currentUserService: CurrentUserService,
    private val jobService: com.curiq.api.service.JobService
) {

    private val logger = org.slf4j.LoggerFactory.getLogger(SavedItemController::class.java)

    @PostMapping("/sync")
    fun syncBookmarks(@RequestBody request: SyncRequest): ResponseEntity<SyncResponse> {
        val userId = currentUserService.userId()
        val serverTime = System.currentTimeMillis()
        
        logger.info("Received sync request with ${request.changes.size} changes")

        // 1. Process client changes
        for (clientItem in request.changes) {
            clientItem.userId = userId
            logger.info("Processing clientItem: uuid=${clientItem.uuid}, url=${clientItem.url}, metadataStatus=${clientItem.metadataStatus}")
            
            if (clientItem.urlHash.isNullOrEmpty() && clientItem.url.isNotBlank()) {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(clientItem.url.toByteArray(Charsets.UTF_8))
                clientItem.urlHash = hashBytes.joinToString("") { "%02x".format(it) }
            }

            val existing = repository.findByUuid(clientItem.uuid)
            
            if (existing.isPresent) {
                val dbItem = existing.get()
                logger.info("Found existing item with id=${dbItem.id} for uuid=${clientItem.uuid}")
                if (dbItem.userId != userId) {
                    continue // Ignore access violation
                }
                
                // Conflict Resolution
                if (clientItem.version > dbItem.version) {
                    clientItem.id = dbItem.id
                    clientItem.updatedAt = serverTime
                    try { repository.save(clientItem) } catch (e: org.springframework.dao.OptimisticLockingFailureException) { /* Server won */ }
                } else if (clientItem.version == dbItem.version) {
                    if (clientItem.updatedAt > dbItem.updatedAt) {
                        clientItem.id = dbItem.id
                        clientItem.updatedAt = serverTime
                        try { repository.save(clientItem) } catch (e: org.springframework.dao.OptimisticLockingFailureException) { /* Server won */ }
                    }
                }
            } else {
                // Deduplication is handled by the client. We accept whatever the client sends.
                logger.info("No existing item found for uuid=${clientItem.uuid}. Creating new item.")

                clientItem.updatedAt = serverTime
                
                // If it's a completely new bookmark coming from Android, enqueue a Metadata Job
                if (!clientItem.deleted && clientItem.metadataStatus == "PROCESSING_METADATA") {
                    try {
                        clientItem.id = null // Force Hibernate to treat this as a NEW entity (Android sends id = 0)
                        val saved = repository.save(clientItem)
                        logger.info("Saved new item with id=${saved.id}. Enqueuing METADATA job.")
                        jobService.enqueue(com.curiq.api.model.JobType.METADATA, saved.id.toString())
                    } catch (e: org.springframework.dao.OptimisticLockingFailureException) { 
                        logger.error("Optimistic locking failure when saving new item: uuid=${clientItem.uuid}", e)
                    }
                } else {
                    logger.info("metadataStatus is ${clientItem.metadataStatus}, skipping job enqueue.")
                    try {
                        clientItem.id = null // Force Hibernate to treat this as a NEW entity
                        repository.save(clientItem)
                    } catch (e: org.springframework.dao.OptimisticLockingFailureException) { 
                        logger.error("Optimistic locking failure when saving new item: uuid=${clientItem.uuid}", e)
                    }
                }
            }
        }
        
        // 2. Fetch server changes for client
        val lastSyncTimestamp = request.syncToken?.toLongOrNull() ?: 0L
        val serverChanges = repository.findByUserIdAndUpdatedAtGreaterThan(userId, lastSyncTimestamp)
        
        val nextSyncToken = serverTime.toString()
        
        return ResponseEntity.ok(SyncResponse(serverChanges, nextSyncToken, serverTime))
    }
}

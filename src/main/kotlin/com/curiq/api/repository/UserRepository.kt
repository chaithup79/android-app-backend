package com.curiq.api.repository

import com.curiq.api.model.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByFirebaseUid(firebaseUid: String): Optional<UserEntity>
}

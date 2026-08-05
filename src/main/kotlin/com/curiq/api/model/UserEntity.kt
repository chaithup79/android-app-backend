package com.curiq.api.model

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
    var firebaseUid: String = "",

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "display_name")
    var displayName: String? = null,

    @Column(name = "photo_url", columnDefinition = "TEXT")
    var photoUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = System.currentTimeMillis(),

    @Column(name = "last_login_at", nullable = false)
    var lastLoginAt: Long = System.currentTimeMillis()
)

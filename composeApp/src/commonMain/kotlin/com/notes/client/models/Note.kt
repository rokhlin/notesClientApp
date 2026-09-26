package com.notes.client.models

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

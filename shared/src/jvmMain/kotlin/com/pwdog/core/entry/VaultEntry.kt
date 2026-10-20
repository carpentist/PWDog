package com.pwdog.core.entry

data class VaultEntry(
    val id: String,
    val title: String,
    val url: String,
    val username: String,
    val password: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)

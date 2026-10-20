package com.pwdog.core.entry

import java.net.URI

object HostMatcher {
    fun hostOf(url: String?): String? {
        val trimmed = url?.trim().orEmpty()
        if (trimmed.isEmpty()) return null

        val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return runCatching { URI(candidate).host }.getOrNull()?.let(::normalizeHost)
    }

    fun normalizeHost(host: String?): String? {
        val value = host?.trim()?.lowercase()?.removeSuffix(".").orEmpty()
        if (value.isEmpty() || value.length > 253) return null
        if (!value.matches(HOST_PATTERN)) return null
        if (value.startsWith(".") || value.endsWith(".") || value.contains("..")) return null
        return value
    }

    fun matches(url: String?, requestedHost: String?): Boolean {
        val host = normalizeHost(requestedHost) ?: return false
        return hostOf(url) == host
    }

    private val HOST_PATTERN = Regex("[a-z0-9.-]+")
}

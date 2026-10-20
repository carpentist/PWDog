package com.pwdog.core.entry

data class EntryGroup(
    val key: String,
    val host: String?,
    val entries: List<VaultEntry>,
) {
    val label: String
        get() = host ?: entries.firstOrNull()?.title.orEmpty()

    val urlTemplate: String
        get() = entries.firstOrNull { it.url.isNotBlank() }?.url.orEmpty()
}

object EntryGrouping {
    fun group(entries: List<VaultEntry>): List<EntryGroup> =
        entries
            .groupBy { entry ->
                val host = HostMatcher.hostOf(entry.url)
                if (host != null) "host:$host" else "entry:${entry.id}"
            }
            .map { (key, group) ->
                EntryGroup(
                    key = key,
                    host = group.firstOrNull()?.let { HostMatcher.hostOf(it.url) },
                    entries = group,
                )
            }
            .sortedWith(
                compareBy<EntryGroup> { it.host == null }
                    .thenBy { it.label.lowercase() },
            )
}

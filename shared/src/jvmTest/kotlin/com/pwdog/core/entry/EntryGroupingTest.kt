package com.pwdog.core.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EntryGroupingTest {
    @Test
    fun groupsAccountsByExactHost() {
        val entries = listOf(
            entry("1", "GitHub - Work", "https://github.com", "alice@work.example"),
            entry("2", "GitHub - Personal", "https://github.com/login", "alice@personal.example"),
            entry("3", "GitHub Enterprise", "https://sub.github.com", "alice"),
            entry("4", "Standalone", "", "solo"),
            entry("5", "Another standalone", "", "other"),
        )

        val groups = EntryGrouping.group(entries)

        val github = groups.single { it.host == "github.com" }
        assertEquals(2, github.entries.size)
        assertEquals(
            setOf("alice@work.example", "alice@personal.example"),
            github.entries.map { it.username }.toSet(),
        )
        assertEquals("https://github.com", github.urlTemplate)

        val enterprise = groups.single { it.host == "sub.github.com" }
        assertEquals(1, enterprise.entries.size)

        val withoutHost = groups.filter { it.host == null }
        assertEquals(2, withoutHost.size)
        assertNotNull(withoutHost.firstOrNull { it.label == "Standalone" })
    }

    @Test
    fun keepsSingleSiteGroupForOneAccount() {
        val groups = EntryGrouping.group(
            listOf(entry("1", "Mail", "https://mail.example.com", "me@example.com")),
        )
        assertEquals(1, groups.size)
        assertEquals("mail.example.com", groups.single().label)
        assertEquals(1, groups.single().entries.size)
        assertTrue(groups.single().urlTemplate.startsWith("https://"))
    }

    private fun entry(
        id: String,
        title: String,
        url: String,
        username: String,
    ) = VaultEntry(
        id = id,
        title = title,
        url = url,
        username = username,
        password = "secret-$id",
        notes = "",
        createdAt = 0,
        updatedAt = 0,
    )
}

package com.pwdog.core.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HostMatcherTest {
    @Test
    fun extractsNormalizedHosts() {
        assertEquals("github.com", HostMatcher.hostOf("https://github.com/login"))
        assertEquals("github.com", HostMatcher.hostOf("github.com/login"))
        assertEquals("example.com", HostMatcher.hostOf("http://EXAMPLE.com:8080/path"))
        assertEquals("example.com", HostMatcher.hostOf("https://example.com./path"))
        assertNull(HostMatcher.hostOf(""))
        assertNull(HostMatcher.hostOf("not a url"))
    }

    @Test
    fun matchesOnlyExactHosts() {
        assertTrue(HostMatcher.matches("https://github.com/login", "github.com"))
        assertTrue(HostMatcher.matches("github.com", "GITHUB.COM."))
        assertFalse(HostMatcher.matches("https://evilgithub.com/login", "github.com"))
        assertFalse(HostMatcher.matches("https://sub.github.com/login", "github.com"))
        assertFalse(HostMatcher.matches("https://github.com/login", "evilgithub.com"))
    }

    @Test
    fun rejectsInvalidRequestedHosts() {
        assertNull(HostMatcher.normalizeHost(""))
        assertNull(HostMatcher.normalizeHost("example.com/path"))
        assertNull(HostMatcher.normalizeHost("https://example.com"))
        assertNull(HostMatcher.normalizeHost("..example.com"))
        assertNull(HostMatcher.normalizeHost("example..com"))
        assertNull(HostMatcher.normalizeHost("例子.com"))
    }
}

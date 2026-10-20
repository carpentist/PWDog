package com.pwdog.core.entry

import kotlin.test.Test
import kotlin.test.assertEquals

class BrowserPasswordImporterTest {
    @Test
    fun parsesChromeEdgeCsvWithQuotedFields() {
        val csv = """
            name,url,username,password,note
            GitHub,https://github.com,alice,secret,"work, account"
            "Example, Inc.",https://example.com,bob,p@ss,note
        """.trimIndent()

        val preview = BrowserPasswordImporter.parse(csv)

        assertEquals("Chrome / Edge CSV", preview.format)
        assertEquals(2, preview.credentials.size)
        assertEquals("GitHub", preview.credentials[0].title)
        assertEquals("work, account", preview.credentials[0].notes)
        assertEquals("Example, Inc.", preview.credentials[1].title)
        assertEquals("p@ss", preview.credentials[1].password)
    }

    @Test
    fun parsesFirefoxCsvAndDerivesTitleFromHost() {
        val csv = "url,username,password,httpRealm,formActionOrigin,guid,timeCreated,timeLastUsed,timePasswordChanged\n" +
            "https://mail.example.com,me@example.com,secret,,,guid,0,0,0"

        val preview = BrowserPasswordImporter.parse(csv)

        assertEquals("Firefox CSV", preview.format)
        assertEquals(1, preview.credentials.size)
        assertEquals("mail.example.com", preview.credentials[0].title)
        assertEquals("me@example.com", preview.credentials[0].username)
    }

    @Test
    fun parsesBitwardenAndSkipsNonLoginItems() {
        val csv = "folder,favorite,type,name,notes,fields,reprompt,login_uri,login_username,login_password,login_totp\n" +
            ",,login,GitHub,note,,0,https://github.com,alice,secret,\n" +
            ",,note,Secure Note,note,,0,,,,\n"

        val preview = BrowserPasswordImporter.parse(csv)

        assertEquals("Bitwarden CSV", preview.format)
        assertEquals(1, preview.credentials.size)
        assertEquals("GitHub", preview.credentials[0].title)
        assertEquals(1, preview.skippedInvalid)
    }

    @Test
    fun handlesBomCrLfAndDuplicates() {
        val csv = "﻿name,url,username,password,note\r\n" +
            "GitHub,https://github.com,alice,secret,\r\n" +
            "GitHub,https://github.com,alice,secret,\r\n" +
            ",https://example.com,bob,,note\r\n"

        val preview = BrowserPasswordImporter.parse(csv)

        assertEquals(1, preview.credentials.size)
        assertEquals(1, preview.skippedDuplicates)
        assertEquals(1, preview.skippedInvalid)
    }

    @Test
    fun rejectsCsvWithoutPasswordColumn() {
        val csv = "name,url,username\nGitHub,https://github.com,alice"
        val failed = runCatching { BrowserPasswordImporter.parse(csv) }.isFailure
        assertEquals(true, failed)
    }
}

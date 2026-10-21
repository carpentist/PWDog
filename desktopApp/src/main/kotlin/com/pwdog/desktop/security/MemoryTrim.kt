package com.pwdog.desktop.security

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32

object MemoryTrim {
    private interface Psapi : Library {
        fun EmptyWorkingSet(hProcess: Pointer): Boolean

        companion object {
            val INSTANCE: Psapi = Native.load("psapi", Psapi::class.java)
        }
    }

    fun trimWorkingSet(): Boolean {
        if (!Platform.isWindows()) return false
        return runCatching {
            val process = Kernel32.INSTANCE.GetCurrentProcess()
            Psapi.INSTANCE.EmptyWorkingSet(process.pointer)
        }.getOrDefault(false)
    }
}

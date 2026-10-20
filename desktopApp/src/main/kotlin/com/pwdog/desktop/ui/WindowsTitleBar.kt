package com.pwdog.desktop.ui

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.ptr.IntByReference
import java.awt.Window

object WindowsTitleBar {
    private interface DwmApi : Library {
        fun DwmSetWindowAttribute(
            hwnd: Pointer,
            attribute: Int,
            value: Pointer,
            size: Int,
        ): Int

        companion object {
            val INSTANCE: DwmApi = Native.load("dwmapi", DwmApi::class.java)
        }
    }

    fun setDarkMode(dark: Boolean): Boolean {
        if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            return false
        }
        return runCatching {
            val window = Window.getWindows().firstOrNull { it.isShowing }
                ?: return false
            val pointer = Native.getWindowPointer(window) ?: return false
            val hwnd = HWND(pointer)
            val value = IntByReference(if (dark) 1 else 0)

            val modern = DwmApi.INSTANCE.DwmSetWindowAttribute(
                hwnd.pointer,
                20,
                value.pointer,
                4,
            )
            if (modern == 0) return true

            val legacy = DwmApi.INSTANCE.DwmSetWindowAttribute(
                hwnd.pointer,
                19,
                value.pointer,
                4,
            )
            legacy == 0
        }.getOrDefault(false)
    }
}

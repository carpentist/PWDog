package com.pwdog.desktop.ui

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import java.awt.Dimension
import java.awt.Window
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

object PasswordImportPicker {
    private var lastDirectory: File? = null

    fun choose(darkTheme: Boolean): File? {
        installLookAndFeel(darkTheme)

        val chooser = JFileChooser().apply {
            dialogTitle = "选择浏览器密码导出文件"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter(
                "CSV / 文本文件 (*.csv, *.txt)",
                "csv",
                "txt",
            )
            preferredSize = Dimension(820, 540)
            currentDirectory = lastDirectory ?: defaultDirectory()
        }

        val parent = Window.getWindows().firstOrNull { it.isActive && it.isShowing }
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null

        val file = chooser.selectedFile ?: return null
        lastDirectory = file.parentFile
        return file
    }

    private fun installLookAndFeel(darkTheme: Boolean) {
        runCatching {
            if (darkTheme) {
                FlatDarkLaf.setup()
            } else {
                FlatLightLaf.setup()
            }
        }
    }

    private fun defaultDirectory(): File {
        val home = File(System.getProperty("user.home"))
        return listOf(
            File(home, "Downloads"),
            File(home, "Desktop"),
            home,
        ).firstOrNull { it.isDirectory } ?: home
    }
}

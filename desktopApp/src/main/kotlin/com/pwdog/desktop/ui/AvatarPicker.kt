package com.pwdog.desktop.ui

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Window
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.min

object AvatarPicker {
    private const val MAX_INPUT_BYTES = 8L * 1024 * 1024
    private const val MAX_OUTPUT_BYTES = 512 * 1024
    private const val SIZE = 256
    private const val PREVIEW_SIZE = 300

    private var lastDirectory: File? = null

    fun choose(darkTheme: Boolean = false): ByteArray? {
        installLookAndFeel(darkTheme)

        val chooser = JFileChooser().apply {
            dialogTitle = "选择头像图片"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter(
                "图片 (*.png, *.jpg, *.jpeg, *.gif, *.bmp)",
                "png",
                "jpg",
                "jpeg",
                "gif",
                "bmp",
            )
            preferredSize = Dimension(920, 600)
            currentDirectory = lastDirectory ?: defaultPicturesDirectory()
        }

        val imagePreview = JLabel("选择图片后显示预览", SwingConstants.CENTER).apply {
            preferredSize = Dimension(PREVIEW_SIZE + 20, PREVIEW_SIZE + 20)
            isOpaque = true
            background = UIManager.getColor("Panel.background") ?: Color(0xF2F2F2)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
        val fileInfo = JLabel(" ").apply {
            border = BorderFactory.createEmptyBorder(0, 12, 8, 12)
        }
        val previewPanel = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(350, 420)
            border = BorderFactory.createTitledBorder("预览")
            add(imagePreview, BorderLayout.CENTER)
            add(fileInfo, BorderLayout.SOUTH)
        }
        chooser.accessory = previewPanel

        fun updatePreview(file: File?) {
            imagePreview.icon = null
            if (file == null || !file.isFile) {
                imagePreview.text = "选择图片后显示预览"
                fileInfo.text = " "
                return
            }
            runCatching {
                val source = ImageIO.read(file)
                    ?: throw IllegalArgumentException("不是可识别的图片")
                val scale = min(
                    PREVIEW_SIZE.toDouble() / source.width,
                    PREVIEW_SIZE.toDouble() / source.height,
                )
                val width = (source.width * scale).toInt().coerceAtLeast(1)
                val height = (source.height * scale).toInt().coerceAtLeast(1)
                val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                val graphics: Graphics2D = scaled.createGraphics()
                try {
                    graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                    )
                    graphics.setRenderingHint(
                        RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY,
                    )
                    graphics.drawImage(source, 0, 0, width, height, null)
                } finally {
                    graphics.dispose()
                }
                imagePreview.text = null
                imagePreview.icon = ImageIcon(scaled)
                fileInfo.text = "<html>${file.name}<br>" +
                    "${source.width} × ${source.height} · ${file.length() / 1024} KB</html>"
            }.onFailure {
                imagePreview.text = "无法预览此图片"
                fileInfo.text = file.name
            }
        }

        updatePreview(chooser.selectedFile)
        chooser.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY) {
            updatePreview(chooser.selectedFile)
        }

        val parent = Window.getWindows().firstOrNull { it.isActive && it.isShowing }
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null

        val file = chooser.selectedFile ?: return null
        lastDirectory = file.parentFile
        return normalize(file)
    }

    fun normalize(file: File): ByteArray {
        require(file.length() <= MAX_INPUT_BYTES) { "图片不能超过 8 MB" }
        val source = ImageIO.read(file) ?: throw IllegalArgumentException("无法读取图片")
        val output = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB)
        val graphics: Graphics2D = output.createGraphics()
        try {
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR,
            )
            graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY,
            )
            val scale = maxOf(
                SIZE.toDouble() / source.width,
                SIZE.toDouble() / source.height,
            )
            val width = (source.width * scale).toInt()
            val height = (source.height * scale).toInt()
            graphics.drawImage(
                source,
                (SIZE - width) / 2,
                (SIZE - height) / 2,
                width,
                height,
                null,
            )
        } finally {
            graphics.dispose()
        }

        val bytes = ByteArrayOutputStream().use { stream ->
            check(ImageIO.write(output, "png", stream)) { "图片编码失败" }
            stream.toByteArray()
        }
        require(bytes.size <= MAX_OUTPUT_BYTES) { "头像处理后仍然过大" }
        return bytes
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

    private fun defaultPicturesDirectory(): File {
        val home = File(System.getProperty("user.home"))
        return listOf(File(home, "Pictures"), home)
            .firstOrNull { it.isDirectory }
            ?: home
    }
}

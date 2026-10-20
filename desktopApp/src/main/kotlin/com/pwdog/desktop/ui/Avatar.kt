package com.pwdog.desktop.ui

import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pwdog.core.account.Account
import org.jetbrains.skia.Image as SkiaImage

val AVATAR_COLORS: List<Int> = listOf(
    0xFF6A4B8C.toInt(),
    0xFF2563EB.toInt(),
    0xFF2E7D32.toInt(),
    0xFFB45309.toInt(),
    0xFFB3261E.toInt(),
    0xFF0F766E.toInt(),
)

fun defaultAvatarColor(seed: String): Int =
    AVATAR_COLORS[(seed.hashCode() and Int.MAX_VALUE) % AVATAR_COLORS.size]

@Composable
fun AccountAvatar(account: Account, size: Dp = 44.dp) {
    AvatarView(
        imageBytes = account.avatarImage,
        color = account.avatarColor ?: defaultAvatarColor(account.id),
        name = account.displayName,
        size = size,
    )
}

@Composable
fun AvatarView(
    imageBytes: ByteArray?,
    color: Int,
    name: String,
    size: Dp,
) {
    val bitmap = remember(imageBytes) {
        imageBytes?.let { bytes ->
            runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            ComposeImage(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = name.trim().take(1).ifBlank { "?" }.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

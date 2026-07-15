package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun AdOverlay(
    isVisible: Boolean,
    bannerUrl: String,
    redirectUrl: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (isVisible) {
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Catch clicks to avoid closing unless X is clicked */ },
                contentAlignment = Alignment.Center
            ) {
                // Central Ad Dialog Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("ad_overlay_card"),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header showing "Sponsored" / "تبلیغات" in Persian and close button
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .testTag("ad_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "بستن",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = "تبلیغ ویژه",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Advertisement Banner Area
                        var isImageLoading by remember { mutableStateOf(true) }
                        var isImageError by remember { mutableStateOf(false) }

                        val cleanBannerUrl = remember(bannerUrl) {
                            val trimmed = bannerUrl.trim()
                            if (trimmed.isNotBlank() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                                "https://$trimmed"
                            } else {
                                trimmed
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    )
                                )
                                .clickable {
                                    openUrlInBrowser(context, redirectUrl)
                                }
                                .testTag("ad_banner_image_click"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Render fallbacks if URL is invalid or empty
                            if (cleanBannerUrl.isBlank() || isImageError) {
                                // Premium vector programmatic gradient ad fallback
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Launch,
                                            contentDescription = "تبلیغ",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "بهترین ابزارهای مدیریت PDF",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 15.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "جهت کسب اطلاعات بیشتر کلیک کنید",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(cleanBannerUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "بنر تبلیغاتی",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    onLoading = { isImageLoading = true },
                                    onSuccess = {
                                        isImageLoading = false
                                        isImageError = false
                                    },
                                    onError = {
                                        isImageLoading = false
                                        isImageError = true
                                    }
                                )

                                if (isImageLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Call to Action Text & Button
                        Text(
                            text = "با کلیک بر روی دکمه زیر وارد سایت اسپانسر شوید",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                openUrlInBrowser(context, redirectUrl)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("ad_cta_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = "مشاهده وبسایت"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ورود به لینک تبلیغ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun openUrlInBrowser(context: android.content.Context, url: String) {
    try {
        var cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return

        // 1. Determine if it is a Telegram link or username
        var isTelegram = false
        var telegramUsername: String? = null

        if (cleanUrl.startsWith("@")) {
            isTelegram = true
            telegramUsername = cleanUrl.substring(1)
        } else if (cleanUrl.contains("t.me/") || cleanUrl.contains("telegram.me/")) {
            isTelegram = true
            // Extract the username/path after t.me/ or telegram.me/
            val index = if (cleanUrl.contains("t.me/")) cleanUrl.indexOf("t.me/") + 5 else cleanUrl.indexOf("telegram.me/") + 12
            val path = cleanUrl.substring(index)
            // Get everything before the first '?' if there is one
            val queryIndex = path.indexOf('?')
            telegramUsername = if (queryIndex != -1) path.substring(0, queryIndex) else path
            // Trim trailing slashes
            if (telegramUsername.endsWith("/")) {
                telegramUsername = telegramUsername.dropLast(1)
            }
        } else if (!cleanUrl.contains(".") && !cleanUrl.contains("/") && !cleanUrl.contains(":")) {
            // It's a plain username/bot
            isTelegram = true
            telegramUsername = cleanUrl
        } else if (cleanUrl.startsWith("tg://")) {
            isTelegram = true
            // e.g. tg://resolve?domain=username
            val uri = Uri.parse(cleanUrl)
            telegramUsername = uri.getQueryParameter("domain")
        }

        if (isTelegram && !telegramUsername.isNullOrBlank()) {
            try {
                // Try to open with official Telegram app URI scheme
                val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$telegramUsername")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(tgIntent)
            } catch (e: Exception) {
                // If Telegram app is not installed, open via web browser
                val webUrl = "https://t.me/$telegramUsername"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        } else {
            // Standard URL opening
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

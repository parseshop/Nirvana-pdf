package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_pdfs")
data class RecentPdf(
    @PrimaryKey val uri: String,
    val displayName: String,
    val size: String,
    val lastOpened: Long = System.currentTimeMillis()
)

@Entity(tableName = "ad_config")
data class AdConfig(
    @PrimaryKey val id: Int = 1,
    val bannerUrl: String,
    val redirectUrl: String,
    val isAdEnabled: Boolean = true,
    val telegramUsername: String = "tg_admin_support",
    val customSyncUrl: String = ""
)

data class InstalledUser(
    val userId: String,
    val deviceModel: String,
    val installTime: String,
    val lastActive: String
)

package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allRecent: Flow<List<RecentPdf>> = appDao.getAllRecentFlow()
    val adConfigFlow: Flow<AdConfig?> = appDao.getAdConfigFlow()

    suspend fun insertRecent(pdf: RecentPdf) {
        appDao.insertRecent(pdf)
    }

    suspend fun deleteRecent(uri: String) {
        appDao.deleteRecentByUri(uri)
    }

    suspend fun clearRecent() {
        appDao.clearAllRecent()
    }

    suspend fun getAdConfig(): AdConfig? {
        return appDao.getAdConfig()
    }

    suspend fun saveAdConfig(config: AdConfig) {
        appDao.insertAdConfig(config)
    }
}

package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Recent PDFs
    @Query("SELECT * FROM recent_pdfs ORDER BY lastOpened DESC")
    fun getAllRecentFlow(): Flow<List<RecentPdf>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(pdf: RecentPdf)

    @Query("DELETE FROM recent_pdfs WHERE uri = :uri")
    suspend fun deleteRecentByUri(uri: String)

    @Query("DELETE FROM recent_pdfs")
    suspend fun clearAllRecent()

    // Ad Configuration
    @Query("SELECT * FROM ad_config WHERE id = 1 LIMIT 1")
    fun getAdConfigFlow(): Flow<AdConfig?>

    @Query("SELECT * FROM ad_config WHERE id = 1 LIMIT 1")
    suspend fun getAdConfig(): AdConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdConfig(config: AdConfig)
}

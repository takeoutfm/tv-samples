/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tv.reference.shared.watchprogress

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Room class and interface for tracking watch progress of videos.
 *
 * This is basically how much of a video the user has watched according to ExoPlayer, which allows
 * users to resume from where they were watching the video previously.
 */
@Entity(tableName = "watch_progress")
data class WatchProgress(
    // A unique identifier for the video
    @PrimaryKey
    @ColumnInfo(name = "video_id") var videoId: String,
    @ColumnInfo(name = "start_position") var startPosition: Long,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "modified_at") var modifiedAt: Long = System.currentTimeMillis()
)

@Dao
interface OldWatchProgressDao {
    @Query("SELECT * FROM watch_progress WHERE video_id = :videoId LIMIT 1")
    fun getWatchProgressByVideoId(videoId: String): LiveData<WatchProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watchProgress: WatchProgress)

    @Query("DELETE FROM watch_progress")
    suspend fun deleteAll()
}

@Dao
abstract class WatchProgressDao {
    @Query("SELECT * FROM watch_progress WHERE video_id = :videoId LIMIT 1")
    abstract fun getWatchProgressByVideoId(videoId: String): LiveData<WatchProgress>

    @Query("SELECT * from watch_progress ORDER BY modified_at DESC")
    abstract fun getRecentWatchProgress(): LiveData<List<WatchProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(watchProgress: WatchProgress)

    suspend fun insertWithTimestamp(watchProgress: WatchProgress) {
        insert(watchProgress.apply{
            createdAt = System.currentTimeMillis()
            modifiedAt = System.currentTimeMillis()
        })
    }

    @Update
    abstract suspend fun update(watchProgress: WatchProgress)

    suspend fun updateWithTimestamp(watchProgress: WatchProgress) {
        update(watchProgress.apply{
            modifiedAt = System.currentTimeMillis()
        })
    }

    @Query("DELETE FROM watch_progress where video_id = :videoId")
    abstract suspend fun deleteWatchProgress(videoId: String)

    @Query("DELETE FROM watch_progress")
    abstract suspend fun deleteAll()
}
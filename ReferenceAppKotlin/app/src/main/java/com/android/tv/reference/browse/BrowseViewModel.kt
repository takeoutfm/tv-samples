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
package com.android.tv.reference.browse

import android.app.Application
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.android.tv.reference.auth.UserManager
import com.android.tv.reference.repository.VideoRepository
import com.android.tv.reference.repository.VideoRepositoryFactory
import com.android.tv.reference.shared.datamodel.Progress
import com.android.tv.reference.shared.datamodel.VideoGroup
import com.android.tv.reference.shared.watchprogress.WatchProgress
import com.android.tv.reference.shared.watchprogress.WatchProgressDatabase
import com.android.tv.reference.shared.watchprogress.WatchProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrowseViewModel(application: Application) : AndroidViewModel(application) {
    private val videoRepository = VideoRepositoryFactory.getVideoRepository()
    private val userManager = UserManager.getInstance(application.applicationContext)
    private val watchProgressDatabase = WatchProgressDatabase.getDatabase(application)
    val browseContent = MutableLiveData<List<VideoGroup>>()
    val customMenuItems = MutableLiveData<List<BrowseCustomMenu>>(listOf())
    val isSignedIn = Transformations.map(userManager.userInfo) { it != null }
    val watchProgress = watchProgressDatabase.watchProgressDao().getRecentWatchProgress()

    fun refresh() {
        viewModelScope.launch {
            asyncRefresh()
            pushWatchProgress()
        }
    }

    private suspend fun asyncRefresh() {
        val groupList = getVideoGroupList(videoRepository)
        browseContent.value = groupList
    }

    suspend fun getVideoGroupList(repository: VideoRepository): List<VideoGroup> {
        val videosByCategory = repository.getAllVideos(refresh = true).groupBy { it.category }
        val videoGroupList = mutableListOf<VideoGroup>()
        videoGroupList.addAll(repository.getHomeGroups())
        videosByCategory.forEach { (k, v) ->
            videoGroupList.add(VideoGroup(k, v))
        }
        return videoGroupList
    }

    fun signOut() = viewModelScope.launch(Dispatchers.IO) {
        userManager.signOut()
    }

    private fun pushWatchProgress() {
        watchProgress.observeForever(object : Observer<List<WatchProgress>?> {
            override fun onChanged(list: List<WatchProgress>?) {
                if (list != null) {
                    watchProgress.removeObserver(this)
                    viewModelScope.launch {
                        updateWatchProgress(list);
                        syncWatchProgress()
                    }
                }
            }
        })
    }

    private suspend fun updateWatchProgress(watchList: List<WatchProgress>) {
        val videoRepository = VideoRepositoryFactory.getVideoRepository()
        val update = mutableListOf<Progress>()
        watchList.forEach { progress ->
            update.add(
                Progress(
                    id = progress.videoId,
                    position = progress.startPosition,
                    timestamp = progress.modifiedAt,
                )
            )
        }
        if (update.isNotEmpty()) {
            videoRepository.updateProgress(update)
        }
    }

    private suspend fun syncWatchProgress() {
        val videoRepository = VideoRepositoryFactory.getVideoRepository()
        val watchProgressRepository =
            WatchProgressRepository(watchProgressDatabase.watchProgressDao())
        val progress = videoRepository.getProgress()
        progress.forEach { p ->
            val video = videoRepository.getVideoById(p.id)
            video?.let {
                watchProgressRepository.insert(
                    WatchProgress(
                        videoId = p.id,
                        startPosition = p.position,
                        modifiedAt = p.timestamp
                    )
                )
            }
        }
    }
}


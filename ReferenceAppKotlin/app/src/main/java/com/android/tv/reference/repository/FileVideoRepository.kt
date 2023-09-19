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
package com.android.tv.reference.repository

import android.app.Application
import com.android.tv.reference.shared.datamodel.*
import com.takeoutfm.tv.R

/**
 * VideoRepository implementation to read video data from a file saved on /res/raw
 */
class FileVideoRepository(override val application: Application) : VideoRepository {
    // Underscore name to allow lazy loading since "getAllVideos" matches the getter name otherwise
    private val _allVideos: List<Video> by lazy {
        val jsonString = readJsonFromFile()
        VideoParser.loadVideosFromJson(jsonString)
    }

    private fun readJsonFromFile(): String {
        val inputStream = application.resources.openRawResource(R.raw.api)
        return inputStream.bufferedReader().use {
            it.readText()
        }
    }

    override suspend fun getAllVideos(refresh: Boolean): List<Video> {
        return _allVideos
    }

    override suspend fun getHomeGroups(): List<VideoGroup> {
        TODO("Not yet implemented")
    }

    override suspend fun getVideoDetail(id: String): Detail? {
        TODO("Not yet implemented")
    }

    override suspend fun getProfile(id: String): Profile? {
        TODO("Not yet implemented")
    }

    override suspend fun search(query: String): List<Video> {
        TODO("Not yet implemented")
    }

    override fun getVideoById(id: String): Video? {
        val jsonString = readJsonFromFile()
        return VideoParser.findVideoFromJson(jsonString, id)
    }

    override fun getVideoByVideoUri(uri: String): Video? {
        return _allVideos
            .firstOrNull { it.videoUri == uri }
    }

    override fun getAllVideosFromSeries(seriesUri: String): List<Video> {
        return _allVideos.filter { it.seriesUri == seriesUri }
    }

    override suspend fun updateProgress(progress: List<Progress>): Int {
        TODO("Not yet implemented")
    }

    override fun getVideoProgress(video: Video): Progress? {
        TODO("Not yet implemented")
    }

    override suspend fun getProgress(): List<Progress> {
        TODO("Not yet implemented")
    }
}

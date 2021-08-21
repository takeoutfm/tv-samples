// Copyright (C) 2021 The Takeout Authors.
//
// This file is part of Takeout.
//
// Takeout is free software: you can redistribute it and/or modify it under the
// terms of the GNU Affero General Public License as published by the Free
// Software Foundation, either version 3 of the License, or (at your option)
// any later version.
//
// Takeout is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for
// more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with Takeout.  If not, see <https://www.gnu.org/licenses/>.

package com.defsub.takeout.repository

import android.app.Application
import com.android.tv.reference.auth.UserManager
import com.android.tv.reference.repository.VideoRepository
import com.android.tv.reference.shared.datamodel.Video
import com.android.tv.reference.shared.datamodel.VideoType
import com.defsub.takeout.client.Client
import com.defsub.takeout.client.location
import com.defsub.takeout.client.year
import kotlinx.coroutines.runBlocking

/**
 * VideoRepository implementation to read video data from a file saved on /res/raw
 */
class TakeoutVideoRepository(override val application: Application) : VideoRepository {

    private fun load(): List<Video> {
        val userManager = UserManager.getInstance(application.applicationContext)
        if (!userManager.isSignedIn()) {
            return emptyList()
        }
        val userInfo = userManager.userInfo.value ?: return emptyList()
        val videos = mutableListOf<Video>()
        runBlocking {
            val client = Client(userInfo.endpoint, userInfo.token)
            val movies = client.movies(0)
            for (m in movies.movies) {
                val posterUrl = "http://image.tmdb.org/t/p/w342/${m.posterPath}"
                val backdropUrl = "http://image.tmdb.org/t/p/w1280/${m.backdropPath}"
                val locationUrl = "${userInfo.endpoint}${m.location()}"
                val category = "${m.sortTitle[0]}"
                val vote = if (m.voteAverage == null) 0 else (m.voteAverage * 10).toInt()
                val v = Video(
                    id = m.id.toString(),
                    name = m.title,
                    description = m.overview,
                    uri = locationUrl,
                    videoUri = locationUrl,
                    thumbnailUri = posterUrl,
                    backgroundImageUri = backdropUrl,
                    category = category,
                    videoType = VideoType.MOVIE,
                    vote = vote,
                    year = m.year(),
                    rating = m.rating,
                    tagline = m.tagline,
                    headers = mapOf("Cookie" to "Takeout=${userInfo.token}")
                )
                videos.add(v)
            }
            client.close()
        }
        return videos
    }

    override fun getAllVideos(): List<Video> {
        return load()
    }

    override fun getVideoById(id: String): Video? {
        for (v in load()) {
            if (v.name == id) {
                return v
            }
        }
        return null
    }

    override fun getVideoByVideoUri(uri: String): Video? {
        return getAllVideos()
            .firstOrNull { it.videoUri == uri }
    }

    override fun getAllVideosFromSeries(seriesUri: String): List<Video> {
        return getAllVideos().filter { it.seriesUri == seriesUri }
    }
}

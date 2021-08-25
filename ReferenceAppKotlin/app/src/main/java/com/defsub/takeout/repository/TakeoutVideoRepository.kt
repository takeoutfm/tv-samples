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
import com.android.tv.reference.auth.UserInfo
import com.android.tv.reference.auth.UserManager
import com.android.tv.reference.repository.VideoRepository
import com.android.tv.reference.shared.datamodel.*
import com.defsub.takeout.client.Client
import com.defsub.takeout.client.Movie
import com.defsub.takeout.client.location
import com.defsub.takeout.client.year
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * VideoRepository implementation to read video data from a file saved on /res/raw
 */
class TakeoutVideoRepository(override val application: Application) : VideoRepository {

    private var userInfo: UserInfo? = null
    private var client: Client? = null

    private var allVideos = mutableListOf<Video>()
    private var newVideos = mutableListOf<Video>()
    private var addedVideos = mutableListOf<Video>()

    private fun checkSignedIn(): Boolean {
        val userManager = UserManager.getInstance(application.applicationContext)
        val signedIn = userManager.isSignedIn()
        if (signedIn) {
            if (userInfo == null || client == null) {
                val user = userManager.userInfo.value
                user?.let {
                    userInfo = it
                    client = Client(it.endpoint, it.token)
                }
            }
        } else {
            userInfo = null
            client = null
            allVideos.clear()
            newVideos.clear()
            addedVideos.clear()
        }
        return signedIn
    }

    private fun load() {
        if (!checkSignedIn()) return

        if (allVideos.isNotEmpty()) {
            return
        }

        runBlocking {
            val videos = mutableListOf<Video>()
            val movies = client!!.movies(0)
            for (m in movies.movies) {
                videos.add(toVideo(m))
            }
            allVideos.addAll(videos)
            // also load home for new and added
            home()
        }
    }

    private suspend fun home() {
        val view = client!!.home(0)
        newVideos.clear()
        addedVideos.clear()
        view.newMovies.forEach {
            newVideos.add(toVideo(it))
        }
        view.addedMovies.forEach {
            addedVideos.add(toVideo(it))
        }
    }

    override fun getNewReleases(): List<Video> {
        load()
        return newVideos
    }

    override fun getRecentlyAdded(): List<Video> {
        load()
        return addedVideos
    }

    override fun getAllVideos(): List<Video> {
        load()
        return allVideos
    }

    override fun getVideoDetail(id: String): Detail? {
        if (!checkSignedIn()) return null
        var detail: Detail? = null
        runBlocking {
            val view = client!!.movie(id.toInt(), 0)
            detail = toDetail(view)
        }
        return detail
    }

    override fun getProfile(id: String): Profile? {
        if (!checkSignedIn()) return null
        var profile: Profile? = null
        runBlocking {
            val view = client!!.profile(id.toInt(), 0)
            profile = toProfile(view)
        }
        return profile
    }

    override fun search(query: String): List<Video> {
        if (!checkSignedIn()) return emptyList()
        val results: List<Video>
        runBlocking {
            val result = client!!.search(query)
            results = result.movies?.map { toVideo(it) } ?: emptyList()
        }
        return results
    }

    override fun getVideoById(id: String): Video? {
        for (v in getAllVideos()) {
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

    private fun toProfile(p: com.defsub.takeout.client.ProfileView): Profile {
        return Profile(
            id = p.person.id.toString(),
            person = toPerson(p.person),
            videos = p.starring?.map { toVideo(it) } ?: emptyList()
        )
    }

    private fun toPerson(p: com.defsub.takeout.client.Person): Person {
        return Person(
            id = p.id.toString(),
            name = p.name,
            bio = p.bio ?: "",
            birthplace = p.birthplace ?: "",
            birthday = p.birthday ?: "",
            thumbnailUri = "http://image.tmdb.org/t/p/w185/${p.profilePath}"
        )
    }

    private fun toCast(c: com.defsub.takeout.client.Cast): Cast {
        return Cast(
            id = c.id.toString(),
            person = toPerson(c.person),
            character = c.character
        )
    }

    private fun toDetail(view: com.defsub.takeout.client.MovieView): Detail {
        return Detail(
            id = view.movie.id.toString(),
            video = toVideo(view.movie),
            genres = view.genres ?: emptyList(),
            cast = view.cast?.map { toCast(it) } ?: emptyList(),
            related = view.other?.map { toVideo(it) } ?: emptyList(),
        )
    }

    private fun toVideo(m: Movie): Video {
        val userInfo = userInfo!!
        val posterUrl = "http://image.tmdb.org/t/p/w342/${m.posterPath}"
        val backdropUrl = "http://image.tmdb.org/t/p/w1280/${m.backdropPath}"
        val locationUrl = "${userInfo.endpoint}${m.location()}"
        val category = "${m.sortTitle[0]}"
        val vote = if (m.voteAverage == null) 0 else (m.voteAverage * 10).toInt()
        return Video(
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
    }

}

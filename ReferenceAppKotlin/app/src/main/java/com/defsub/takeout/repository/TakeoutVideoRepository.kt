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
import android.net.Uri
import com.android.tv.reference.auth.UserInfo
import com.android.tv.reference.auth.UserManager
import com.android.tv.reference.repository.VideoRepository
import com.android.tv.reference.shared.datamodel.*
import com.defsub.takeout.client.Client
import com.defsub.takeout.client.Movie
import com.defsub.takeout.client.location
import com.defsub.takeout.client.year
import kotlinx.coroutines.*
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
    private var recommendVideos = mutableListOf<VideoGroup>()

    private suspend fun signOut() {
        val userManager = UserManager.getInstance(application.applicationContext)
        userManager.signOut()
    }

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
            recommendVideos.clear()
        }
        return signedIn
    }

    private suspend fun load() {
        Timber.d("loading...")
        if (!checkSignedIn()) return

        if (allVideos.isNotEmpty()) {
            Timber.d("already loaded")
            return
        }

        try {
            Timber.d("loading movies")
            val movies = client!!.movies(0)
            allVideos.clear()
            allVideos.addAll(movies.movies.map { toVideo(it) })
            Timber.d("loading movies..done")
            // also load home for new and added
            home()
        } catch (e: Exception) {
            Timber.e(e)
            signOut()
        }
    }

    private suspend fun home() {
        val view = client!!.home(0)
        newVideos.clear()
        addedVideos.clear()
        recommendVideos.clear()
        view.newMovies.forEach {
            newVideos.add(toVideo(it))
        }
        view.addedMovies.forEach {
            addedVideos.add(toVideo(it))
        }
        view.recommendMovies.forEach { recommend ->
            val videos = recommend.movies.map { m -> toVideo(m) }
            recommendVideos.add(VideoGroup(category = recommend.name, videoList = videos))
        }
    }

    override suspend fun getHomeGroups(): List<VideoGroup> {
        load()
        val groups = mutableListOf<VideoGroup>()
        if (recommendVideos.isNotEmpty()) {
            groups.addAll(recommendVideos)
        }
        if (newVideos.isNotEmpty()) {
            groups.add(VideoGroup(category = "New Releases", newVideos))
        }
        if (addedVideos.isNotEmpty()) {
            groups.add(VideoGroup(category = "Recently Added", addedVideos))
        }
        return groups
    }

    override suspend fun getAllVideos(): List<Video> {
        load()
        return allVideos
    }

    private val idPattern = Regex("""/([0-9]+)/?""")

    private fun getId(uri: String): Int {
        val values = idPattern.find(uri)?.groupValues ?: emptyList()
        return if (values.isNotEmpty()) values[1].toInt() else -1
    }

    // takeout://movies/$id
    override suspend fun getVideoDetail(id: String): Detail? {
        if (!checkSignedIn()) return null
        var detail: Detail? = null
        val view = client!!.movie(getId(id), 0)
        detail = toDetail(view)
        return detail
    }

    override suspend fun getProfile(id: String): Profile? {
        if (!checkSignedIn()) return null
        var profile: Profile? = null
        val view = client!!.profile(getId(id), 0)
        profile = toProfile(view)
        return profile
    }

    override suspend fun search(query: String): List<Video> {
        if (!checkSignedIn()) return emptyList()
        val results: List<Video>
        val result = client!!.search(query)
        results = result.movies?.map { toVideo(it) } ?: emptyList()
        return results
    }

    override fun getVideoById(id: String): Video? {
        return allVideos.firstOrNull { it.id == id }
    }

    override fun getVideoByVideoUri(uri: String): Video? {
        return allVideos
            .firstOrNull { it.videoUri == uri }
    }

    override fun getAllVideosFromSeries(seriesUri: String): List<Video> {
        return allVideos.filter { it.seriesUri == seriesUri }
    }

    private fun toProfile(p: com.defsub.takeout.client.ProfileView): Profile {
        return Profile(
            id = "takeout://people/${p.person.id}/profile",
            person = toPerson(p.person),
            videos = p.starring?.map { toVideo(it) } ?: emptyList()
        )
    }

    private fun toPerson(p: com.defsub.takeout.client.Person): Person {
        return Person(
            id = "takeout://people/${p.id}",
            name = p.name,
            bio = p.bio ?: "",
            birthplace = p.birthplace ?: "",
            birthday = p.year(),
            thumbnailUri = "http://image.tmdb.org/t/p/w185/${p.profilePath}"
        )
    }

    private fun toCast(c: com.defsub.takeout.client.Cast): Cast {
        return Cast(
            id = "takeout://cast/${c.id}",
            person = toPerson(c.person),
            character = c.character
        )
    }

    private fun toDetail(view: com.defsub.takeout.client.MovieView): Detail {
        return Detail(
            id = "takeout://movies/${view.movie.id}/detail",
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
        val uri = "takeout://movies/${m.id}"
        return Video(
            id = uri,
            name = m.title,
            description = m.overview,
            uri = uri,
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

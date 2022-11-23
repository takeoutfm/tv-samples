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
import com.android.tv.reference.shared.datamodel.Cast
import com.android.tv.reference.shared.datamodel.Person
import com.defsub.takeout.client.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * VideoRepository implementation to read video data from a file saved on /res/raw
 */
class TakeoutVideoRepository(override val application: Application) : VideoRepository,
    Client.Listener {

    private var userInfo: UserInfo? = null
    private var client: Client? = null

    private var allVideos = mutableListOf<Video>()
    private var newVideos = mutableListOf<Video>()
    private var addedVideos = mutableListOf<Video>()
    private var recommendVideos = mutableListOf<VideoGroup>()
    private var progressMap = mutableMapOf<String, Offset>()
    private var etags = mutableMapOf<String, Video>()

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
                    client =
                        Client(
                            endpoint = it.endpoint,
                            tokens = Tokens(
                                accessToken = it.accessToken,
                                mediaToken = it.mediaToken,
                                refreshToken = it.refreshToken
                            )
                        )
                    client?.addListener(this)
                }
            }
        } else {
            userInfo = null
            client = null
            clearCache()
        }
        return signedIn
    }

    override fun onTokens(tokens: Tokens?) {
        val userManager = UserManager.getInstance(application.applicationContext)
        if (tokens != null) {
            val user = UserInfo(
                accessToken = tokens.accessToken,
                mediaToken = tokens.mediaToken,
                refreshToken = tokens.refreshToken,
                endpoint = userInfo?.endpoint ?: "",
                displayName = userInfo?.displayName ?: ""
            )
            userManager.updateUserInfo(user)
            userInfo = user
        } else {
            runBlocking {
                userManager.signOut()
            }
        }
    }

    private fun clearCache() {
        allVideos.clear()
        newVideos.clear()
        addedVideos.clear()
        recommendVideos.clear()
        progressMap.clear()
    }

    private suspend fun load() {
        Timber.d("loading...")
        if (!checkSignedIn()) {
            Timber.d("user not signed in...")
            return
        }

        if (allVideos.isNotEmpty()) {
            Timber.d("already loaded")
            return
        }

        try {
            Timber.d("loading movies")
            val movies = client!!.movies(0)
            allVideos.clear()
            allVideos.addAll(movies.movies.map { toVideo(it) })
            etags.clear()
            allVideos.forEach {
                etags[it.etag] = it
            }
            Timber.d("loading movies..done")
            // also load home for new and added
            home()
            progress()
        } catch (e: Exception) {
            Timber.e(e, "load error XXX")
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
        view.recommendMovies?.forEach { recommend ->
            val videos = recommend.movies.map { m -> toVideo(m) }
            recommendVideos.add(VideoGroup(category = recommend.name, videoList = videos))
        }
    }

    private suspend fun progress() {
        val view = client!!.progress(0)
        progressMap.clear()
        view.offsets.forEach {
            progressMap[it.etag] = it
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

    override suspend fun getAllVideos(refresh: Boolean): List<Video> {
        if (refresh) {
            clearCache()
        }
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
        val view = client!!.movie(getId(id), 0)
        return toDetail(view)
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
        // allow lookup by id or etag
        return allVideos.firstOrNull { it.id == id } ?: etags[id]
    }

    override fun getVideoByVideoUri(uri: String): Video? {
        return allVideos
            .firstOrNull { it.videoUri == uri }
    }

    override fun getAllVideosFromSeries(seriesUri: String): List<Video> {
        return allVideos.filter { it.seriesUri == seriesUri }
    }

    override suspend fun updateProgress(progress: List<Progress>): Int {
        val client = client!!

        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        df.timeZone = TimeZone.getTimeZone("UTC")

        val list = mutableListOf<Offset>()
        progress.forEach { progress ->
            val video = getVideoById(progress.id)
            if (video != null) {
                val date = df.format(Date(progress.timestamp))
                val offset = Offset(
                    etag = video.etag,
                    offset = (progress.position / 1000).toInt(),
                    duration = (progress.duration?.div(1000))?.toInt(),
                    date = date
                )
//                Timber.d("updateProgress ${offset.etag} ${offset.date} ${Date(progress.timestamp)}")
                progressMap[offset.etag] = offset
                list.add(offset)
            }
        }
        if (list.isEmpty()) {
            return 0
        }
        return client.updateProgress(Offsets(offsets = list))
    }

    override fun getVideoProgress(video: Video): Progress? {
        val offset = progressMap[video.etag]
        if (offset != null) {
            return toProgress(video, offset)
        }
        return null
    }

    override suspend fun getProgress(): List<Progress> {
        progress()
        val list = mutableListOf<Progress>()
        progressMap.values.forEach {
            val video = etags[it.etag]
            if (video != null) {
                list.add(toProgress(video, it))
            }
        }
        return list
    }

    private fun toProfile(p: com.defsub.takeout.client.ProfileView): Profile {
        return Profile(
            id = "takeout://people/${p.person.id}/profile",
            person = toPerson(p.person),
            videos = p.starring?.map { toVideo(it) } ?: emptyList()
        )
    }

    private fun toPerson(p: com.defsub.takeout.client.Person): Person {
        val endpoint = userInfo?.endpoint ?: ""
        return Person(
            id = "takeout://people/${p.id}",
            name = p.name,
            bio = p.bio ?: "",
            birthplace = p.birthplace ?: "",
            birthday = p.year(),
            thumbnailUri = "$endpoint/img/tm/w185${p.profilePath}"
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
            uri = "${client!!.endpoint()}${view.location}",
            headers = mapOf(
                HttpHeaders.Authorization to "Bearer ${userInfo?.mediaToken}",
                HttpHeaders.UserAgent to client!!.userAgent()
            ),
            video = toVideo(view.movie),
            genres = view.genres ?: emptyList(),
            cast = view.cast?.map { toCast(it) } ?: emptyList(),
            related = view.other?.map { toVideo(it) } ?: emptyList(),
        )
    }

    private fun category(title: String): String {
        val ch = title[0]
        return if (ch.isDigit()) {
            // all numeric will be category #
            "#"
        } else {
            ch.uppercase();
        }
    }

    private fun toVideo(m: Movie): Video {
        val endpoint = userInfo?.endpoint ?: ""
        val posterUrl = "$endpoint/img/tm/w342${m.posterPath}"
        val backdropUrl = "$endpoint/img/tm/w1280${m.backdropPath}"
        val category = category(m.sortTitle)
        val vote = if (m.voteAverage == null) 0 else (m.voteAverage * 10).toInt()
        val uri = "takeout://movies/${m.id}"
        return Video(
            id = uri,
            name = m.title,
            description = m.overview,
            uri = uri,
            videoUri = "", // get from detail
            thumbnailUri = posterUrl,
            backgroundImageUri = backdropUrl,
            category = category,
            videoType = VideoType.MOVIE,
            vote = vote,
            year = m.year(),
            rating = m.rating,
            duration = m.iso8601(),
            tagline = m.tagline,
            etag = m.key()
        )
    }

    private fun toProgress(video: Video, offset: Offset): Progress {
        // TODO optimize this
        var date: Date? = null
        try {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            df.timeZone = TimeZone.getTimeZone("UTC")
            date = df.parse(offset.date)
        } catch (e: ParseException) {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            df.timeZone = TimeZone.getTimeZone("UTC")
            date = df.parse(offset.date)
        }
        return Progress(
            id = video.id,
            position = offset.offset * 1000L,
            timestamp = date?.time ?: 0
        )
    }

}

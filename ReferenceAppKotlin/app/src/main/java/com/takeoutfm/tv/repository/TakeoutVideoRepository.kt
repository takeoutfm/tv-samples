// Copyright (C) 2021 defsub
//
// This file is part of TakeoutFM.
//
// TakeoutFM is free software: you can redistribute it and/or modify it under the
// terms of the GNU Affero General Public License as published by the Free
// Software Foundation, either version 3 of the License, or (at your option)
// any later version.
//
// TakeoutFM is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for
// more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with TakeoutFM.  If not, see <https://www.gnu.org/licenses/>.

package com.takeoutfm.tv.repository

import android.app.Application
import com.android.tv.reference.auth.UserInfo
import com.android.tv.reference.auth.UserManager
import com.android.tv.reference.repository.VideoRepository
import com.android.tv.reference.shared.datamodel.*
import com.android.tv.reference.shared.datamodel.Cast
import com.android.tv.reference.shared.datamodel.Person
import com.takeoutfm.tv.client.*
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

    private var allMovies = mutableListOf<Video>()
    private var newMovies = mutableListOf<Video>()
    private var addedMovies = mutableListOf<Video>()
    private var recommendMovies = mutableListOf<VideoGroup>()
    private var progressMap = mutableMapOf<String, Offset>()
    private var etagMap = mutableMapOf<String, Video>()
    private var allSeries = mutableListOf<Series>()
    private var allEpisodes = mutableListOf<Video>()

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

    override fun onAccessCode(accessCode: AccessCode?) {
        TODO("Not yet implemented")
    }

    private fun clearCache() {
        allMovies.clear()
        newMovies.clear()
        addedMovies.clear()
        recommendMovies.clear()
        progressMap.clear()
        allSeries.clear()
        allEpisodes.clear()
        etagMap.clear()
    }

    private suspend fun load() {
        Timber.d("loading...")
        if (!checkSignedIn()) {
            Timber.d("user not signed in...")
            return
        }

        if (etagMap.isNotEmpty()) {
            Timber.d("already loaded")
            return
        }

        try {
            etagMap.clear()
            allMovies.clear()

            Timber.d("loading movies")
            val movies = client!!.movies(0)
            Timber.d("got movies")
            for (m in movies.movies) {
                val video = toVideo(m)
                allMovies.add(video)
                etagMap[video.etag] = video
            }
            Timber.d("loading movies..done")

            Timber.d("loading series")
            allSeries.clear()
            allEpisodes.clear()
            val tvList = client!!.tvList(0)
            for (s in tvList.series) {
                val episodes = mutableListOf<TVEpisode>()
                for (e in tvList.episodes) {
                    val video = toVideo(s, e)
                    allEpisodes.add(video)
                    etagMap[video.etag] = video
                    if (s.tvid == e.tvid) {
                        episodes.add(e)
                    }
                }
                allSeries.add(toSeries(s, episodes))
            }
            Timber.d("loading series..done")

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
        newMovies.clear()
        addedMovies.clear()
        recommendMovies.clear()
        view.newMovies.forEach {
            newMovies.add(toVideo(it))
        }
        view.addedMovies.forEach {
            addedMovies.add(toVideo(it))
        }
        view.recommendMovies?.forEach { recommend ->
            val videos = mutableListOf<Video>()
            for (m in recommend.movies) {
                val v = etagMap[m.etag]
                if (v != null) {
                    videos.add(v)
                }
            }
//            val videos = recommend.movies.map { m -> toVideo(m) }
            recommendMovies.add(VideoGroup(category = recommend.name, videoList = videos))
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
        if (recommendMovies.isNotEmpty()) {
            groups.addAll(recommendMovies)
        }
        if (newMovies.isNotEmpty()) {
            groups.add(VideoGroup(category = "New Releases", newMovies))
        }
        if (addedMovies.isNotEmpty()) {
            groups.add(VideoGroup(category = "Recently Added", addedMovies))
        }
        return groups
    }

    override suspend fun getAllVideos(refresh: Boolean): List<Video> {
        if (refresh) {
            clearCache()
        }
        load()
        return allMovies
    }

    override suspend fun getAllSeries(refresh: Boolean): List<Series> {
        if (refresh) {
            clearCache()
        }
        load()
        return allSeries
    }

    private val idPattern = Regex("""/([0-9]+)/?""")

    private fun getId(uri: String): Int {
        val values = idPattern.find(uri)?.groupValues ?: emptyList()
        return if (values.isNotEmpty()) values[1].toInt() else -1
    }

    // takeout://movies/$id
    // takeout://tv/episodes/$id
    override suspend fun getVideoDetail(id: String): Detail? {
        if (!checkSignedIn()) return null
        if (id.startsWith("takeout://movies/")) {
            val view = client!!.movie(getId(id), 0)
            return toDetail(view)
        } else if (id.startsWith("takeout://tv/episodes/")) {
            val view = client!!.tvEpisode(getId(id), 0)
            return toDetail(view)
        }
        return null
    }

    override suspend fun getProfile(id: String): Profile? {
        if (!checkSignedIn()) return null
        var profile: Profile?
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
        return allMovies.firstOrNull { it.id == id } ?: etagMap[id]
    }

    override fun getVideoByVideoUri(uri: String): Video? {
        return allMovies
            .firstOrNull { it.videoUri == uri }
    }

    override fun getAllVideosFromSeries(seriesUri: String): List<Video> {
        return allMovies.filter { it.seriesUri == seriesUri }
    }

    override suspend fun updateProgress(progress: List<Progress>): Int {
        val client = client!!

        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        df.timeZone = TimeZone.getTimeZone("UTC")

        val list = mutableListOf<Offset>()
        progress.forEach { p ->
            val video = getVideoById(p.id)
            if (video != null) {
                val date = df.format(Date(p.timestamp))
                val offset = Offset(
                    etag = video.etag,
                    offset = (p.position / 1000).toInt(),
                    duration = (p.duration?.div(1000))?.toInt(),
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
            val video = etagMap[it.etag]
            if (video != null) {
                list.add(toProgress(video, it))
            }
        }
        return list
    }

    private fun toProfile(p: ProfileView): Profile {
        val videos = mutableListOf<Video>()
        val series = mutableListOf<Series>()

        if (p.movies.starring != null) {
            for (m in p.movies.starring) {
                val id = "takeout://movies/${m.id}"
                val v = allMovies.firstOrNull { it.id == id }
                if (v != null) {
                    videos.add(v)
                }
            }
        }

        if (p.shows.starring != null) {
            for (s in p.shows.starring) {
                val id = "takeout://tv/series/${s.id}"
                val v = allSeries.firstOrNull { it.id == id }
                if (v != null) {
                    series.add(v)
                }
            }
        }

        return Profile(
            id = "takeout://people/${p.person.peid}/profile",
            person = toPerson(p.person),
            videos = videos,
            series = series,
        )
    }

    private fun toPerson(p: com.takeoutfm.tv.client.Person): Person {
        val endpoint = userInfo?.endpoint ?: ""
        return Person(
            id = "takeout://people/${p.peid}",
            name = p.name,
            bio = p.bio ?: "",
            birthplace = p.birthplace ?: "",
            birthday = p.year(),
            thumbnailUri = "$endpoint/img/tm/w185${p.profilePath}"
        )
    }

    private fun toCast(c: com.takeoutfm.tv.client.Cast): Cast {
        return Cast(
            id = "takeout://cast/${c.id}",
            person = toPerson(c.person),
            character = c.character
        )
    }

    private fun toDetail(view: MovieView): Detail {
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

    private fun toDetail(view: TVEpisodeView): Detail {
        return Detail(
            id = "takeout://tv/episodes/${view.episode.id}/detail",
            uri = "${client!!.endpoint()}${view.location}",
            headers = mapOf(
                HttpHeaders.Authorization to "Bearer ${userInfo?.mediaToken}",
                HttpHeaders.UserAgent to client!!.userAgent()
            ),
            video = toVideo(view.series, view.episode),
            genres = emptyList(),
            cast = view.cast?.map { toCast(it) } ?: emptyList(),
            related = emptyList(),
        )
    }

    private fun category(title: String): String {
        val ch = title[0]
        return if (ch.isDigit()) {
            // all numeric will be category #
            "#"
        } else {
            ch.uppercase()
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

    private fun toSeries(s: TVSeries, episodes: List<TVEpisode>): Series {
        val endpoint = userInfo?.endpoint ?: ""
        val posterUrl = "$endpoint/img/tm/w342${s.posterPath}"
        val backdropUrl = "$endpoint/img/tm/w1280${s.backdropPath}"
        val uri = "takeout://tv/series/${s.id}"
        val vote = if (s.voteAverage == null) 0 else (s.voteAverage * 10).toInt()
        return Series(
            id = uri,
            name = s.name,
            thumbnailUri = posterUrl,
            backgroundImageUri = backdropUrl,
            tagline = s.tagline,
            rating = s.rating,
            year = s.year(),
            vote = vote,
            seasonCount = s.seasonCount,
            episodeCount = s.episodeCount,
            episodes = episodes.map { toVideo(s, it) }
        )
    }

    private fun toVideo(s: TVSeries, e: TVEpisode): Video {
        val endpoint = userInfo?.endpoint ?: ""
        val posterUrl = "$endpoint/img/tm/w300${e.stillPath}"
        val backdropUrl = "$endpoint/img/tm/w1280${s.backdropPath}"
        val category = category(s.sortName)
        val vote = if (e.voteAverage == null) 0 else (e.voteAverage * 10).toInt()
        val uri = "takeout://tv/episodes/${e.id}"
        return Video(
            id = uri,
            name = e.name,
            description = e.overview,
            uri = uri,
            videoUri = "", // get from detail
            thumbnailUri = posterUrl,
            backgroundImageUri = backdropUrl,
            category = category,
            videoType = VideoType.EPISODE,
            seasonNumber = "${e.season}",
            episodeNumber = "${e.episode}",
            seriesUri = "takeout://tv/series/${s.id}",
            seasonUri = "takeout://tv/series/${s.id}/season/${e.season}",
            vote = vote,
            year = e.year(),
            rating = s.rating,
            duration = e.iso8601(),
            tagline = s.tagline,
            etag = e.key()
        )
    }

    private fun toProgress(video: Video, offset: Offset): Progress {
        // TODO optimize this
        var date: Date?
        try {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            df.timeZone = TimeZone.getTimeZone("UTC")
            date = df.parse(offset.date)
        } catch (_: ParseException) {
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

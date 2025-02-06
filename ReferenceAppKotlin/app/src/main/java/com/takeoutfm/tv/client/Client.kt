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

package com.takeoutfm.tv.client

import android.os.Build
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.URLEncoder

class Client(
    private val endpoint: String = defaultEndpoint,
    private var tokens: Tokens? = null
) {
    private val client: HttpClient = client()
    private var listener: Listener? = null

    private val version = "0.4.2" // #version#

    private fun client(timeout: Long = defaultTimeout): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                    allowSpecialFloatingPointValues = true
                    useArrayPolymorphism = false
                })
            }
            install(HttpTimeout) {
                timeout.let {
                    connectTimeoutMillis = it
                    requestTimeoutMillis = it
                    socketTimeoutMillis = it
                }
            }
            expectSuccess = true
        }
    }

    fun addListener(l: Listener) {
        listener = l
    }

    interface Listener {
        fun onTokens(tokens: Tokens?)
        fun onAccessCode(accessCode: AccessCode?)
    }

    fun close() {
        client.close()
    }

    fun endpoint(): String {
        return endpoint
    }

    fun userAgent(): String {
        return "TakeoutFM-TV/$version (takeoutfm.com; Android ${Build.VERSION.RELEASE})"
    }

    private suspend inline fun <reified T> get(uri: String, ttl: Int? = 0): T {
        Timber.d("get $endpoint$uri")
        val accessToken = tokens?.accessToken ?: throw IllegalStateException()
        return client.get("$endpoint$uri") {
            accept(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.UserAgent, userAgent())
        }.body()
    }

    private suspend inline fun <reified T> post(uri: String, body: Any?): T {
        Timber.d("post $endpoint$uri")
        val accessToken = tokens?.accessToken ?: throw IllegalStateException()
        return client.post("$endpoint$uri") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.UserAgent, userAgent())
            setBody(body)
        }.body()
    }

    private suspend inline fun <reified T> retryGet(uri: String, ttl: Int? = 0): T {
        try {
           return get(uri, ttl)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                if (refreshTokens()) {
                    return get(uri, ttl)
                }
            }
            throw e
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend inline fun <reified T> retryPost(uri: String, body: Any?): T {
        try {
            return post(uri, body)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                if (refreshTokens()) {
                    return post(uri, body)
                }
            }
            throw e
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun login(user: String, pass: String): Tokens? {
        Timber.d("post $endpoint/api/token")
        val body = User(user, pass)
        val tokens: Tokens? = client.post("$endpoint/api/token") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, userAgent())
            setBody(body)
        }.body()
        listener?.onTokens(tokens)
        return tokens
    }

    suspend fun code(): AccessCode? {
        return try {
            Timber.d("get $endpoint/api/code")
            val accessCode: AccessCode? = client.get("$endpoint/api/code") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, userAgent())
            }.body()
            listener?.onAccessCode(accessCode)
            accessCode
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    suspend fun checkCode(accessCode: AccessCode): Tokens? {
        return try {
            Timber.d("post $endpoint/api/code")
            val tokens: Tokens? = client.post("$endpoint/api/code") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, userAgent())
                header(HttpHeaders.Authorization, "Bearer ${accessCode.accessToken}")
                setBody(accessCode)
            }.body()
            listener?.onTokens(tokens)
            tokens
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    fun loggedIn(): Boolean {
        return tokens?.valid() ?: false
    }

    private suspend fun refreshTokens(): Boolean {
        Timber.d("refreshTokens")
        val client = client()
        val mediaToken = tokens?.mediaToken ?: throw IllegalStateException()
        val refreshToken = tokens?.refreshToken ?: throw IllegalStateException()
        Timber.d("get $endpoint/api/token")
        try {
            val result: RefreshTokens = client.get("$endpoint/api/token") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, userAgent())
                header(HttpHeaders.Authorization, "Bearer $refreshToken")
            }.body()
            return if (result.valid()) {
                tokens = Tokens(
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken,
                    mediaToken = mediaToken
                )
                listener?.onTokens(tokens)
                true
            } else {
                false
            }
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                // refresh token no longer valid
                tokens = null
                listener?.onTokens(null)
            }
            return false
        }
    }

    suspend fun home(ttl: Int): HomeView {
        return retryGet("/api/home", ttl)
    }

    suspend fun artists(ttl: Int): ArtistsView {
        return retryGet("/api/artists", ttl)
    }

    suspend fun artist(id: Int, ttl: Int): ArtistView {
        return retryGet("/api/artist/$id", ttl)
    }

    suspend fun artistSingles(id: Int, ttl: Int): SinglesView {
        return retryGet("/api/artist/$id/singles", ttl)
    }

    suspend fun artistSinglesPlaylist(id: Int, ttl: Int): Spiff {
        return retryGet("/api/artist/$id/singles/playlist", ttl)
    }

    suspend fun artistPopular(id: Int, ttl: Int): PopularView {
        return retryGet("/api/artist/$id/popular", ttl)
    }

    suspend fun artistPopularPlaylist(id: Int, ttl: Int): Spiff {
        return retryGet("/api/artist/$id/popular/playlist", ttl)
    }

    suspend fun artistPlaylist(id: Int, ttl: Int): Spiff {
        return retryGet("/api/artist/$id/playlist", ttl)
    }

    suspend fun artistRadio(id: Int, ttl: Int): Spiff {
        return retryGet("/api/artist/$id/radio", ttl)
    }

    suspend fun release(id: Int, ttl: Int): ReleaseView {
        return retryGet("/api/releases/$id", ttl)
    }

    suspend fun releasePlaylist(id: Int, ttl: Int): Spiff {
        return retryGet("/api/releases/$id/playlist", ttl)
    }

    suspend fun radio(ttl: Int): RadioView {
        return retryGet("/api/radio", ttl)
    }

    suspend fun station(id: Int, ttl: Int): Spiff {
        return retryGet("/api/stations/$id", ttl)
    }

    suspend fun movies(ttl: Int): MoviesView {
        return retryGet("/api/movies", ttl)
    }

    suspend fun movie(id: Int, ttl: Int): MovieView {
        return retryGet("/api/movies/$id", ttl)
    }

    suspend fun moviePlaylist(id: Int, ttl: Int): Spiff {
        return retryGet("/api/movies/$id/playlist", ttl)
    }

    suspend fun tvList(ttl: Int): TVListView {
        return retryGet("/api/tv", ttl)
    }

    suspend fun tvShows(ttl: Int): TVShowsView {
        return retryGet("/api/tv/series", ttl)
    }

    suspend fun tvSeries(id: Int, ttl: Int): TVSeriesView {
        return retryGet("/api/tv/series/$id", ttl)
    }

    suspend fun tvEpisode(id: Int, ttl: Int): TVEpisodeView {
        return retryGet("/api/tv/episodes/$id", ttl)
    }

    suspend fun profile(peid: Int, ttl: Int): ProfileView {
        return retryGet("/api/profiles/$peid", ttl)
    }

    suspend fun genre(name: String, ttl: Int): GenreView {
        return retryGet("/api/movie-genres/$name", ttl)
    }

    suspend fun playlist(ttl: Int? = null): Spiff {
        return retryGet("/api/playlist", ttl)
    }

    suspend fun search(query: String): SearchView {
        val q = URLEncoder.encode(query, "utf-8")
        return retryGet("/api/search?q=$q", 0)
    }

    suspend fun progress(ttl: Int): ProgressView {
        return retryGet("/api/progress", ttl)
    }

    suspend fun updateProgress(offsets: Offsets): Int {
        val response: HttpResponse = retryPost("/api/progress", offsets)
        return response.status.value;
    }

    companion object {
        const val defaultEndpoint = "https://"
        const val defaultTimeout = 30 * 1000L
    }
}

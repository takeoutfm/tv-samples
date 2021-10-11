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

package com.defsub.takeout.client

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import timber.log.Timber
import java.net.URLEncoder

class Client(private val endpoint: String = defaultEndpoint,
             private var cookie: String? = null) {
    private var client: HttpClient = client()

    private fun client(timeout: Long = defaultTimeout): HttpClient {
        return HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
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
        }
    }

    fun close() {
        client.close()
    }

    private suspend inline fun <reified T> get(uri: String, ttl: Int? = 0): T {
        Timber.d("get $endpoint$uri")
        return client.get("$endpoint$uri") {
            accept(ContentType.Application.Json)
                cookie?.let { header(HttpHeaders.Cookie, "Takeout=$cookie") }
        }
    }

    fun loggedIn(): Boolean {
        return cookie != null
    }

    suspend fun login(user: String, pass: String): String? {
        cookie = null
        val client = client()
        val result: LoginResponse = client.post("$endpoint/api/login") {
            contentType(ContentType.Application.Json)
            body = User(user, pass)
        }
        return if (result.status == 200) {
            cookie = result.cookie
            cookie
        } else {
            null
        }
    }

    suspend fun home(ttl: Int): HomeView {
        return get("/api/home", ttl)
    }

    suspend fun artists(ttl: Int): ArtistsView {
        return get("/api/artists", ttl)
    }

    suspend fun artist(id: Int, ttl: Int): ArtistView {
        return get("/api/artist/$id", ttl)
    }

    suspend fun artistSingles(id: Int, ttl: Int): SinglesView {
        return get("/api/artist/$id/singles", ttl)
    }

    suspend fun artistSinglesPlaylist(id: Int, ttl: Int): Spiff {
        return get("/api/artist/$id/singles/playlist", ttl)
    }

    suspend fun artistPopular(id: Int, ttl: Int): PopularView {
        return get("/api/artist/$id/popular", ttl)
    }

    suspend fun artistPopularPlaylist(id: Int, ttl: Int): Spiff {
        return get("/api/artist/$id/popular/playlist", ttl)
    }

    suspend fun artistPlaylist(id: Int, ttl: Int): Spiff {
        return get("/api/artist/$id/playlist", ttl)
    }

    suspend fun artistRadio(id: Int, ttl: Int): Spiff {
        return get("/api/artist/$id/radio", ttl)
    }

    suspend fun release(id: Int, ttl: Int): ReleaseView {
        return get("/api/releases/$id", ttl)
    }

    suspend fun releasePlaylist(id: Int, ttl: Int): Spiff {
        return get("/api/releases/$id/playlist", ttl)
    }

    suspend fun radio(ttl: Int): RadioView {
        return get("/api/radio", ttl)
    }

    suspend fun station(id: Int, ttl: Int): Spiff {
        return get("/api/radio/$id", ttl)
    }

    suspend fun movies(ttl: Int): MoviesView {
        return get("/api/movies", ttl)
    }

    suspend fun movie(id: Int, ttl: Int): MovieView {
        return get("/api/movies/$id", ttl)
    }

    suspend fun profile(id: Int, ttl: Int): ProfileView {
        return get("/api/profiles/$id", ttl)
    }

    suspend fun genre(name: String, ttl: Int): GenreView {
        return get("/api/movies/genres/$name", ttl)
    }

    suspend fun playlist(ttl: Int? = null): Spiff {
        return get("/api/playlist", ttl)
    }

    suspend fun search(query: String): SearchView {
        val q = URLEncoder.encode(query, "utf-8")
        return get("/api/search?q=$q", 0)
    }

    companion object {
        const val defaultEndpoint = "https://takeout.fm"
        const val defaultTimeout = 30*1000L
    }
}
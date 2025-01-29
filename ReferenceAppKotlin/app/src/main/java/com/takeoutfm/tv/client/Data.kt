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

@file:OptIn(InternalSerializationApi::class)

package com.takeoutfm.tv.client

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class User(
    @SerialName("User") val user: String,
    @SerialName("Pass") val pass: String
)

@Serializable
data class LoginResponse(
    @SerialName("Status") val status: Int,
    @SerialName("Message") val message: String,
    @SerialName("Cookie") val cookie: String
)

@Serializable
data class Tokens(
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("MediaToken") val mediaToken: String,
    @SerialName("RefreshToken") val refreshToken: String
)

fun Tokens.valid(): Boolean {
    return accessToken.isNotEmpty() && mediaToken.isNotEmpty() && refreshToken.isNotEmpty()
}

@Serializable
data class AccessCode(
    @SerialName("Code") val code: String,
    @SerialName("AccessToken") val accessToken: String,
)

fun AccessCode.isEmpty(): Boolean {
    return code.isEmpty() || accessToken.isEmpty()
}

@Serializable
data class RefreshTokens(
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("RefreshToken") val refreshToken: String
)

fun RefreshTokens.valid(): Boolean {
    return accessToken.isNotEmpty() && refreshToken.isNotEmpty()
}

@Serializable
data class Artist(
    @SerialName("ID") val id: Int,
    @SerialName("Name") val name: String,
    @SerialName("SortName") val sortName: String,
    @SerialName("ARID") val arid: String?,
    @SerialName("Disambiguation") val disambiguation: String?,
    @SerialName("Country") val country: String?,
    @SerialName("Area") val area: String?,
    @SerialName("Date") val date: String?,
    @SerialName("EndDate") val endData: String?,
    @SerialName("Genre") val genre: String?
)

@Serializable
data class Track(
    @SerialName("ID") val id: Int,
    @SerialName("Artist") val artist: String,
    @SerialName("Release") val release: String,
    @SerialName("Date") val date: String?,
    @SerialName("TrackNum") val trackNum: Int?,
    @SerialName("DiscNum") val discNum: Int?,
    @SerialName("Title") val title: String,
    @SerialName("Size") val size: Int,
    @SerialName("RGID") val rgid: String?,
    @SerialName("REID") val reid: String?,
    @SerialName("ReleaseTitle") val releaseTitle: String,
    @SerialName("ETag") val etag: String,
    @SerialName("Artwork") val artwork: Boolean,
    @SerialName("FrontArtwork") val frontArtwork: Boolean,
    @SerialName("BackArtwork") val backArtwork: Boolean,
    @SerialName("OtherArtwork") val otherArtwork: String?,
    @SerialName("GroupArtwork") val groupArtwork: Boolean
)

fun Track.otherArtwork(): Boolean {
    return if (otherArtwork != null && otherArtwork.isNotEmpty()) {
//        otherArtwork.toBooleanStrict()
        false
    } else {
        false
    }
}

fun Track.year(): Int {
    return year(date)
}

fun Track.cover(size: Int = 250): String {
    val url = if (groupArtwork) {
        "https://coverartarchive.org/release-group/$rgid"
    } else {
        "https://coverartarchive.org/release/$reid"
    }
    if (artwork && frontArtwork) {
        return "$url/front-$size"
    } else if (artwork && otherArtwork()) {
        return "$url/$otherArtwork-$size"
    }
    return ""
}

//fun Track.location(): String {
//    return "/api/tracks/$uuid/location"
//}

fun Track.key(): String {
    return etag.replace("\"", "")
}

@Serializable
data class Release(
    @SerialName("ID") val id: Int,
    @SerialName("Name") val name: String,
    @SerialName("Artist") val artist: String,
    @SerialName("RGID") val rgid: String?,
    @SerialName("REID") val reid: String?,
    @SerialName("Disambiguation") val disambiguation: String?,
    @SerialName("Type") val type: String?,
    @SerialName("Date") val date: String?,
    @SerialName("ReleaseDate") val releaseDate: String?,
    @SerialName("Artwork") val artwork: Boolean,
    @SerialName("FrontArtwork") val frontArtwork: Boolean,
    @SerialName("BackArtwork") val backArtwork: Boolean,
    @SerialName("OtherArtwork") val otherArtwork: String?,
    @SerialName("GroupArtwork") val groupArtwork: Boolean
)

fun Release.otherArtwork(): Boolean {
    return if (otherArtwork != null && otherArtwork.isNotEmpty()) {
//        otherArtwork.toBooleanStrict()
        false
    } else {
        false
    }
}

fun Release.year(): Int {
    return year(date)
}

fun Release.cover(size: Int = 250): String {
    val url = if (groupArtwork) {
        "https://coverartarchive.org/release-group/$rgid"
    } else {
        "https://coverartarchive.org/release/$reid"
    }
    if (artwork && frontArtwork) {
        return "$url/front-$size"
    } else if (artwork && otherArtwork()) {
        return "$url/$otherArtwork-$size"
    }
    return ""
}

@Serializable
data class Location(
    @SerialName("ID") val id: Int,
    @SerialName("Url") val url: String,
    @SerialName("Size") val size: Long,
    @SerialName("ETag") val etag: String
)

@Serializable
data class Station(
    @SerialName("ID") val id: Int,
    @SerialName("Name") val name: String,
    @SerialName("Ref") val ref: String
)

@Serializable
data class Movie(
    @SerialName("ID") val id: Int,
    @SerialName("TMID") val tmid: Int,
    @SerialName("IMID") val imid: String,
    @SerialName("Title") val title: String,
    @SerialName("SortTitle") val sortTitle: String,
    @SerialName("Date") val date: String,
    @SerialName("Rating") val rating: String,
    @SerialName("Tagline") val tagline: String,
    @SerialName("Overview") val overview: String,
    @SerialName("Budget") val budget: String,
    @SerialName("Revenue") val revenue: String,
    @SerialName("Runtime") val runtime: Int,
    @SerialName("VoteAverage") val voteAverage: Float?,
    @SerialName("VoteCount") val voteCount: Int?,
    @SerialName("BackdropPath") val backdropPath: String,
    @SerialName("PosterPath") val posterPath: String,
    @SerialName("ETag") val etag: String
)

fun Movie.year(): Int {
    return year(date)
}

fun Movie.key(): String {
    return etag.replace("\"", "")
}

fun Movie.iso8601(): String {
    val hours = runtime / 60
    val mins = runtime % 60
    return "PT%02dH%02dM".format(hours, mins)
}


@Serializable
data class TVSeries(
    @SerialName("ID") val id: Int,
    @SerialName("TVID") val tvid: Int,
    @SerialName("Name") val name: String,
    @SerialName("SortName") val sortName: String,
    @SerialName("Overview") val overview: String,
    @SerialName("Date") val date: String,
    @SerialName("EndDate") val endDate: String,
    @SerialName("Tagline") val tagline: String,
    @SerialName("SeasonCount") val seasonCount: Int,
    @SerialName("EpisodeCount") val episodeCount: Int,
    @SerialName("VoteAverage") val voteAverage: Float?,
    @SerialName("VoteCount") val voteCount: Int?,
    @SerialName("PosterPath") val posterPath: String,
    @SerialName("BackdropPath") val backdropPath: String,
    @SerialName("Rating") val rating: String,
)

fun TVSeries.year(): Int {
    return year(date)
}

@Serializable
data class TVEpisode(
    @SerialName("ID") val id: Int,
    @SerialName("TVID") val tvid: Int,
    @SerialName("Name") val name: String,
    @SerialName("Overview") val overview: String,
    @SerialName("Date") val date: String,
    @SerialName("StillPath") val stillPath: String,
    @SerialName("Runtime") val runtime: Int,
    @SerialName("Season") val season: Int,
    @SerialName("Episode") val episode: Int,
    @SerialName("VoteAverage") val voteAverage: Float?,
    @SerialName("VoteCount") val voteCount: Int?,
    @SerialName("ETag") val etag: String,
    @SerialName("Size") val size: Long,
)

fun TVEpisode.year(): Int {
    return year(date)
}

fun TVEpisode.key(): String {
    return etag.replace("\"", "")
}

fun TVEpisode.iso8601(): String {
    val hours = runtime / 60
    val mins = runtime % 60
    return "PT%02dH%02dM".format(hours, mins)
}

@Serializable
data class TVListView(
    @SerialName("Series") val series: List<TVSeries>,
    @SerialName("Episodes") val episodes: List<TVEpisode>
)

@Serializable
data class TVShowsView(
    @SerialName("Series") val series: List<TVSeries>
)

@Serializable
data class TVSeriesView(
    @SerialName("Series") val series: TVSeries,
    @SerialName("Episodes") val episodes: List<TVEpisode>,
    @SerialName("Cast") val cast: List<Cast>?,
    @SerialName("Crew") val crew: List<Crew>?,
    @SerialName("Starring") val starring: List<Person>?,
    @SerialName("Directing") val directing: List<Person>?,
    @SerialName("Writing") val writing: List<Person>?,
    @SerialName("Genres") val genres: List<String>,
    @SerialName("Vote") val vote: Int,
    @SerialName("VoteCount") val voteCount: Int,
)

@Serializable
data class TVEpisodeView(
    @SerialName("Series") val series: TVSeries,
    @SerialName("Episode") val episode: TVEpisode,
    @SerialName("Location") val location: String,
    @SerialName("Cast") val cast: List<Cast>?,
    @SerialName("Crew") val crew: List<Crew>?,
    @SerialName("Starring") val starring: List<Person>?,
    @SerialName("Directing") val directing: List<Person>?,
    @SerialName("Writing") val writing: List<Person>?,
    @SerialName("Vote") val vote: Int,
    @SerialName("VoteCount") val voteCount: Int,
)

@Serializable
data class Person(
    @SerialName("ID") val id: Int,
    @SerialName("PEID") val peid: Int,
    @SerialName("Name") val name: String,
    @SerialName("ProfilePath") val profilePath: String?,
    @SerialName("Bio") val bio: String?,
    @SerialName("Birthplace") val birthplace: String?,
    @SerialName("Birthday") val birthday: String?,
    @SerialName("Deathday") val deathday: String?
)

fun Person.year(): Int {
    return year(birthday)
}

interface Role {
    val person: Person
    val role: String
}

@Serializable
data class Cast(
    @SerialName("ID") val id: Int,
    @SerialName("TMID") val tmid: Int? = null,
    @SerialName("TVID") val tvid: Int? = null,
    @SerialName("EID") val eid: Int? = null,
    @SerialName("PEID") val peid: Int,
    @SerialName("Character") val character: String,
    @SerialName("Person") override val person: Person,
) : Role {
    override val role get() = character
}

@Serializable
data class Crew(
    @SerialName("ID") val id: Int,
    @SerialName("TMID") val tmid: Int? = null,
    @SerialName("TVID") val tvid: Int? = null,
    @SerialName("EID") val eid: Int? = null,
    @SerialName("PEID") val peid: Int,
    @SerialName("Department") val department: String,
    @SerialName("Job") val job: String,
    @SerialName("Person") override val person: Person
) : Role {
    override val role get() = job
}

@Serializable
data class Collection(
    @SerialName("ID") val id: Int,
    @SerialName("Name") val name: String,
    @SerialName("SortName") val sortName: String,
    @SerialName("TMID") val tmid: String
)

@Serializable
data class Recommend(
    @SerialName("Name") val name: String,
    @SerialName("Movies") val movies: List<Movie>,
)

@Serializable
data class HomeView(
    @SerialName("AddedReleases") val added: List<Release>,
    @SerialName("NewReleases") val released: List<Release>,
    @SerialName("AddedMovies") val addedMovies: List<Movie>,
    @SerialName("NewMovies") val newMovies: List<Movie>,
    @SerialName("RecommendMovies") val recommendMovies: List<Recommend>?,
    @SerialName("AddedTVEpisodes") val addedTVEpisodes: List<TVEpisode>?,
)

@Serializable
data class ArtistView(
    @SerialName("Artist") val artist: Artist,
    @SerialName("Image") val image: String?,
    @SerialName("Background") val background: String?,
    @SerialName("Releases") val releases: List<Release>,
    @SerialName("Popular") val popular: List<Track>,
    @SerialName("Singles") val singles: List<Track>,
    @SerialName("Similar") val similar: List<Artist>
)

@Serializable
data class ReleaseView(
    @SerialName("Artist") val artist: Artist,
    @SerialName("Release") val release: Release,
    @SerialName("Tracks") val tracks: List<Track>,
    @SerialName("Popular") val popular: List<Track>,
    @SerialName("Singles") val singles: List<Track>,
    @SerialName("Similar") val similar: List<Release>
)

@Serializable
data class SearchView(
    @SerialName("Artists") val artists: List<Artist>?,
    @SerialName("Releases") val releases: List<Release>?,
    @SerialName("Tracks") val tracks: List<Track>?,
    @SerialName("Movies") val movies: List<Movie>?,
    @SerialName("Query") val query: String,
    @SerialName("Hits") val hits: String
)

@Serializable
data class SinglesView(
    @SerialName("Artist") val artist: Artist,
    @SerialName("Singles") val singles: List<Track>
)

@Serializable
data class PopularView(
    @SerialName("Artist") val artist: Artist,
    @SerialName("Popular") val popular: List<Track>
)

@Serializable
data class MoviesView(
    @SerialName("Movies") val movies: List<Movie>,
)

@Serializable
data class MovieView(
    @SerialName("Movie") val movie: Movie,
    @SerialName("Location") val location: String,
    @SerialName("Collection") val collection: Collection?,
    @SerialName("Other") val other: List<Movie>?,
    @SerialName("Cast") val cast: List<Cast>?,
    @SerialName("Crew") val crew: List<Crew>?,
    @SerialName("Starring") val starring: List<Person>?,
    @SerialName("Directing") val directing: List<Person>?,
    @SerialName("Writing") val writing: List<Person>?,
    @SerialName("Genres") val genres: List<String>?,
    @SerialName("Vote") val vote: Int?,
    @SerialName("VoteCount") val voteCount: Int?,
)

@Serializable
data class MovieCredits(
    @SerialName("Starring") val starring: List<Movie>?,
    @SerialName("Directing") val directing: List<Movie>?,
    @SerialName("Writing") val writing: List<Movie>?,
)

@Serializable
data class TVCredits(
    @SerialName("Starring") val starring: List<TVSeries>?,
    @SerialName("Directing") val directing: List<TVSeries>?,
    @SerialName("Writing") val writing: List<TVSeries>?,
)

@Serializable
data class ProfileView(
    @SerialName("Person") val person: Person,
    @SerialName("Movies") val movies: MovieCredits,
    @SerialName("Shows") val shows: TVCredits,
)

@Serializable
data class GenreView(
    @SerialName("Name") val name: String,
    @SerialName("Movies") val movies: List<Movie>,
)

@Serializable
data class ArtistsView(
    @SerialName("Artists") val artists: List<Artist>?
)

@Serializable
data class RadioView(
    @SerialName("Genre") val genre: List<Station>?,
    @SerialName("Similar") val similar: List<Station>?,
    @SerialName("Period") val period: List<Station>?,
    @SerialName("Series") val series: List<Station>?,
    @SerialName("Other") val other: List<Station>?
)

@Serializable
data class Spiff(
    @SerialName("Index") val index: Int,
    @SerialName("Position") val position: Float,
    @SerialName("Playlist") val playlist: Playlist
)

@Serializable
data class Entry(
    @SerialName("Creator") val creator: String,
    @SerialName("Album") val album: String,
    @SerialName("Title") val title: String,
    @SerialName("Image") val image: String,
    @SerialName("Location") val locations: List<String>,
    @SerialName("Identifier") val identifiers: List<String>,
    @SerialName("Size") val sizes: List<Int> = emptyList()
)

@Serializable
data class Playlist(
    @SerialName("Location") val location: String? = null,
    @SerialName("Creator") val creator: String? = null,
    @SerialName("Title") val title: String,
    @SerialName("Image") val image: String? = null,
    @SerialName("Track") val tracks: List<Entry>
)

@Serializable
data class Offset(
    @SerialName("ID") val id: Int? = null,
    @SerialName("ETag") val etag: String,
    @SerialName("Duration") val duration: Int? = null,
    @SerialName("Offset") val offset: Int,
    @SerialName("Date") val date: String
)

@Serializable
data class Offsets(
    @SerialName("Offsets") val offsets: List<Offset>
)

@Serializable
data class ProgressView(
    @SerialName("Offsets") val offsets: List<Offset>
)

private val utcFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))

private fun year(date: String?): Int {
    var year = -1
    date?.let {
        try {
            val d = LocalDate.parse(date, utcFormatter)
            year = d.year
        } catch (e: DateTimeException) {
        }
    }
    return year
}
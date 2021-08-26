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

fun Track.location(): String {
    return "/api/tracks/$id/location"
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
    @SerialName("Runtime") val runtime: String,
    @SerialName("VoteAverage") val voteAverage: Float?,
    @SerialName("VoteCount") val voteCount: Int?,
    @SerialName("BackdropPath") val backdropPath: String,
    @SerialName("PosterPath") val posterPath: String,
    @SerialName("ETag") val etag: String
)

fun Movie.year(): Int {
    return year(date)
}

fun Movie.location(): String {
    return "/api/movies/$id/location"
}

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

@Serializable
data class Cast(
    @SerialName("ID") val id: Int,
    @SerialName("TMID") val tmid: Int,
    @SerialName("PEID") val peid: Int,
    @SerialName("Character") val character: String,
    @SerialName("Person") val person: Person
)

@Serializable
data class Crew(
    @SerialName("ID") val id: Int,
    @SerialName("TMID") val tmid: Int,
    @SerialName("PEID") val peid: Int,
    @SerialName("Department") val department: String,
    @SerialName("Job") val job: String,
    @SerialName("Person") val person: Person
)

@Serializable
data class Collection(
    @SerialName("ID") val id: Int,
    @SerialName("Name") val name: String,
    @SerialName("SortName") val sortName: String,
    @SerialName("TMID") val tmid: String
)

@Serializable
data class HomeView(
    @SerialName("AddedReleases") val added: List<Release>,
    @SerialName("NewReleases") val released: List<Release>,
    @SerialName("AddedMovies") val addedMovies: List<Movie>,
    @SerialName("NewMovies") val newMovies: List<Movie>
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
data class ProfileView(
    @SerialName("Person") val person: Person,
    @SerialName("Starring") val starring: List<Movie>?,
    @SerialName("Directing") val directing: List<Movie>?,
    @SerialName("Writing") val writing: List<Movie>?,
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
    @SerialName("index") val index: Int,
    @SerialName("position") val position: Float,
    @SerialName("playlist") val playlist: Playlist
)

@Serializable
data class Entry(
    @SerialName("creator") val creator: String,
    @SerialName("album") val album: String,
    @SerialName("title") val title: String,
    @SerialName("image") val image: String,
    @SerialName("location") val locations: List<String>,
    @SerialName("identifier") val identifiers: List<String>,
    @SerialName("size") val sizes: List<Int> = emptyList()
)

@Serializable
data class Playlist(
    @SerialName("location") val location: String? = null,
    @SerialName("creator") val creator: String? = null,
    @SerialName("title") val title: String,
    @SerialName("image") val image: String? = null,
    @SerialName("track") val tracks: List<Entry>
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
package com.example.movienight.dto

import com.google.gson.annotations.SerializedName


data class MovieResponse(
    val results: List<Movie>
)

data class Movie(
    val id: Int,
    val title: String,
    val overview: String?,
    val poster_path: String?,
    @SerializedName("vote_average") val voteAverage: Float,
    @SerializedName("release_date") val release_date: String?,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("original_language") val original_language: String?,
    val genre_ids: List<Int>? = emptyList(),
    val runtime: Int? = null
)


data class WatchProvidersResponse(
    val results: Map<String, CountryProviders>
)

data class CountryProviders(
    val flatrate: List<Provider>?, val rent: List<Provider>?, val buy: List<Provider>?
)

data class GenreResponse(
    val genres: List<Genre>
)

data class Genre(
    val id: Int, val name: String
)


data class Provider(
    val provider_id: Int,
    val provider_name: String,
    val logo_path: String?,
    var type: String? = null
)

data class ReleaseDatesResponse(
    val results: List<ReleaseDateResult>
)

data class ReleaseDateResult(
    val iso_3166_1: String, val release_dates: List<ReleaseDate>
)

data class ReleaseDate(
    val certification: String, val note: String?
)

data class MovieDetailsResponse(
    val id: Int,
    val title: String,
    val runtime: Int?,
    val overview: String?,
    val poster_path: String?
)








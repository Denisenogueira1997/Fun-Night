package com.example.movienight.dto

import com.google.gson.annotations.SerializedName

data class SeriesResponse(
    val results: List<Series>
)

data class Series(
    val id: Int,
    val name: String,
    val poster_path: String?,
    @SerializedName("vote_average") val voteAverage: Float,
    @SerializedName("vote_count") val voteCount: Int,
    val first_air_date: String?,
    @SerializedName("original_language") val original_language: String?,
    val overview: String?,
    val genre_ids: List<Int>?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null
)

data class SeriesDetailsResponse(
    val id: Int,
    val name: String,
    val overview: String?,
    val poster_path: String?,
    @SerializedName("vote_average") val voteAverage: Float,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("original_language") val originalLanguage: String?,
    val genres: List<Genre>?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?
)


data class SeriesContentRatingResponse(val results: List<SeriesRating>)

data class SeriesRating(
    val iso_3166_1: String,
    val rating: String
)





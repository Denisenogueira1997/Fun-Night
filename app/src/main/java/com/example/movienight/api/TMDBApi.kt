package com.example.movienight.api


import com.example.movienight.dto.ContentRatingsResponse
import com.example.movienight.dto.GenreResponse
import com.example.movienight.dto.MovieDetailsResponse
import com.example.movienight.dto.MovieResponse
import com.example.movienight.dto.ReleaseDatesResponse
import com.example.movienight.dto.SeriesDetailsResponse
import com.example.movienight.dto.SeriesResponse
import com.example.movienight.dto.WatchProvidersResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface TMDBApi {


    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "pt-BR",
        @Query("sort_by") sortBy: String = "vote_average.desc",
        @Query("vote_count.gte") voteCount: Int,
        @Query("vote_average.gte") minVote: Float,
        @Query("page") page: Int,
        @Query("without_genres") withoutGenres: String? = null
    ): MovieResponse

    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("api_key") apiKey: String, @Query("language") language: String = "pt-BR"
    ): GenreResponse

    @GET("movie/{movie_id}/release_dates")
    suspend fun getMovieReleaseDates(
        @Path("movie_id") movieId: Int, @Query("api_key") apiKey: String
    ): ReleaseDatesResponse


    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "pt-BR"
    ): MovieDetailsResponse


    @GET("discover/tv")
    suspend fun discoverSeries(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "pt-BR",
        @Query("sort_by") sortBy: String = "vote_average.desc",
        @Query("vote_count.gte") voteCount: Int,
        @Query("vote_average.gte") minVote: Float,
        @Query("page") page: Int,
        @Query("without_genres") withoutGenres: String? = null
    ): SeriesResponse

    @GET("genre/tv/list")
    suspend fun getSeriesGenres(
        @Query("api_key") apiKey: String, @Query("language") language: String = "pt-BR"
    ): GenreResponse

    @GET("tv/{tv_id}/content_ratings")
    suspend fun getSeriesContentRatings(
        @Path("tv_id") tvId: Int, @Query("api_key") apiKey: String
    ): ContentRatingsResponse


    @GET("tv/{tv_id}")
    suspend fun getSeriesDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "pt-BR"
    ): SeriesDetailsResponse


    @GET("movie/{movie_id}/watch/providers")
    suspend fun getWatchProviders(
        @Path("movie_id") movieId: Int, @Query("api_key") apiKey: String
    ): WatchProvidersResponse


}



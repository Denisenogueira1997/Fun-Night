package com.example.movienight.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movienight.BuildConfig
import com.example.movienight.dto.Movie
import com.example.movienight.dto.Provider
import com.example.movienight.remote.RetrofitInstance
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {
    private val apiKey = BuildConfig.TMDB_API_KEY

    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie

    private val _streamingMap = MutableStateFlow<Map<Int, List<Provider>>>(emptyMap())
    val streamingMap: StateFlow<Map<Int, List<Provider>>> = _streamingMap

    private val _ageWarning = MutableStateFlow<String?>(null)
    val ageWarning: StateFlow<String?> = _ageWarning

    private val _genres = MutableStateFlow<Map<Int, String>>(emptyMap())
    val genres: StateFlow<Map<Int, String>> = _genres

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchGenres()
    }

    private fun fetchGenres() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getMovieGenres(apiKey)
                _genres.value = response.genres.associate { it.id to it.name }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isTitleLatin(text: String?): Boolean {
        return !text.isNullOrBlank() && text.any { it.isLetter() && it.code < 256 }
    }

    fun fetchMovies(
        pagesToSearch: Int = 5,
        k: Float = 20f,
        minWeightedScore: Float = 7f,
        minVoteCount: Int = 30,
        maxAttempts: Int = 2
    ) {
        viewModelScope.launch {
            _selectedMovie.value = null
            _ageWarning.value = null
            _streamingMap.value = emptyMap()
            _isLoading.value = true

            var movieFound: Movie? = null
            var attempt = 0

            while (movieFound == null && attempt < maxAttempts) {
                attempt++

                try {
                    val firstResponse =
                        RetrofitInstance.api.discoverMovies(
                            apiKey = apiKey,
                            sortBy = "popularity.desc",
                            language = "pt-BR",
                            page = 1,
                            voteCount = minVoteCount,
                            minVote = 6f,
                            withoutGenres = null,
                            minReleaseDate = "1985-01-01"
                        )


                    val totalPages =
                        firstResponse.totalPages
                            .coerceAtMost(500)


                    val pages =
                        (1..totalPages)
                            .shuffled()
                            .take(pagesToSearch)
                    val responses = pages.map { page ->
                        async {
                            RetrofitInstance.api.discoverMovies(
                                apiKey = apiKey,
                                sortBy = "popularity.desc",
                                language = "pt-BR",
                                page = page,
                                voteCount = minVoteCount,
                                minVote = 6f,
                                withoutGenres = null,
                                minReleaseDate = "1985-01-01"
                            )
                        }
                    }.awaitAll()

                    val allResults = responses.flatMap { res ->
                        res.results.filter { m ->
                            val genres = m.genre_ids ?: emptyList()

                            val isOnlyDrama = genres.size == 1 && genres.contains(18)

                            val weightedScore =
                                (m.voteAverage * m.voteCount + 7f * k) / (m.voteCount + k)

                            weightedScore >= minWeightedScore &&
                                    isTitleLatin(m.title) &&
                                    !isOnlyDrama &&
                                    genres.none { it == 27 }
                        }

                    }.shuffled()

                    movieFound = allResults.firstOrNull()?.let { randomMovie ->
                        val details = RetrofitInstance.api.getMovieDetails(randomMovie.id, apiKey)
                        val releaseDates =
                            RetrofitInstance.api.getMovieReleaseDates(randomMovie.id, apiKey)
                        val brCert =
                            releaseDates.results.firstOrNull { it.iso_3166_1 == "BR" }?.release_dates?.firstOrNull { !it.certification.isNullOrEmpty() }?.certification
                                ?: releaseDates.results.firstOrNull { it.release_dates.isNotEmpty() }?.release_dates?.firstOrNull { !it.certification.isNullOrEmpty() }?.certification

                        _ageWarning.value = brCert

                        val movieWithRuntime = randomMovie.copy(runtime = details.runtime)
                        fetchWatchProviders(movieWithRuntime.id)
                        movieWithRuntime
                    }

                    _selectedMovie.value = movieFound

                } catch (e: Exception) {
                    e.printStackTrace()
                    _ageWarning.value = null
                }
            }

            _isLoading.value = false
        }
    }

    fun fetchMoviesWithSelectedStreaming(
        selectedProviderIds: List<Int> = listOf(49, 118, 119, 531),
        pagesToSearch: Int = 5,
        k: Float = 20f,
        minWeightedScore: Float = 7f,
        minVoteCount: Int = 30,
        maxAttempts: Int = 2
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedMovie.value = null
            _ageWarning.value = null
            _streamingMap.value = emptyMap()

            var movieFound: Movie? = null
            var attempt = 0

            while (movieFound == null && attempt < maxAttempts) {
                attempt++

                try {
                    val firstResponse =
                        RetrofitInstance.api.discoverMovies(
                            apiKey = apiKey,
                            language = "pt-BR",
                            page = 1,
                            voteCount = minVoteCount,
                            minVote = 6f,
                            sortBy = "popularity.desc",
                            withoutGenres = "27",
                            minReleaseDate = "1950-01-01"
                        )


                    val totalPages =
                        firstResponse.totalPages
                            .coerceAtMost(500)


                    val pages =
                        (1..totalPages)
                            .shuffled()
                            .take(pagesToSearch)
                    val responses = pages.map { page ->
                        async {
                            RetrofitInstance.api.discoverMovies(
                                apiKey = apiKey,
                                language = "pt-BR",
                                page = page,
                                voteCount = minVoteCount ,
                                minVote = 0f,
                                sortBy = "popularity.desc",
                                withoutGenres = "27",
                                minReleaseDate = "1950-01-01"
                            )
                        }
                    }.awaitAll()

                    val allResults = responses.flatMap { res ->
                        res.results.filter { movie ->
                            val weightedScore =
                                (movie.voteAverage * movie.voteCount + 7f * k) / (movie.voteCount + k)
                            weightedScore >= minWeightedScore && movie.voteCount >= minVoteCount && isTitleLatin(movie.title)
                        }
                    }.shuffled()
                    val candidates = allResults.take(20)

                    val moviesWithProviders = candidates.map { movie ->

                        async {

                            try {

                                val providersResponse =
                                    RetrofitInstance.api.getWatchProviders(
                                        movie.id,
                                        apiKey
                                    )

                                val br =
                                    providersResponse.results["BR"]

                                val allProviders =
                                    mutableListOf<Provider>()


                                br?.flatrate?.forEach {
                                    allProviders.add(
                                        it.copy(type = "flatrate")
                                    )
                                }


                                br?.rent?.forEach {
                                    allProviders.add(
                                        it.copy(type = "rent")
                                    )
                                }


                                br?.buy?.forEach {
                                    allProviders.add(
                                        it.copy(type = "buy")
                                    )
                                }


                                if (
                                    allProviders.any {
                                        it.provider_id in selectedProviderIds
                                    }
                                ) {

                                    movie to allProviders

                                } else {

                                    null
                                }


                            } catch (e: Exception) {

                                null
                            }

                        }

                    }.awaitAll()


                    val selected =
                        moviesWithProviders
                            .filterNotNull()
                            .firstOrNull()


                    selected?.let { (movie, providers) ->


                        val details =
                            RetrofitInstance.api.getMovieDetails(
                                movie.id,
                                apiKey
                            )


                        val movieWithRuntime =
                            movie.copy(
                                runtime = details.runtime
                            )


                        _selectedMovie.value =
                            movieWithRuntime


                        _streamingMap.value =
                            mapOf(
                                movie.id to providers
                            )


                        val brCert =
                            try {

                                RetrofitInstance.api
                                    .getMovieReleaseDates(
                                        movie.id,
                                        apiKey
                                    )
                                    .results
                                    .find {
                                        it.iso_3166_1 == "BR"
                                    }
                                    ?.release_dates
                                    ?.firstOrNull()
                                    ?.certification

                            } catch (e: Exception) {

                                null
                            }


                        _ageWarning.value =
                            when {

                                brCert.isNullOrEmpty() ->
                                    null

                                brCert.equals(
                                    "L",
                                    ignoreCase = true
                                ) ->
                                    "L"

                                brCert.toIntOrNull() != null ->
                                    brCert

                                else ->
                                    brCert
                            }


                        movieFound =
                            movieWithRuntime
                    }


                } catch (e: Exception) {

                    e.printStackTrace()
                    _ageWarning.value = null
                }
            }


            _isLoading.value = false
        }
    }


    private suspend fun fetchWatchProviders(movieId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getWatchProviders(movieId, apiKey)
                val br = response.results["BR"]

                val all = mutableListOf<Provider>()
                br?.flatrate?.forEach { all.add(it.copy(type = "flatrate")) }
                br?.rent?.forEach { all.add(it.copy(type = "rent")) }
                br?.buy?.forEach { all.add(it.copy(type = "buy")) }

                _streamingMap.value =
                    _streamingMap.value + (movieId to (all.ifEmpty { emptyList() }))
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    _streamingMap.value = _streamingMap.value + (movieId to emptyList())
                } else {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _streamingMap.value = _streamingMap.value + (movieId to emptyList())
            }
        }
    }

    fun clearMovie() {
        _selectedMovie.value = null
        _ageWarning.value = null
        _streamingMap.value = emptyMap()
    }
}

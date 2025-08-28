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
        k: Float = 30f,
        minWeightedScore: Float = 7f,
        minVoteCount: Int = 10,
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
                    val pages = (1..500).shuffled().take(pagesToSearch)
                    val responses = pages.map { page ->
                        async {
                            RetrofitInstance.api.discoverMovies(
                                apiKey = apiKey,
                                language = "pt-BR",
                                page = page,
                                voteCount = minVoteCount,
                                minVote = 0f,
                                withoutGenres = null
                            )
                        }
                    }.awaitAll()

                    val allResults = responses.flatMap { res ->
                        res.results.filter { m ->
                            val weightedScore =
                                (m.voteAverage * m.voteCount + 7f * k) / (m.voteCount + k)
                            weightedScore >= minWeightedScore && isTitleLatin(m.title) && m.genre_ids?.none { it == 27 } == true
                        }
                    }.shuffled()

                    movieFound = allResults.firstOrNull()?.let { randomMovie ->
                        val details = RetrofitInstance.api.getMovieDetails(randomMovie.id, apiKey)
                        val movieWithRuntime = randomMovie.copy(runtime = details.runtime)
                        fetchWatchProviders(movieWithRuntime.id)
                        movieWithRuntime
                    }

                    _selectedMovie.value = movieFound

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _isLoading.value = false
        }
    }

    fun fetchMoviesWithSelectedStreaming(
        selectedProviderIds: List<Int> = listOf(119, 13, 49, 118, 137),
        pagesToSearch: Int = 5,
        k: Float = 30f,
        minWeightedScore: Float = 7f,
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
                    val pages = (1..500).shuffled().take(pagesToSearch)
                    val responses = pages.map { page ->
                        async {
                            RetrofitInstance.api.discoverMovies(
                                apiKey = apiKey,
                                language = "pt-BR",
                                page = page,
                                voteCount = 1,
                                minVote = 0f,
                                sortBy = "popularity.desc",
                                withoutGenres = "27"
                            )
                        }
                    }.awaitAll()

                    val allResults = responses.flatMap { res ->
                        res.results.filter { movie ->
                            val weightedScore =
                                (movie.voteAverage * movie.voteCount + 7f * k) / (movie.voteCount + k)
                            weightedScore >= minWeightedScore && isTitleLatin(movie.title)
                        }
                    }.shuffled()

                    for (movie in allResults) {
                        val details = RetrofitInstance.api.getMovieDetails(movie.id, apiKey)
                        val providersResponse =
                            RetrofitInstance.api.getWatchProviders(movie.id, apiKey)

                        val br = providersResponse.results["BR"]
                        val allProviders = mutableListOf<Provider>()
                        br?.flatrate?.forEach { allProviders.add(it.copy(type = "flatrate")) }
                        br?.rent?.forEach { allProviders.add(it.copy(type = "rent")) }
                        br?.buy?.forEach { allProviders.add(it.copy(type = "buy")) }

                        if (allProviders.any { it.provider_id in selectedProviderIds }) {
                            val movieWithRuntime = movie.copy(runtime = details.runtime)
                            _selectedMovie.value = movieWithRuntime
                            _streamingMap.value = mapOf(movie.id to allProviders)

                            val brCert = RetrofitInstance.api.getMovieReleaseDates(
                                movie.id,
                                apiKey
                            ).results.find { it.iso_3166_1 == "BR" }?.release_dates?.firstOrNull()?.certification

                            _ageWarning.value = when {
                                brCert.isNullOrEmpty() -> null
                                brCert.toIntOrNull() != null && brCert.toInt() > 16 -> "Filme indicado para maiores de $brCert anos."
                                else -> "Classificação: $brCert"
                            }

                            movieFound = movieWithRuntime
                            break
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    _ageWarning.value = null
                }
            }

            _isLoading.value = false
        }
    }

    private fun fetchWatchProviders(movieId: Int) {
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

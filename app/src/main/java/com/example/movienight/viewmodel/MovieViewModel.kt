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


    private fun isTitleLatin(text: String?): Boolean {
        return !text.isNullOrBlank() && text.any { it.isLetter() && it.code < 256 }
    }

    private val _genres = MutableStateFlow<Map<Int, String>>(emptyMap())
    val genres: StateFlow<Map<Int, String>> = _genres

    private val _availableProviders = MutableStateFlow<List<Provider>>(emptyList())
    val availableProviders: StateFlow<List<Provider>> = _availableProviders

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

    fun fetchMovies(pagesToSearch: Int = 5) {
        viewModelScope.launch {
            _selectedMovie.value = null
            _ageWarning.value = null

            try {
                val pages = (1..500).shuffled().take(pagesToSearch)
                val responses = pages.map { page ->
                    async {
                        RetrofitInstance.api.discoverMovies(
                            apiKey = apiKey,
                            language = "pt-BR",
                            page = page,
                            voteCount = 30,
                            minVote = 7f,
                            withoutGenres = null
                        )
                    }
                }.awaitAll()

                val allResults = responses.flatMap { res ->
                    res.results.filter { m ->
                        m.genre_ids?.none { it == 27 } == true &&
                                isTitleLatin(m.title)
                    }
                }

                if (allResults.isNotEmpty()) {
                    val randomMovie = allResults.random()

                    val details = RetrofitInstance.api.getMovieDetails(randomMovie.id, apiKey)
                    val movieWithRuntime = randomMovie.copy(runtime = details.runtime)
                    _selectedMovie.value = movieWithRuntime

                    fetchWatchProviders(movieWithRuntime.id)


                    val releaseDates =
                        RetrofitInstance.api.getMovieReleaseDates(randomMovie.id, apiKey)
                    val brCert = releaseDates.results
                        .find { it.iso_3166_1 == "BR" }
                        ?.release_dates
                        ?.firstOrNull()
                        ?.certification

                    if (!brCert.isNullOrEmpty() && brCert.toIntOrNull() != null && brCert.toInt() > 16) {
                        _ageWarning.value = "Filme indicado para maiores de $brCert anos."
                    } else {
                        _ageWarning.value = null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _ageWarning.value = null
            }
        }
    }
    fun fetchMoviesWithStreaming(pagesToSearch: Int = 5) {
        viewModelScope.launch {
            _selectedMovie.value = null
            _ageWarning.value = null

            try {
                val pages = (1..500).shuffled().take(pagesToSearch)
                val responses = pages.map { page ->
                    async {
                        RetrofitInstance.api.discoverMovies(
                            apiKey = apiKey,
                            language = "pt-BR",
                            page = page,
                            voteCount = 30,
                            minVote = 7f,
                            withoutGenres = "27"
                        )
                    }
                }.awaitAll()

                val allResults = responses.flatMap { res ->
                    res.results.filter { m ->
                        m.genre_ids?.none { it == 27 } == true &&
                                isTitleLatin(m.title)
                    }
                }

                val movieWithStreaming = allResults.shuffled().firstNotNullOfOrNull { movie ->
                    val providersResponse = RetrofitInstance.api.getWatchProviders(movie.id, apiKey)
                    val br = providersResponse.results["BR"]

                    val allProviders = mutableListOf<Provider>()
                    br?.flatrate?.forEach { allProviders.add(it.copy(type = "flatrate")) }
                    br?.rent?.forEach { allProviders.add(it.copy(type = "rent")) }
                    br?.buy?.forEach { allProviders.add(it.copy(type = "buy")) }

                    return@firstNotNullOfOrNull if (allProviders.isNotEmpty()) {
                        val details = RetrofitInstance.api.getMovieDetails(movie.id, apiKey)
                        val movieWithRuntime = movie.copy(runtime = details.runtime)
                        _streamingMap.value = mapOf(movie.id to allProviders)
                        movieWithRuntime
                    } else {
                        null
                    }
                }

                movieWithStreaming?.let { movie ->
                    _selectedMovie.value = movie

                    val releaseDates =
                        RetrofitInstance.api.getMovieReleaseDates(movie.id, apiKey)
                    val brCert = releaseDates.results
                        .find { it.iso_3166_1 == "BR" }
                        ?.release_dates
                        ?.firstOrNull()
                        ?.certification

                    if (!brCert.isNullOrEmpty() && brCert.toIntOrNull() != null && brCert.toInt() > 16) {
                        _ageWarning.value = "Filme indicado para maiores de $brCert anos."
                    } else {
                        _ageWarning.value = null
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _ageWarning.value = null
            }
        }
    }

    fun fetchMoviesWithSelectedStreaming(selectedProviderIds: List<Int>, pagesToSearch: Int = 5) {
        viewModelScope.launch {
            _selectedMovie.value = null
            _ageWarning.value = null

            try {
                val pages = (1..500).shuffled().take(pagesToSearch)
                val responses = pages.map { page ->
                    async {
                        RetrofitInstance.api.discoverMovies(
                            apiKey = apiKey,
                            language = "pt-BR",
                            page = page,
                            voteCount = 30,
                            minVote = 7f,
                            withoutGenres = "18,27"
                        )
                    }
                }.awaitAll()

                val allResults = responses.flatMap { res ->
                    res.results.filter { m ->
                        m.genre_ids?.none { it == 18 || it == 27 } == true &&
                                isTitleLatin(m.title)
                    }
                }

                val filteredMovie = allResults.shuffled().firstNotNullOfOrNull { movie ->
                    val providersResponse = RetrofitInstance.api.getWatchProviders(movie.id, apiKey)
                    val br = providersResponse.results["BR"]

                    val allProviders = mutableListOf<Provider>()
                    br?.flatrate?.forEach { allProviders.add(it.copy(type = "flatrate")) }
                    br?.rent?.forEach { allProviders.add(it.copy(type = "rent")) }
                    br?.buy?.forEach { allProviders.add(it.copy(type = "buy")) }

                    val hasSelectedProvider = allProviders.any { it.provider_id in selectedProviderIds }

                    if (hasSelectedProvider) {
                        val details = RetrofitInstance.api.getMovieDetails(movie.id, apiKey)
                        val movieWithRuntime = movie.copy(runtime = details.runtime)
                        _streamingMap.value = mapOf(movie.id to allProviders)
                        movieWithRuntime
                    } else {
                        null
                    }
                }

                filteredMovie?.let { movie ->
                    _selectedMovie.value = movie

                    val releaseDates =
                        RetrofitInstance.api.getMovieReleaseDates(movie.id, apiKey)
                    val brCert = releaseDates.results
                        .find { it.iso_3166_1 == "BR" }
                        ?.release_dates
                        ?.firstOrNull()
                        ?.certification

                    if (!brCert.isNullOrEmpty() && brCert.toIntOrNull() != null && brCert.toInt() > 16) {
                        _ageWarning.value = "Filme indicado para maiores de $brCert anos."
                    } else {
                        _ageWarning.value = null
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _ageWarning.value = null
            }
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

                _streamingMap.value = _streamingMap.value + (movieId to all)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearMovie() {
        _selectedMovie.value = null
        _ageWarning.value = null
        _streamingMap.value = emptyMap()
    }

}

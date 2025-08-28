package com.example.movienight.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movienight.BuildConfig
import com.example.movienight.dto.Provider
import com.example.movienight.dto.Series
import com.example.movienight.remote.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SeriesViewModel : ViewModel() {

    private val apiKey = BuildConfig.TMDB_API_KEY

    private val _selectedSeries = MutableStateFlow<Series?>(null)
    val selectedSeries: StateFlow<Series?> = _selectedSeries

    private val _streamingMap = MutableStateFlow<Map<Int, List<Provider>>>(emptyMap())
    val streamingMap: StateFlow<Map<Int, List<Provider>>> = _streamingMap

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
                val response = RetrofitInstance.api.getSeriesGenres(apiKey)
                _genres.value = response.genres.associate { it.id to it.name }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isTitleLatin(text: String?): Boolean {
        return !text.isNullOrBlank() && text.any { it.isLetter() && it.code < 256 }
    }

    fun fetchSeries(
        pagesToSearch: Int = 5,
        k: Float = 30f,
        minWeightedScore: Float = 7f,
        minVoteCount: Int = 10,
        maxAttempts: Int = 2
    ) {
        viewModelScope.launch {
            _selectedSeries.value = null
            _streamingMap.value = emptyMap()
            _isLoading.value = true

            var seriesFound: Series? = null
            var attempt = 0

            while (seriesFound == null && attempt < maxAttempts) {
                attempt++

                try {
                    val pages = (1..500).shuffled().take(pagesToSearch)
                    val allResults = mutableListOf<Series>()

                    for (page in pages) {
                        val response = RetrofitInstance.api.discoverSeries(
                            apiKey = apiKey,
                            language = "pt-BR",
                            page = page,
                            voteCount = minVoteCount,
                            minVote = 0f,
                            withoutGenres = null
                        )
                        val filtered = response.results.filter { s ->
                            val weightedScore =
                                (s.voteAverage * s.voteCount + 7f * k) / (s.voteCount + k)
                            weightedScore >= minWeightedScore && s.voteCount >= minVoteCount && isTitleLatin(
                                s.name
                            ) && s.genre_ids?.none { it == 16 } == true
                        }
                        allResults.addAll(filtered)
                    }

                    allResults.shuffle()

                    val s = allResults.firstOrNull()
                    s?.let { serie ->
                        val details = RetrofitInstance.api.getSeriesDetails(serie.id, apiKey)
                        val seriesWithDetails = Series(
                            id = details.id,
                            name = details.name,
                            poster_path = details.poster_path,
                            voteAverage = details.voteAverage,
                            voteCount = details.voteCount,
                            first_air_date = details.firstAirDate,
                            original_language = details.originalLanguage,
                            overview = details.overview,
                            genre_ids = details.genres?.map { it.id } ?: emptyList(),
                            numberOfSeasons = details.numberOfSeasons)
                        _selectedSeries.value = seriesWithDetails


                        try {
                            val providersResponse =
                                RetrofitInstance.api.getWatchProviders(serie.id, apiKey)
                            val br = providersResponse.results["BR"]
                            val allProviders = mutableListOf<Provider>()
                            br?.flatrate?.forEach { allProviders.add(it.copy(type = "flatrate")) }
                            br?.rent?.forEach { allProviders.add(it.copy(type = "rent")) }
                            br?.buy?.forEach { allProviders.add(it.copy(type = "buy")) }

                            _streamingMap.value = mapOf(seriesWithDetails.id to allProviders)

                        } catch (e: Exception) {
                            e.printStackTrace()
                            _streamingMap.value = mapOf(seriesWithDetails.id to emptyList())
                        }

                        seriesFound = seriesWithDetails
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _isLoading.value = false
        }
    }

    fun clearSeries() {
        _selectedSeries.value = null
        _streamingMap.value = emptyMap()
    }
}

package com.example.movienight.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movienight.BuildConfig
import com.example.movienight.dto.Provider
import com.example.movienight.dto.Series
import com.example.movienight.remote.RetrofitInstance
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SeriesViewModel : ViewModel() {
    private val apiKey = BuildConfig.TMDB_API_KEY

    private val _selectedSeries = MutableStateFlow<Series?>(null)
    val selectedSeries: StateFlow<Series?> = _selectedSeries

    private val _streamingMap = MutableStateFlow<Map<Int, List<Provider>>>(emptyMap())
    val streamingMap: StateFlow<Map<Int, List<Provider>>> = _streamingMap

    private val _ageWarning = MutableStateFlow<String?>(null)
    val ageWarning: StateFlow<String?> = _ageWarning

    private fun isTitleLatin(text: String?): Boolean {
        return !text.isNullOrBlank() && text.any { it.isLetter() && it.code < 256 }
    }

    private val _genres = MutableStateFlow<Map<Int, String>>(emptyMap())
    val genres: StateFlow<Map<Int, String>> = _genres

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

    fun fetchSeries(pagesToSearch: Int = 5) {
        viewModelScope.launch {
            _selectedSeries.value = null
            _ageWarning.value = null

            try {
                val pages = (1..500).shuffled().take(pagesToSearch)
                val responses = pages.map { page ->
                    async {
                        RetrofitInstance.api.discoverSeries(
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
                    res.results.filter { s ->
                        s.genre_ids?.none {  it == 16 } == true &&
                                isTitleLatin(s.name)
                    }
                }

                if (allResults.isNotEmpty()) {
                    val randomSeries = allResults.random()
                    val details = RetrofitInstance.api.getSeriesDetails(randomSeries.id, apiKey)

                    _selectedSeries.value = Series(
                        id = details.id,
                        name = details.name,
                        poster_path = details.poster_path,
                        voteAverage = details.voteAverage,
                        voteCount = details.voteCount,
                        first_air_date = details.firstAirDate,
                        original_language = details.originalLanguage,
                        overview = details.overview,
                        genre_ids = details.genres?.map { it.id } ?: emptyList(),
                        numberOfSeasons = details.numberOfSeasons
                    )

                    fetchWatchProviders(randomSeries.id)


                    val ratings =
                        RetrofitInstance.api.getSeriesContentRatings(randomSeries.id, apiKey)
                    val brRating = ratings.results.find { it.iso_3166_1 == "BR" }?.rating

                    if (!brRating.isNullOrEmpty() && brRating.toIntOrNull() != null && brRating.toInt() > 16) {
                        _ageWarning.value = "Série indicada para maiores de $brRating anos."
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

    private fun fetchWatchProviders(seriesId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getWatchProviders(seriesId, apiKey)
                val br = response.results["BR"]

                val all = mutableListOf<Provider>()
                br?.flatrate?.forEach { all.add(it.copy(type = "flatrate")) }
                br?.rent?.forEach { all.add(it.copy(type = "rent")) }
                br?.buy?.forEach { all.add(it.copy(type = "buy")) }

                _streamingMap.value = _streamingMap.value + (seriesId to all)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearSeries() {
        _selectedSeries.value = null
        _ageWarning.value = null
        _streamingMap.value = emptyMap()
    }

}

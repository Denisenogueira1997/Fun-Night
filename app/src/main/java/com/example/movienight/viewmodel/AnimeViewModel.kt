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

class AnimeViewModel : ViewModel() {
    private val apiKey = BuildConfig.TMDB_API_KEY

    private val _selectedAnime = MutableStateFlow<Series?>(null)
    val selectedAnime: StateFlow<Series?> = _selectedAnime

    private val _streamingMap = MutableStateFlow<Map<Int, List<Provider>>>(emptyMap())
    val streamingMap: StateFlow<Map<Int, List<Provider>>> = _streamingMap

    private val _ageWarning = MutableStateFlow<String?>(null)
    val ageWarning: StateFlow<String?> = _ageWarning


    fun fetchAnime(pagesToSearch: Int = 5) {
        viewModelScope.launch {
            _selectedAnime.value = null
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
                            withoutGenres = "27"
                        )
                    }
                }.awaitAll()

                val allResults = responses.flatMap { res ->
                    res.results.filter { s ->
                        s.original_language == "ja" || s.genre_ids?.contains(16) == true
                    }
                }

                if (allResults.isNotEmpty()) {
                    val randomAnime = allResults.random()
                    val details = RetrofitInstance.api.getSeriesDetails(randomAnime.id, apiKey)

                    _selectedAnime.value = Series(
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

                    fetchWatchProviders(randomAnime.id)
                    val ratings =
                        RetrofitInstance.api.getSeriesContentRatings(randomAnime.id, apiKey)
                    val brCert = ratings.results.find { it.iso_3166_1 == "BR" }?.rating


                    if (!brCert.isNullOrEmpty() && brCert.toIntOrNull() != null && brCert.toInt() > 16) {
                        _ageWarning.value = " Anime indicado para maiores de $brCert anos."
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

    private fun fetchWatchProviders(animeId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getWatchProviders(animeId, apiKey)
                val br = response.results["BR"]

                val all = mutableListOf<Provider>()
                br?.flatrate?.forEach { all.add(it.copy(type = "flatrate")) }
                br?.rent?.forEach { all.add(it.copy(type = "rent")) }
                br?.buy?.forEach { all.add(it.copy(type = "buy")) }

                _streamingMap.value = _streamingMap.value + (animeId to all)
            } catch (e: Exception) {
                e.printStackTrace()

            }
        }
    }

    fun clearAnime() {
        _selectedAnime.value = null
        _ageWarning.value = null
        _streamingMap.value = emptyMap()
    }

}



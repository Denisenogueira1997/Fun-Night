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

class CartoonViewModel : ViewModel() {

    private val apiKey = BuildConfig.TMDB_API_KEY

    private val _selectedCartoon = MutableStateFlow<Series?>(null)
    val selectedCartoon: StateFlow<Series?> = _selectedCartoon

    private val _streamingMap = MutableStateFlow<Map<Int, List<Provider>>>(emptyMap())
    val streamingMap: StateFlow<Map<Int, List<Provider>>> = _streamingMap

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private fun isTitleLatin(text: String?): Boolean {
        return !text.isNullOrBlank() && text.any { it.isLetter() && it.code < 256 }
    }

    private val _ageWarning = MutableStateFlow<String?>(null)
    val ageWarning: StateFlow<String?> = _ageWarning

    fun fetchCartoon(
        pagesToSearch: Int = 10,
        k: Float = 15f,
        minWeightedScore: Float = 7f,
        minVoteCount: Int = 15,
        maxAttempts: Int = 3
    ) {
        viewModelScope.launch {
            _selectedCartoon.value = null
            _streamingMap.value = emptyMap()
            _ageWarning.value = null
            _isLoading.value = true

            var cartoonFound: Series? = null
            var attempt = 0

            while (cartoonFound == null && attempt < maxAttempts) {
                attempt++

                try {
                    val firstResponse = RetrofitInstance.api.discoverSeries(
                        apiKey = apiKey,
                        language = "pt-BR",
                        sortBy = "popularity.desc",
                        page = 1,
                        voteCount = minVoteCount,
                        minVote = 6f,
                        withoutGenres = null
                    )

                    val totalPages = firstResponse.totalPages.coerceAtMost(500)

                    val pages = (1..totalPages)
                        .shuffled()
                        .take(pagesToSearch)
                    val allResults = mutableListOf<Series>()

                    for (page in pages) {
                        val response = RetrofitInstance.api.discoverSeries(
                            apiKey = apiKey,
                            sortBy = "popularity.desc",
                            language = "pt-BR",
                            page = page,
                            voteCount = minVoteCount,
                            minVote = 6f,
                            withoutGenres = "27"
                        )
                        val filtered = response.results.filter { s ->
                            val weightedScore =
                                (s.voteAverage * s.voteCount + 7f * k) / (s.voteCount + k)
                            weightedScore >= minWeightedScore &&
                                    s.voteCount >= minVoteCount
                                    && (s.original_language != "ja"
                                    && s.originCountry?.contains("JP") != true)
                                    && s.genre_ids?.contains(16) == true &&
                                    isTitleLatin(s.name)
                        }
                        allResults.addAll(filtered)
                    }

                    allResults.shuffle()

                    val s = allResults.firstOrNull()
                    s?.let { serie ->
                        val details = RetrofitInstance.api.getSeriesDetails(serie.id, apiKey)

                        val brCert = try {
                            val ratings =
                                RetrofitInstance.api.getSeriesContentRatings(serie.id, apiKey)
                            ratings.results.firstOrNull { it.iso_3166_1 == "BR" }?.rating
                                ?: ratings.results.firstOrNull()?.rating
                        } catch (e: Exception) {
                            null
                        }
                        setAgeRating(brCert)
                        val cartoonWithDetails = Series(
                            id = details.id,
                            name = details.name,
                            poster_path = details.poster_path,
                            voteAverage = details.voteAverage,
                            voteCount = details.voteCount,
                            first_air_date = details.firstAirDate,
                            original_language = details.originalLanguage,
                            originCountry = details.originCountry,
                            overview = details.overview,
                            genre_ids = details.genres?.map { it.id } ?: emptyList(),
                            numberOfSeasons = details.numberOfSeasons)
                        _selectedCartoon.value = cartoonWithDetails

                        try {
                            val providersResponse =
                                RetrofitInstance.api.getWatchProviders(serie.id, apiKey)
                            val br = providersResponse.results["BR"]
                            val allProviders = mutableListOf<Provider>()
                            br?.flatrate?.forEach { allProviders.add(it.copy(type = "flatrate")) }
                            br?.rent?.forEach { allProviders.add(it.copy(type = "rent")) }
                            br?.buy?.forEach { allProviders.add(it.copy(type = "buy")) }

                            _streamingMap.value = mapOf(cartoonWithDetails.id to allProviders)

                        } catch (e: Exception) {
                            e.printStackTrace()
                            _streamingMap.value = mapOf(cartoonWithDetails.id to emptyList())
                        }

                        cartoonFound = cartoonWithDetails
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    _ageWarning.value = null
                }
            }

            _isLoading.value = false
        }
    }

    fun clearCartoon() {
        _selectedCartoon.value = null
        _streamingMap.value = emptyMap()
    }

    private fun setAgeRating(brCert: String?) {
        _ageWarning.value = when {
            brCert.isNullOrEmpty() -> null
            brCert.equals("L", ignoreCase = true) -> "L"
            brCert.toIntOrNull() != null -> brCert
            else -> brCert
        }
    }
}
package com.example.movienight.view


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movienight.view.components.MovieItem
import com.example.movienight.view.components.SeriesItem
import com.example.movienight.view.components.TmdbAttribution
import com.example.movienight.view.components.WarningCard
import com.example.movienight.viewmodel.AnimeViewModel
import com.example.movienight.viewmodel.MovieViewModel
import com.example.movienight.viewmodel.SeriesViewModel

@Composable
fun MainScreen(
    movieViewModel: MovieViewModel = viewModel(),
    seriesViewModel: SeriesViewModel = viewModel(),
    animeViewModel: AnimeViewModel = viewModel()
) {

    val movieGenres by movieViewModel.genres.collectAsState()
    val seriesGenres by seriesViewModel.genres.collectAsState()

    val movie by movieViewModel.selectedMovie.collectAsState()
    val movieProviders by movieViewModel.streamingMap.collectAsState()
    val movieAgeWarning by movieViewModel.ageWarning.collectAsState()

    val series by seriesViewModel.selectedSeries.collectAsState()
    val seriesProviders by seriesViewModel.streamingMap.collectAsState()
    val seriesAgeWarning by seriesViewModel.ageWarning.collectAsState()

    val anime by animeViewModel.selectedAnime.collectAsState()
    val animeProviders by animeViewModel.streamingMap.collectAsState()


    val scrollState = rememberScrollState()



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)
            .systemBarsPadding()
            .verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(
            onClick = {
                seriesViewModel.clearSeries()
                animeViewModel.clearAnime()
                movieViewModel.fetchMovies()
            },

            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)


        ) {
            Text("🎬 Sortear Filme")
        }

        Spacer(modifier = Modifier.height(16.dp))


        Button(
            onClick = {
                movieViewModel.clearMovie()
                animeViewModel.clearAnime()
                seriesViewModel.fetchSeries()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("📺 Sortear Série")
        }

        Spacer(modifier = Modifier.height(16.dp))


        Button(
            onClick = {
                movieViewModel.clearMovie()
                seriesViewModel.clearSeries()
                animeViewModel.fetchAnime()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("🎌 Sortear Anime ou Desenho")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                seriesViewModel.clearSeries()
                animeViewModel.clearAnime()
                movieViewModel.fetchMoviesWithStreaming()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("🎬 Sortear Filme com Streaming")
        }


        movieAgeWarning?.let { WarningCard(it) }
        seriesAgeWarning?.let { WarningCard(it) }


        movie?.let {
            MovieItem(
                movie = it, providers = movieProviders[it.id], genreMap = movieGenres
            )
        }

        series?.let {
            SeriesItem(
                series = it, providers = seriesProviders[it.id], genreMap = seriesGenres
            )
        }

        anime?.let {
            SeriesItem(
                series = it, providers = animeProviders[it.id], genreMap = seriesGenres
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        TmdbAttribution()

    }
}




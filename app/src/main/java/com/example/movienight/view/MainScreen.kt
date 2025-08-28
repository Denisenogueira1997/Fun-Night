package com.example.movienight.view


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val isLoading by movieViewModel.isLoading.collectAsState()

    val series by seriesViewModel.selectedSeries.collectAsState()
    val seriesProviders by seriesViewModel.streamingMap.collectAsState()
    val isLoadingSeries by seriesViewModel.isLoading.collectAsState()

    val anime by animeViewModel.selectedAnime.collectAsState()
    val isLoadingAnime by animeViewModel.isLoading.collectAsState()
    val animeProviders by animeViewModel.streamingMap.collectAsState()

    val isLoadingAny = isLoading || isLoadingSeries || isLoadingAnime

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tela Inicial", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .consumeWindowInsets(WindowInsets.systemBars),
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .background(MaterialTheme.colorScheme.onBackground)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        seriesViewModel.clearSeries()
                        animeViewModel.clearAnime()
                        movieViewModel.fetchMovies()
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("🎬 Sortear Filme")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        movieViewModel.clearMovie()
                        animeViewModel.clearAnime()
                        seriesViewModel.fetchSeries()
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("📺 Sortear Série")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        movieViewModel.clearMovie()
                        seriesViewModel.clearSeries()
                        animeViewModel.fetchAnime()
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("🎌 Sortear Anime ou Desenho")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        seriesViewModel.clearSeries()
                        animeViewModel.clearAnime()
                        movieViewModel.fetchMoviesWithSelectedStreaming()
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("🎬 Sortear Filme com Streaming")
                }

                Spacer(modifier = Modifier.height(16.dp))

                movieAgeWarning?.let { WarningCard(it) }

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
            if (isLoadingAny) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
package com.example.movienight.view.components


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.example.movienight.R
import com.example.movienight.dto.Movie
import com.example.movienight.dto.Provider


@Composable
fun MovieItem(
    movie: Movie,
    providers: List<Provider>?,
    genreMap: Map<Int, String> = emptyMap(),
    ageWarning: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Column() {
                movie.poster_path?.let { posterPath ->
                    Image(
                        painter = rememberImagePainter("https://image.tmdb.org/t/p/w185$posterPath"),
                        contentDescription = "Poster",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(100.dp)
                            .height(150.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.tmdb),
                    contentDescription = "TMDB Logo",
                    modifier = Modifier
                        .height(50.dp)
                        .width(100.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = movie.title)

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Nota: ${movie.voteAverage}")

                Spacer(modifier = Modifier.height(8.dp))

                val ano =
                    movie.release_date?.takeIf { it.length >= 4 }?.substring(0, 4) ?: "Desconhecido"
                Text(text = "Ano: $ano")

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Idioma original: ${movie.original_language?.uppercase() ?: "Desconhecido"}")

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Total de votos: ${movie.voteCount ?: 0}")

                Spacer(modifier = Modifier.height(12.dp))

                val genreNames = movie.genre_ids?.mapNotNull { genreMap[it] }?.joinToString(",")
                    ?: "Desconhecido"
                Text(text = "Gêneros: $genreNames")

                Spacer(modifier = Modifier.height(8.dp))

                if (providers.isNullOrEmpty()) {
                    Text("Nenhuma plataforma disponível para streaming, aluguel ou compra.")
                } else {
                    StreamingSection(providers)
                }


                Spacer(modifier = Modifier.height(8.dp))

                val sinopse = movie.overview
                Text(
                    text = if (!sinopse.isNullOrBlank()) sinopse else "Sinopse não disponível"
                )

                Spacer(modifier = Modifier.height(8.dp))

                movie.runtime?.let {
                    Text("Duração: $it min")
                } ?: Text("Duração não disponível")

                WarningCard(
                    ageWarning ?: "Classificação indicativa não disponível",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

        }
    }
}


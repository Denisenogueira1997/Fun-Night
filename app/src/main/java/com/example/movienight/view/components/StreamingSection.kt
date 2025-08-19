package com.example.movienight.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.example.movienight.dto.Provider

@Composable
fun StreamingSection(providers: List<Provider>) {
    val flatrate = providers.filter { it.type == "flatrate" }
    val rent = providers.filter { it.type == "rent" }
    val buy = providers.filter { it.type == "buy" }

    if (flatrate.isNotEmpty()) {
        Text("Disponível para streaming:")
        flatrate.forEach {
            ProviderRow(it)
        }
    }

    if (rent.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Disponível para aluguel:")
        rent.forEach {
            ProviderRow(it)
        }
    }

    if (buy.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Disponível para compra:")
        buy.forEach {
            ProviderRow(it)
        }
    }

    if (flatrate.isEmpty() && rent.isEmpty() && buy.isEmpty()) {
        Text("Nenhuma plataforma disponível para streaming, aluguel ou compra.")
    }
}

@Composable
fun ProviderRow(provider: Provider) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)
    ) {
        provider.logo_path?.let { path ->
            Image(
                painter = rememberImagePainter("https://image.tmdb.org/t/p/w45$path"),
                contentDescription = provider.provider_name,
                modifier = Modifier
                    .width(32.dp)
                    .height(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = provider.provider_name)

    }
}

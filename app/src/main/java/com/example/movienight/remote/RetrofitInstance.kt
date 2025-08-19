package com.example.movienight.remote


import com.example.movienight.api.TMDBApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private val loggingInterceptor: HttpLoggingInterceptor
        get() = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }

    private val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()

    private val retrofit by lazy {
        Retrofit.Builder().baseUrl("https://api.themoviedb.org/3/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
    }

    val api: TMDBApi by lazy {
        retrofit.create(TMDBApi::class.java)
    }
}

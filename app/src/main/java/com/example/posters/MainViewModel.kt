package com.example.posters

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Define states for our UI
sealed interface WallpaperUiState {
    data class Success(val wallpapers: List<Wallpaper>) : WallpaperUiState
    object Error : WallpaperUiState
    object Loading : WallpaperUiState
}

class MainViewModel : ViewModel() {

    var wallpaperUiState: WallpaperUiState by mutableStateOf(WallpaperUiState.Loading)
        private set

    init {
        fetchWallpapers()
    }

    private fun fetchWallpapers() {
        viewModelScope.launch {
            wallpaperUiState = WallpaperUiState.Loading
            try {
                // Create an OkHttpClient with timeouts
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()
                // Create a Retrofit instance with the OkHttpClient
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://posters-backend-ibn4.onrender.com/") // Your Render URL
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                // Create an instance of ApiService
                val apiService = retrofit.create(ApiService::class.java)

                // Make the simple network call.
                val newWallpapers = apiService.getWallpapers()
                wallpaperUiState = WallpaperUiState.Success(newWallpapers)

            } catch (e: Exception) {
                wallpaperUiState = WallpaperUiState.Error
            }
        }
    }
}
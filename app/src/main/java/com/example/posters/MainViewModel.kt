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
                // 1. Create a custom OkHttpClient with a longer timeout
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS) // Wait 60 seconds to connect
                    .readTimeout(60, TimeUnit.SECONDS)    // Wait 60 seconds for data
                    .writeTimeout(60, TimeUnit.SECONDS)   // Wait 60 seconds to send data
                    .build()

                // 2. Build Retrofit using our custom client
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://posters-backend-ibn4.onrender.com/") // Your Render URL
                    .client(okHttpClient) // <-- Add the custom client here
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(ApiService::class.java)

                // Make the network call
                wallpaperUiState = WallpaperUiState.Success(apiService.getWallpapers())

            } catch (e: Exception) {
                // Handle errors (e.g., no internet)
                wallpaperUiState = WallpaperUiState.Error
            }
        }
    }
}
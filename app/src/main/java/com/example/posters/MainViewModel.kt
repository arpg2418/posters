package com.example.posters

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
                // Create the Retrofit instance
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://posters-backend-ibn4.onrender.com/getWallpapers") // e.g., "https://posters-backend.onrender.com/"
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
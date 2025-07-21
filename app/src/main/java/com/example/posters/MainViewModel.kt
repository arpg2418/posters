package com.example.posters

import android.util.Log
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

// The UI state now holds a persistent list and loading flags.
data class WallpaperUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingNextPage: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false // To know when to stop fetching
)

class MainViewModel : ViewModel() {
    var uiState by mutableStateOf(WallpaperUiState())
        private set
    private var currentPage = 0
    private lateinit var apiService: ApiService

    init {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://posters-backend-ibn4.onrender.com/") // Your Render URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        loadNextPage() // Load the first page initially
    }

    fun loadNextPage() {
        // Prevent multiple requests at the same time or if we've reached the end
        if (uiState.isLoadingNextPage || uiState.endReached) return

        viewModelScope.launch {
            // Set the correct loading state
            uiState = if (currentPage == 0) {
                uiState.copy(isLoading = true)
            } else {
                uiState.copy(isLoadingNextPage = true)
            }

            try {
                Log.d("MainViewModel", "Starting network request for page: $currentPage")
                // Fetch the next page of wallpapers
                val newWallpapers = apiService.getWallpapers(page = currentPage)

                if (newWallpapers.isNotEmpty()) {
                    // Add the new wallpapers to our existing list
                    uiState = uiState.copy(
                        wallpapers = uiState.wallpapers + newWallpapers,
                        isLoading = false,
                        isLoadingNextPage = false
                    )
                    currentPage++ // Increment the page number for the next request
                } else {
                    // If we get an empty list, we've reached the end
                    uiState = uiState.copy(
                        isLoading = false,
                        isLoadingNextPage = false,
                        endReached = true
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to load page: $currentPage", e)
                uiState = uiState.copy(error = "Failed to load wallpapers.", isLoading = false, isLoadingNextPage = false)
            }
        }
    }
}
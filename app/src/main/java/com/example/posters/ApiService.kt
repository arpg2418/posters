package com.example.posters

import retrofit2.http.GET

interface ApiService {
    // We are back to a simple GET request with no body.
    @GET("getWallpapers")
    suspend fun getWallpapers(): List<Wallpaper>
}
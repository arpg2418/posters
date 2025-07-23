package com.example.posters

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    // We are back to a simple GET request with no body.
    @GET("getWallpapers")
    suspend fun getWallpapers(@Query("page") page: Int): List<Wallpaper>

    @GET("getWallpaperById")
    suspend fun getWallpaperById(@Query("id") id: String): Wallpaper
}
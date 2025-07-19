package com.example.posters

import retrofit2.http.GET

interface ApiService {
    @GET("getWallpapers") // This matches the endpoint name in your backend code
    suspend fun getWallpapers(): List<Wallpaper>
}
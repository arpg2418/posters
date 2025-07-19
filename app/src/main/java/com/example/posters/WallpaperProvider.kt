package com.example.posters

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

// It's best practice to keep the ContentProvider in its own file.
// This class is responsible for securely providing access to files
// stored in the app's private directory.
class WallpaperProvider : ContentProvider() {

    companion object {
        // This authority string must be unique and match what's in the AndroidManifest.xml
        private const val AUTHORITY = "com.example.posters.provider"
        private val BASE_URI = Uri.parse("content://$AUTHORITY")

        // A helper function to easily create a content URI for a given wallpaper ID.
        fun getUriForWallpaper(id: String): Uri = BASE_URI.buildUpon().appendPath(id).build()
    }

    override fun onCreate(): Boolean = true

    // This is the core method that the WallpaperManager calls.
    // It opens a readable file descriptor for the requested URI.
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val wallpaperId = uri.lastPathSegment ?: return null
        val context = context ?: return null

        // Create a file path pointing to the image we saved in our private directory.
        val file = File(context.filesDir, "$wallpaperId.jpg")

        // If the file exists, open it in read-only mode and return it.
        return if (file.exists()) {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
            null
        }
    }

    // The following methods are required by ContentProvider but are not needed
    // for this app's functionality, so they return null or 0.
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = "image/jpeg"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
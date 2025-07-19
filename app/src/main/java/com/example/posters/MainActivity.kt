package com.example.posters

import android.app.WallpaperManager
//import android.content.ContentProvider
//import android.content.ContentValues
import android.content.Context
//import android.database.Cursor
//import android.net.Uri
import android.os.Bundle
//import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.posters.ui.theme.WallpaperAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Data class defining the structure of a wallpaper object.
data class Wallpaper(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val fullUrl: String
)

// This is the main entry point of your app.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WallpaperAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    MainScreen()
                }
            }
        }
    }
}

// This composable orchestrates the main UI, including the top bar, grid, and overlay.
@Composable
fun MainScreen(viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var selectedWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    val uiState = viewModel.wallpaperUiState

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                // The top bar is now simpler, with search removed.
                HomeTopBar()
            },
            containerColor = Color(0xFF121212)
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                Spacer(modifier = Modifier.height(16.dp))

                // This block reacts to the state from the ViewModel (Loading, Success, Error).
                when (uiState) {
                    is WallpaperUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is WallpaperUiState.Success -> {
                        // The grid now directly uses the list from the success state.
                        WallpaperGrid(
                            wallpapers = uiState.wallpapers,
                            onWallpaperClick = { wallpaper -> selectedWallpaper = wallpaper }
                        )
                    }
                    is WallpaperUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Failed to load wallpapers.", color = Color.White)
                        }
                    }
                }
            }
        }

        // The overlay that appears when a wallpaper is selected.
        WallpaperOverlay(
            wallpaper = selectedWallpaper,
            onClose = { selectedWallpaper = null }
        )
    }
}

// The simplified top app bar that only displays the title.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar() {
    TopAppBar(
        title = {
            Text("Posters", color = Color.White, fontWeight = FontWeight.Bold)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

// The staggered grid that displays the wallpaper thumbnails.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperGrid(wallpapers: List<Wallpaper>, onWallpaperClick: (Wallpaper) -> Unit) {
    if (wallpapers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No wallpapers found.", color = Color.Gray)
        }
        return
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(wallpapers, key = { it.id }) { wallpaper ->
            WallpaperThumbnail(
                wallpaper = wallpaper,
                modifier = Modifier.animateItemPlacement(
                    tween(durationMillis = 500)
                ),
                onClick = { onWallpaperClick(wallpaper) }
            )
        }
    }
}

// A single wallpaper item in the grid.
@Composable
fun WallpaperThumbnail(wallpaper: Wallpaper, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        SubcomposeAsyncImage(
            model = wallpaper.thumbnailUrl,
            loading = {
                Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray))
            },
            contentDescription = wallpaper.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// The overlay UI with "View Full Resolution" and "Apply" buttons.
@Composable
fun WallpaperOverlay(wallpaper: Wallpaper?, onClose: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isFullScreen by remember { mutableStateOf(false) }

    LaunchedEffect(wallpaper) {
        if (wallpaper == null) {
            isFullScreen = false
        }
    }

    AnimatedVisibility(
        visible = wallpaper != null,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (isFullScreen) isFullScreen = false else onClose() }
                )
        ) {
            Box(
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            ) {
                AnimatedVisibility(
                    visible = wallpaper != null,
                    enter = scaleIn(spring(0.8f, 200f)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val imageModifier = if (isFullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().heightIn(max = 500.dp)

                        SubcomposeAsyncImage(
                            model = if (isFullScreen) wallpaper?.fullUrl else wallpaper?.thumbnailUrl,
                            loading = {
                                Image(
                                    painter = rememberAsyncImagePainter(model = wallpaper?.thumbnailUrl),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            },
                            contentDescription = "Selected Wallpaper",
                            contentScale = if (isFullScreen) ContentScale.Fit else ContentScale.Crop,
                            modifier = imageModifier
                                .clip(RoundedCornerShape(16.dp))
                                .animateContentSize(spring(0.8f, 300f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { /* Consume click to prevent closing overlay */ }
                                )
                        )

                        AnimatedVisibility(
                            visible = !isFullScreen,
                            enter = slideInVertically { it / 2 } + fadeIn(),
                            exit = slideOutVertically { it / 2 } + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier.padding(top = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(onClick = { isFullScreen = true }) {
                                    Text("View Full Resolution")
                                }
                                Button(onClick = {
                                    coroutineScope.launch {
                                        wallpaper?.let { setWallpaper(context, it) }
                                    }
                                }) {
                                    Text("Apply Wallpaper")
                                }
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = { if (isFullScreen) isFullScreen = false else onClose() },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }
    }
}

// This function now contains the full logic for downloading and applying the wallpaper.
private suspend fun setWallpaper(context: Context, wallpaper: Wallpaper) {
    val loader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(wallpaper.fullUrl)
        .allowHardware(false)
        .build()

    try {
        val result = (loader.execute(request) as SuccessResult).drawable
        val bitmap = (result as android.graphics.drawable.BitmapDrawable).bitmap

        val file = File(context.filesDir, "${wallpaper.id}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream)
        }

        val uri = WallpaperProvider.getUriForWallpaper(wallpaper.id)
        val wallpaperManager = WallpaperManager.getInstance(context)

        // Added a null check and .use block for safety and resource management.
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            wallpaperManager.setStream(inputStream)
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Wallpaper applied!", Toast.LENGTH_SHORT).show()
        }

    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to apply wallpaper.", Toast.LENGTH_SHORT).show()
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun MainScreenPreview() {
    WallpaperAppTheme {
        // MainScreen()
    }
}

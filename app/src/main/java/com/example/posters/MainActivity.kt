package com.example.posters

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput


// Data class defining the structure of a wallpaper object.
data class Wallpaper(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val previewUrl: String,
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
                            ShimmerLoadingGrid()
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

@Composable
fun ShimmerLoadingGrid() {
    // This transition creates the continuous shimmer animation
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-anim"
    )

    // A brush that creates the shimmering gradient effect
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    // A placeholder grid that mimics the real grid's layout
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(10) { // Show 10 placeholder items
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
            )
        }
    }
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
                modifier = Modifier.animateItem(
                    // Apply tween to placement animation (reordering)
                    placementSpec = tween(durationMillis = 500),
                    // Optionally, you can also define fade in/out animations
                    fadeInSpec = tween(durationMillis = 300), // Example for fade in
                    fadeOutSpec = tween(durationMillis = 300)  // Example for fade out
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
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // This effect resets everything when the overlay is closed
    LaunchedEffect(wallpaper) {
        if (wallpaper == null) {
            isFullScreen = false
            scale = 1f
            offset = Offset.Zero
        }
    }
    // This effect resets the zoom/pan when exiting full-screen mode
    LaunchedEffect(isFullScreen) {
        if (!isFullScreen) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    AnimatedVisibility(
        visible = wallpaper != null,
        enter = fadeIn(tween(425)),
        exit = fadeOut(tween(490))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
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
                            model = if (isFullScreen) wallpaper?.fullUrl else wallpaper?.previewUrl,

                            // loading Animation for Med and High-Res Images
                            loading = {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // The placeholder image
                                    Image(
                                        painter = rememberAsyncImagePainter(model = if (isFullScreen) wallpaper?.previewUrl else wallpaper?.thumbnailUrl),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // The progress bar aligned to the bottom
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .height(7.dp)
                                    )
                                }
                            },

                            contentDescription = "Selected Wallpaper",
                            contentScale = if (isFullScreen) ContentScale.Fit else ContentScale.Crop,
                            modifier = imageModifier
                                .clip(RoundedCornerShape(24.dp))
                                .graphicsLayer { // This applies the transformations
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .pointerInput(Unit) { // This detects the gestures
                                    if (isFullScreen) { // Only allow zooming in full-screen
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(1f, 4f) // Clamp zoom between 1x and 4x

                                            // This logic calculates the new offset based on the pan gesture
                                            val newOffset = offset + pan
                                            // This logic is a bit complex, but it prevents panning outside the image bounds
                                            val maxX = (size.width * (scale - 1)) / 2
                                            val maxY = (size.height * (scale - 1)) / 2
                                            offset = Offset(
                                                x = newOffset.x.coerceIn(-maxX, maxX),
                                                y = newOffset.y.coerceIn(-maxY, maxY)
                                            )
                                        }
                                    }
                                }
                                .animateContentSize(spring(0.8f, 300f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { /* Consume click */ }
                                )
                        )

                        AnimatedVisibility(
                            visible = !isFullScreen,
                            enter = slideInVertically { it / 2 } + fadeIn(),
                            exit = slideOutVertically { it / 2 } + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp) // Adds a small space between the rows
                            ) {
                                // First row with the two "apply" buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(onClick = {
                                        coroutineScope.launch {
                                            wallpaper?.let { openSystemWallpaperPreview(context, it) }
                                        }
                                    }) {
                                        Text("Align Poster & Set")
                                    }

                                    Button(onClick = {
                                        coroutineScope.launch {
                                            wallpaper?.let { setWallpaper(context, it) }
                                        }
                                    }) {
                                        Text("Set Poster Directly")
                                    }
                                }

                                // Second row with the "view" button
                                Button(
                                    onClick = { isFullScreen = true }
                                ) {
                                    Text("View Poster in High-Res")
                                }
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = { if (isFullScreen) isFullScreen = false else onClose() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp) // Keep the padding from the bottom
                    .background(
                        color = Color.DarkGray.copy(alpha = 0.9f), // Semi-transparent background
                        shape = CircleShape
                    )
            )
            {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier
                        .size(56.dp) // Increase the icon size
                        .padding(8.dp) // Add some inner padding so the icon doesn't touch the edge
                )
            }
        }
    }
}

//Downloads Full-Res Wallpaper and Gets the URI
private suspend fun downloadAndGetUri(context: Context, wallpaper: Wallpaper): Uri? {
    val loader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(wallpaper.fullUrl)
        .allowHardware(false)
        .build()

    return try {
        val result = (loader.execute(request) as SuccessResult).drawable
        val bitmap = (result as android.graphics.drawable.BitmapDrawable).bitmap

        val file = File(context.filesDir, "${wallpaper.id}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream)
        }
        // Return the content URI for the saved file
        WallpaperProvider.getUriForWallpaper(wallpaper.id)
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to download wallpaper.", Toast.LENGTH_SHORT).show()
        }
        null
    }
}

//Function to directly Apply Wallpaper
private suspend fun setWallpaper(context: Context, wallpaper: Wallpaper) {
    // First, get the URI from our helper function
    val uri = downloadAndGetUri(context, wallpaper) ?: return // Stop if download fails

    try {
        val wallpaperManager = WallpaperManager.getInstance(context)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            wallpaperManager.setStream(inputStream)
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Wallpaper applied directly!", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to apply wallpaper.", Toast.LENGTH_SHORT).show()
        }
    }
}

//Function for launching the system preview
private suspend fun openSystemWallpaperPreview(context: Context, wallpaper: Wallpaper) {
    // This part remains the same. We still need the URI.
    val uri = downloadAndGetUri(context, wallpaper) ?: return

    // --- TIER 1: Try the most specific, modern intent first ---
    // This intent explicitly targets the modern Google Wallpapers picker.
    val modernIntent = android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER)
    modernIntent.setClassName(
        "com.google.android.apps.wallpaper",
        "com.google.android.apps.wallpaper.picker.DeepLinkActivity"
    )
    modernIntent.setDataAndType(uri, "image/jpeg")
    modernIntent.putExtra("mimeType", "image/jpeg")
    modernIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)

    try {
        // Attempt to launch the most modern picker
        context.startActivity(modernIntent)
        return // If successful, we are done.
    } catch (e: Exception) {
        // This will fail if the Google Wallpapers app isn't the handler.
        e.printStackTrace()
    }

    // --- TIER 2: Fallback to the general "set wallpaper" action ---
    // This is the code we tried before. It's the second-best option.
    val generalIntent = android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER)
    generalIntent.setDataAndType(uri, "image/jpeg")
    generalIntent.putExtra("mimeType", "image/jpeg")
    generalIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)

    try {
        context.startActivity(generalIntent)
        return // If successful, we are done.
    } catch (e: Exception) {
        // This will fail on systems that have no handler for this action.
        e.printStackTrace()
    }

    // --- TIER 3: Fallback to the oldest, most compatible cropper ---
    // This is the original intent that shows the simple crop view.
    // It's our last resort to ensure the user can always set a wallpaper.
    try {
        val olderIntent = WallpaperManager.getInstance(context).getCropAndSetWallpaperIntent(uri)
        context.startActivity(olderIntent)
    } catch (e: Exception) {
        // If even this fails, show a final error message.
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Could not open any system wallpaper setter.", Toast.LENGTH_SHORT).show()
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

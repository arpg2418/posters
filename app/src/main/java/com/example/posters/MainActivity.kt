package com.example.posters

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Shapes
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat


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
    // We need a reference to the ViewModel to pass the intent.
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- ADD THIS BLOCK HERE ---
        // This sets the app to immersive mode from the very beginning.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Handle the intent that launched the app
        viewModel.handleIncomingIntent(intent)
        
        // ---------------------------

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
    // This function is called when the app is already running and receives a new deep link.
    override fun onNewIntent(intent: Intent) { // <-- The '?' is removed
        super.onNewIntent(intent)
        viewModel.handleIncomingIntent(intent)
    }
}


// This composable orchestrates the main UI, including the top bar, grid, and overlay.
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    var selectedWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    val uiState = viewModel.uiState // Observe the new state object

    LaunchedEffect(viewModel.deepLinkedWallpaper) {
        // When the deep-linked wallpaper is successfully loaded in the ViewModel...
        if (viewModel.deepLinkedWallpaper != null) {
            // ...set it as the selected wallpaper to open the overlay.
            selectedWallpaper = viewModel.deepLinkedWallpaper
            // Clear it so this doesn't run again on rotation.
            viewModel.clearDeepLink()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { HomeTopBar() },
            containerColor = Color(0xFF121212)
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    ShimmerLoadingGrid()
                } else if (uiState.error != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.error, color = Color.White)
                    }
                } else {
                    WallpaperGrid(
                        wallpapers = uiState.wallpapers,
                        isLoadingNextPage = uiState.isLoadingNextPage,
                        onWallpaperClick = { wallpaper -> selectedWallpaper = wallpaper },
                        onLoadMore = { viewModel.loadNextPage() } // Pass the event handler
                    )
                }
            }
        }

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
    // The key change is using CenterAlignedTopAppBar instead of TopAppBar
    CenterAlignedTopAppBar(
        title = {
            Text("Posters", color = Color.White, fontWeight = FontWeight.Bold)
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// Add this new reusable function
@Composable
fun rememberShimmerBrush(): Brush {
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
    return Brush.linearGradient(
        colors = listOf(
            Color.DarkGray.copy(alpha = 0.9f),
            Color.DarkGray.copy(alpha = 0.7f),
            Color.DarkGray.copy(alpha = 0.9f)
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

@Composable
fun ShimmerLoadingGrid() {
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
                    .background(rememberShimmerBrush())
            )
        }
    }
}

// The staggered grid that displays the wallpaper thumbnails.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperGrid(
    wallpapers: List<Wallpaper>,
    isLoadingNextPage: Boolean,
    onWallpaperClick: (Wallpaper) -> Unit, // <-- This is the missing parameter
    onLoadMore: () -> Unit
) {
    if (wallpapers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No wallpapers found.", color = Color.Gray)
        }
        return
    }

    val gridState = rememberLazyStaggeredGridState() // Remember the grid's scroll state

    // This effect block watches the scroll state to trigger loading the next page
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItemIndex = visibleItems.lastOrNull()?.index ?: 0
                val totalItemCount = gridState.layoutInfo.totalItemsCount
                // If the last visible item is close to the end of the list, load more
                if (lastVisibleItemIndex >= totalItemCount - 10) {
                    onLoadMore()
                }
            }
    }

    LazyVerticalStaggeredGrid(
        state = gridState,
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
                onClick = {onWallpaperClick(wallpaper)}
            )
        }// Add a loading spinner at the bottom of the grid when fetching the next page
        if (isLoadingNextPage) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
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
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(rememberShimmerBrush()) // Use shimmer brush
                )
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
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showShareSheet by remember { mutableStateOf(false) }

    // It will hold the extracted accent color. It's nullable because it takes time to generate.
    var dynamicButtonColor by remember { mutableStateOf<Color?>(null) }
    val fallbackButtonColor = MaterialTheme.colorScheme.primary


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
    // This effect extracts the accent color from the wallpaper image
    LaunchedEffect(wallpaper) {
        if (wallpaper != null) {
            // Use Coil to load the image bitmap
            val request = ImageRequest.Builder(context)
                .data(wallpaper.thumbnailUrl) // Use the medium-res preview for speed
                .allowHardware(false)
                .build()
            val result = (context.imageLoader.execute(request) as? SuccessResult)?.drawable
            if (result != null) {
                // Use the Palette library to generate colors
                val palette = androidx.palette.graphics.Palette.from(
                    (result as android.graphics.drawable.BitmapDrawable).bitmap
                ).generate()
                // Find a vibrant color, with a fallback to the primary theme color
                val vibrantColor = palette.getVibrantColor(fallbackButtonColor.toArgb())
                dynamicButtonColor = Color(vibrantColor)
            }
        } else {
            // Reset the color when the overlay is closed
            dynamicButtonColor = null
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
                                    //View High-Res
                                    Button(
                                        onClick = { isFullScreen = true }
                                    ) {
                                        Text("View Poster in High-Res")
                                    }
                                    //Set Wallpaper
                                    Button(
//                                        onClick = {coroutineScope.launch {wallpaper?.let {openSystemWallpaperPreview(context,it)}}},
                                        onClick = {Toast.makeText(context, "Custom Wallpaper Preview and Set COMING SOON!", Toast.LENGTH_SHORT).show()},
//                                        colors = ButtonDefaults.buttonColors(containerColor = dynamicButtonColor ?: MaterialTheme.colorScheme.primary),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = (dynamicButtonColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.5f),
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
//                                        enabled = false
                                    )
                                    {
                                        Text("Align & Set",
                                            // This adds the strikethrough effect to the text
                                            textDecoration = TextDecoration.LineThrough)
                                    }

                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ){
                                    //Share Button
                                    Button(onClick = { showShareSheet = true })
                                    {
                                    Icon(
                                        imageVector = Icons.Filled.IosShare,
                                        contentDescription = "Download",
                                        )
                                    }

                                    //Download image button
                                    Button(onClick = {
                                        coroutineScope.launch {
                                            wallpaper?.let {
                                                saveWallpaperToGallery(
                                                    context,
                                                    it
                                                )
                                            }
                                        }
                                    },colors = ButtonDefaults.buttonColors(
                                        containerColor = dynamicButtonColor ?: MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary))
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.CloudDownload,
                                            contentDescription = "Download",
                                        )
                                    }
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
                    .padding(bottom = 55.dp) // Keep the padding from the bottom
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
                        .size(64.dp) // Increase the icon size
                        .padding(8.dp) // Add some inner padding so the icon doesn't touch the edge
                )
            }

            // This will show the bottom sheet when the state is true
            if (showShareSheet && wallpaper != null) {
                ShareBottomSheet(
                    wallpaper = wallpaper,
                    onDismiss = { showShareSheet = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    wallpaper: Wallpaper,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    // This is the "smart" URL that will point to our redirector page
    // We will create this page in a later step.
    val shareUrl = "https://arpg2418.github.io/posters-redirect/?wallpaperId=${wallpaper.id}"

    // State to hold the generated QR code bitmap
    var qrCodeBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Generate the QR code when the sheet is first composed
    LaunchedEffect(shareUrl) {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(shareUrl, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        qrCodeBitmap = bmp
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Share Poster", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Display the generated QR code
            qrCodeBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR Code for sharing",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // Display the shareable URL
            OutlinedTextField(
                value = shareUrl,
                onValueChange = {},
                readOnly = true,
                label = { Text("Shareable Link") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    // Native Share Button
                    IconButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share wallpaper via...")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share link")
                    }
                }
            )
        }
    }
}

//Downloads Full-Res Wallpaper and Gets the URI
@SuppressLint("UseKtx")
private suspend fun downloadAndGetUri(context: Context, wallpaper: Wallpaper): Uri? {
    val loader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(wallpaper.fullUrl)
        .allowHardware(false) // This is important for saving
        .build()

    return try {
        // 1. Execute the request and get the result as a generic Drawable
        val result = (loader.execute(request) as SuccessResult).drawable

        // Create a new, blank Bitmap with the same dimensions as the downloaded image.
        val bitmap = Bitmap.createBitmap(
            result.intrinsicWidth,
            result.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        // Create a Canvas to draw on our new Bitmap.
        val canvas = android.graphics.Canvas(bitmap)
        // Draw the downloaded image (whatever its format) onto our canvas.
        result.setBounds(0, 0, canvas.width, canvas.height)
        result.draw(canvas)

        val wallpapersDir = File(context.filesDir, "wallpapers") // 1. Create a reference to the 'wallpapers' directory.
        if (!wallpapersDir.exists()) {
            wallpapersDir.mkdirs() // 2. Ensure the directory exists. If it doesn't, create it.
        }
        val file = File(context.filesDir, "${wallpaper.id}.jpg")// 5. Save the universally-formatted Bitmap to a file.

        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        }
        // 6. Return the content URI for the saved file.
        WallpaperProvider.getUriForWallpaper(wallpaper.id)
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
//            Toast.makeText(context, "Failed to download wallpaper.", Toast.LENGTH_SHORT).show()
            Toast.makeText(context, "Download Error: ${e.message}", Toast.LENGTH_LONG).show()
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
//private suspend fun openSystemWallpaperPreview(context: Context, wallpaper: Wallpaper) {
//    // This part remains the same. We still need the URI.
//    val uri = downloadAndGetUri(context, wallpaper) ?: return
//
//    // --- TIER 1: Try the most specific, modern intent first ---
//    // This intent explicitly targets the modern Google Wallpapers picker.
//    val modernIntent = Intent(Intent.ACTION_SET_WALLPAPER)
//    modernIntent.setClassName(
//        "com.google.android.apps.wallpaper",
//        "com.google.android.apps.wallpaper.picker.DeepLinkActivity"
//    )
//    modernIntent.setDataAndType(uri, "image/jpeg")
//    modernIntent.putExtra("mimeType", "image/jpeg")
//    modernIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//
//    try {
//        // Attempt to launch the most modern picker
//        context.startActivity(modernIntent)
//        return // If successful, we are done.
//    } catch (e: Exception) {
//        // This will fail if the Google Wallpapers app isn't the handler.
//        e.printStackTrace()
//    }
//
//    // --- TIER 2: Fallback to the general "set wallpaper" action ---
//    // This is the code we tried before. It's the second-best option.
//    val generalIntent = Intent(Intent.ACTION_SET_WALLPAPER)
//    generalIntent.setDataAndType(uri, "image/jpeg")
//    generalIntent.putExtra("mimeType", "image/jpeg")
//    generalIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//
//    try {
//        context.startActivity(generalIntent)
//        return // If successful, we are done.
//    } catch (e: Exception) {
//        // This will fail on systems that have no handler for this action.
//        e.printStackTrace()
//    }
//
//    // --- TIER 3: Fallback to the oldest, most compatible cropper ---
//    // This is the original intent that shows the simple crop view.
//    // It's our last resort to ensure the user can always set a wallpaper.
//    try {
//        val olderIntent = WallpaperManager.getInstance(context).getCropAndSetWallpaperIntent(uri)
//        context.startActivity(olderIntent)
//    } catch (e: Exception) {
//        // If even this fails, show a final error message.
//        e.printStackTrace()
//        withContext(Dispatchers.Main) {
//            Toast.makeText(context, "Could not open any system wallpaper setter.", Toast.LENGTH_SHORT).show()
//        }
//    }
//}

// In MainActivity.kt

private suspend fun openSystemWallpaperPreview(context: Context, wallpaper: Wallpaper) {
    // This part remains the same.
    val uri = downloadAndGetUri(context, wallpaper) ?: return

    // --- NEW, SMARTER LOGIC ---
    // 1. Create the modern, general-purpose "set wallpaper" intent.
    val intent = Intent(Intent.ACTION_SET_WALLPAPER)
    intent.setDataAndType(uri, "image/jpeg")
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    // 2. Ask the Android system's PackageManager to find the best app for this job.
    val packageManager = context.packageManager
    val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

    // 3. Check if a default app was found.
    if (resolveInfo != null) {
        try {
            // 3a. If we found a default app, target our intent specifically at that app.
            //     This prevents the chooser dialog from appearing.
            intent.setPackage(resolveInfo.activityInfo.packageName)
            context.startActivity(intent)
            return // Success!
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- FALLBACK: The Oldest, Most Compatible Cropper ---
    // If no default app was found, or if launching it failed,
    // we fall back to the universally compatible cropper.
    try {
        val olderIntent = WallpaperManager.getInstance(context).getCropAndSetWallpaperIntent(uri)
        context.startActivity(olderIntent)
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Could not open system wallpaper setter.", Toast.LENGTH_SHORT).show()
        }
    }
}

private suspend fun saveWallpaperToGallery(context: Context, wallpaper: Wallpaper) {
    // We can reuse our existing download logic to get the bitmap
    val loader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(wallpaper.fullUrl)
        .allowHardware(false)
        .build()

    try {
        val result = (loader.execute(request) as SuccessResult).drawable
        val bitmap = (result as android.graphics.drawable.BitmapDrawable).bitmap

        // Use MediaStore to save the image to the public Pictures directory
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "${wallpaper.id}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // This will place the image in a "Pictures/Posters" folder on modern Android
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Posters")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri).use { stream ->
                if (stream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Poster saved to Gallery!", Toast.LENGTH_SHORT).show()
            }
        } else {
            throw java.io.IOException("Failed to create new MediaStore record.")
        }

    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save poster.", Toast.LENGTH_SHORT).show()
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

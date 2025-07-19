package com.example.posters

import android.app.WallpaperManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.posters.ui.theme.WallpaperAppTheme
import kotlinx.coroutines.launch
import java.io.IOException

// --- Data Classes for our Wallpaper App ---
data class Wallpaper(
    val id: String,
    val name: String,
    val url: String // This now matches the API response
)

data class Category(
    val name: String,
    val imageUrl: String
)

// --- Dummy Data Source ---
object DummyData {
    val categories = listOf(
        Category("Trending", "https://placehold.co/600x400/3498db/ffffff?text=Trending"),
        Category("Nature", "https://placehold.co/600x400/2ecc71/ffffff?text=Nature"),
        Category("Abstract", "https://placehold.co/600x400/9b59b6/ffffff?text=Abstract"),
        Category("Space", "https://placehold.co/600x400/34495e/ffffff?text=Space"),
        Category("Minimal", "https://placehold.co/600x400/ecf0f1/000000?text=Minimal"),
        Category("Anime", "https://placehold.co/600x400/e74c3c/ffffff?text=Anime")
    )
    val wallpapers = listOf(
        Wallpaper("1", "https://placehold.co/400x600/3498db/ffffff?text=Image+1", "Trending", 600),
        Wallpaper("2", "https://placehold.co/400x700/2ecc71/ffffff?text=Image+2", "Nature", 700),
        Wallpaper("3", "https://placehold.co/400x550/9b59b6/ffffff?text=Image+3", "Abstract", 550),
        Wallpaper("4", "https://placehold.co/400x650/e74c3c/ffffff?text=Image+4", "Anime", 650),
        Wallpaper("5", "https://placehold.co/400x500/f1c40f/ffffff?text=Image+5", "Trending", 500),
        Wallpaper("6", "https://placehold.co/400x750/34495e/ffffff?text=Image+6", "Space", 750),
        Wallpaper("7", "https://placehold.co/400x620/27ae60/ffffff?text=Image+7", "Nature", 620),
        Wallpaper("8", "https://placehold.co/400x580/8e44ad/ffffff?text=Image+8", "Abstract", 580),
        Wallpaper("9", "https://placehold.co/400x680/ecf0f1/000000?text=Image+9", "Minimal", 680),
        Wallpaper("10", "https://placehold.co/400x530/c0392b/ffffff?text=Image+10", "Anime", 530),
        Wallpaper("11", "https://placehold.co/400x720/2980b9/ffffff?text=Image+11", "Trending", 720),
        Wallpaper("12", "https://placehold.co/400x630/16a085/ffffff?text=Image+12", "Nature", 630)
    )
}

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

@Composable
fun MainScreen() {
    var selectedCategory by remember { mutableStateOf("Trending") }
    val wallpapersToShow by remember(selectedCategory) {
        derivedStateOf { DummyData.wallpapers } // Show all, filter logic can be added back if needed
    }
    var selectedWallpaper by remember { mutableStateOf<Wallpaper?>(null) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { HomeTopBar() },
            containerColor = Color(0xFF121212)
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                CategorySection(
                    categories = DummyData.categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { categoryName -> selectedCategory = categoryName }
                )
                Spacer(modifier = Modifier.height(16.dp))
                WallpaperGrid(
                    wallpapers = wallpapersToShow.filter { it.category == selectedCategory },
                    onWallpaperClick = { wallpaper -> selectedWallpaper = wallpaper }
                )
            }
        }

        // --- The new Animated Overlay ---
        WallpaperOverlay(
            wallpaper = selectedWallpaper,
            onClose = { selectedWallpaper = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar() {
    TopAppBar(
        title = { Text("Posters", color = Color.White, fontWeight = FontWeight.Bold) },
        actions = {
            IconButton(onClick = { /* TODO: Handle search click */ }) {
                Icon(Icons.Default.Search, "Search Wallpapers", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun CategorySection(
    categories: List<Category>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(categories) { category ->
                CategoryChip(
                    category = category,
                    isSelected = category.name == selectedCategory,
                    onClick = { onCategorySelected(category.name) }
                )
            }
        }
    }
}

@Composable
fun CategoryChip(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(300), label = "borderColor"
    )
    Card(
        modifier = Modifier.width(140.dp).height(70.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(painter = rememberAsyncImagePainter(model = category.imageUrl), contentDescription = category.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.5f)))
            Text(category.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

//@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperGrid(wallpapers: List<Wallpaper>, onWallpaperClick: (Wallpaper) -> Unit) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(wallpapers, key = { it.id }) { wallpaper ->
            WallpaperThumbnail(
                wallpaper = wallpaper,
                modifier = Modifier.animateItem(tween(500)),
                onClick = { onWallpaperClick(wallpaper) }
            )
        }
    }
}

@Composable
fun WallpaperThumbnail(wallpaper: Wallpaper, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().height(wallpaper.height.dp / 3).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = wallpaper.imageUrl),
            contentDescription = "Wallpaper from ${wallpaper.category}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
        )
    }
}

@Composable
fun WallpaperOverlay(wallpaper: Wallpaper?, onClose: () -> Unit) {
    val context = LocalContext.current
    var isFullScreen by remember { mutableStateOf(false) }

    // When the wallpaper changes, reset the full-screen state
    LaunchedEffect(wallpaper) {
        if (wallpaper == null) {
            isFullScreen = false
        }
    }

    AnimatedVisibility(
        visible = wallpaper != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (isFullScreen) isFullScreen = false else onClose() }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                // Animate the content of the overlay
                AnimatedVisibility(
                    visible = wallpaper != null,
                    enter = scaleIn(animationSpec = spring(dampingRatio = 0.7f, stiffness = 150f)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val imageModifier by remember(isFullScreen) {
                            derivedStateOf {
                                if (isFullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().heightIn(max = 500.dp)
                            }
                        }
                        val contentScale by remember(isFullScreen) {
                            derivedStateOf { if (isFullScreen) ContentScale.Fit else ContentScale.Crop }
                        }

                        // The main image that animates
                        Image(
                            painter = rememberAsyncImagePainter(model = wallpaper?.imageUrl),
                            contentDescription = "Selected Wallpaper",
                            contentScale = contentScale,
                            modifier = imageModifier
                                .clip(RoundedCornerShape(16.dp))
                                .animateContentSize(animationSpec = spring(dampingRatio = 0.8f, stiffness = 100f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* Consume click to prevent closing */ }
                        )

                        // Action buttons that animate in
                        AnimatedVisibility(
                            visible = !isFullScreen,
                            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier.padding(top = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(onClick = { isFullScreen = true }) {
                                    Text("View Full Resolution")
                                }
                                Button(onClick = { wallpaper?.let { setWallpaper(context, it) } }) {
                                    Text("Apply Wallpaper")
                                }
                            }
                        }
                    }
                }
            }

            // Close button
            IconButton(
                onClick = { if (isFullScreen) isFullScreen = false else onClose() },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }
    }
}


private fun setWallpaper(context: Context, wallpaper: Wallpaper) {
    try {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val uri = WallpaperProvider.getUriForWallpaper(wallpaper.id)
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            wallpaperManager.setStream(inputStream)
            Toast.makeText(context, "Wallpaper applied!", Toast.LENGTH_SHORT).show()
            inputStream.close()
        } else {
            Toast.makeText(context, "Failed to load wallpaper.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Error setting wallpaper.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Setup required: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// --- ContentProvider Placeholder ---
class WallpaperProvider : ContentProvider() {
    companion object {
        private const val AUTHORITY = "com.example.posters.provider"
        private val BASE_URI = Uri.parse("content://$AUTHORITY")
        fun getUriForWallpaper(id: String): Uri = BASE_URI.buildUpon().appendPath(id).build()
    }

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        throw NotImplementedError("ContentProvider needs to be connected to your app's private storage.")
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = "image/jpeg"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun MainScreenPreview() {
    WallpaperAppTheme {
        MainScreen()
    }
}

package com.example.jpegdiff

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JPEGDiffTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompareApp()
                }
            }
        }
    }
}

// Sleek dark-mode specialized theme with HSL colors and gradients
@Composable
fun JPEGDiffTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF6200EE),
        secondary = Color(0xFF03DAC6),
        background = Color(0xFF0E0E15),
        surface = Color(0xFF1B1B26),
        error = Color(0xFFFF4B4B)
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}

data class CompareResult(
    val success: Boolean,
    val errorMsg: String? = null,
    val resultBitmap: Bitmap? = null,
    val totalPixels: Int = 0,
    val mismatchedPixels: Int = 0,
    val matchPercentage: Float = 0f,
    val width: Int = 0,
    val height: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareApp() {
    val context = LocalContext.current
    var uri1 by remember { mutableStateOf<Uri?>(null) }
    var uri2 by remember { mutableStateOf<Uri?>(null) }
    var isComparing by remember { mutableStateOf(false) }
    var compareResult by remember { mutableStateOf<CompareResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pickImage1 = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            uri1 = uri
            compareResult = null
            errorMessage = null
        }
    }

    val pickImage2 = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            uri2 = uri
            compareResult = null
            errorMessage = null
        }
    }

    LaunchedEffect(uri1, uri2) {
        val u1 = uri1
        val u2 = uri2
        if (u1 != null && u2 != null) {
            isComparing = true
            errorMessage = null
            val result = performComparison(context, u1, u2)
            isComparing = false
            if (result.success) {
                compareResult = result
            } else {
                errorMessage = result.errorMsg ?: "Unknown error"
            }
        } else {
            compareResult = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E0E15),
                        Color(0xFF1B1B2A)
                    )
                )
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Premium Typography Title
        Text(
            text = "JPEG PIXEL DIFF",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            letterSpacing = 2.sp
        )
        Text(
            text = "Compare images pixel-by-pixel with zero tolerance",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Cards for Selecting Images
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ImageSelectCard(
                title = "JPEG 1",
                uri = uri1,
                onClick = { pickImage1.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f)
            )
            ImageSelectCard(
                title = "JPEG 2",
                uri = uri2,
                onClick = { pickImage2.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error message if any
        errorMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Loading spinner when auto-comparing
        if (isComparing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        }

        // Comparison Result UI
        compareResult?.let { result ->
            AnimatedVisibility(visible = true) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Statistics Card
                    StatsCard(result = result)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Download Button
                    var isSaving by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()

                    Button(
                        onClick = {
                            result.resultBitmap?.let { bitmap ->
                                isSaving = true
                                scope.launch {
                                    val savedUri = saveBitmapToDownloads(context, bitmap)
                                    isSaving = false
                                    if (savedUri != null) {
                                        Toast.makeText(context, "Saved difference to Downloads", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isSaving && result.resultBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "DOWNLOAD DIFFERENCE (100% Quality)",
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "DIFFERENCE (Pinch to Zoom, Drag to Pan)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Zoomable Result View
                    result.resultBitmap?.let { bitmap ->
                        ZoomableImage(bitmap = bitmap)
                    }
                }
            }
        }
    }
}

@Composable
fun ImageSelectCard(
    title: String,
    uri: Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var thumbnail by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 4 // Scale down for preview thumbnail
                        }
                        thumbnail = BitmapFactory.decodeStream(stream, null, options)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            thumbnail = null
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2A)),
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable(onClick = onClick)
            .border(1.dp, Color(0xFF2E2E3F), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "+",
                        fontSize = 40.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select $title",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard(result: CompareResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2E2E3F), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Comparison Summary",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Dimensions:", color = Color.Gray)
                Text(text = "${result.width} x ${result.height}", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total Pixels:", color = Color.Gray)
                Text(text = String.format("%,d", result.totalPixels), color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Mismatched Pixels:", color = Color.Gray)
                Text(
                    text = String.format("%,d", result.mismatchedPixels),
                    color = if (result.mismatchedPixels > 0) MaterialTheme.colorScheme.error else Color.Green,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val matchPercentageText = String.format("%.4f%%", result.matchPercentage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Exact Match Rate:", color = Color.Gray)
                Text(
                    text = matchPercentageText,
                    color = if (result.matchPercentage == 100f) Color.Green else Color.Yellow,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun ZoomableImage(bitmap: Bitmap) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.dp, Color(0xFF2E2E3F), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 8f)
                    if (scale > 1f) {
                        offset = offset + pan
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Difference Output",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

// Background pixel matching runner
private suspend fun performComparison(
    context: Context,
    uri1: Uri,
    uri2: Uri
): CompareResult = withContext(Dispatchers.IO) {
    try {
        var bitmap1: Bitmap? = null
        var bitmap2: Bitmap? = null

        context.contentResolver.openInputStream(uri1)?.use { stream ->
            bitmap1 = BitmapFactory.decodeStream(stream)
        }
        context.contentResolver.openInputStream(uri2)?.use { stream ->
            bitmap2 = BitmapFactory.decodeStream(stream)
        }

        val b1 = bitmap1 ?: return@withContext CompareResult(false, "Failed to load JPEG 1")
        val b2 = bitmap2 ?: return@withContext CompareResult(false, "Failed to load JPEG 2")

        val b1IsLarger = b1.width >= b2.width && b1.height >= b2.height
        val b2IsLarger = b2.width >= b1.width && b2.height >= b1.height

        if (!b1IsLarger && !b2IsLarger) {
            return@withContext CompareResult(
                false,
                "Incompatible dimensions: JPEG 1 is ${b1.width}x${b1.height}, but JPEG 2 is ${b2.width}x${b2.height}. One must fit entirely inside the other."
            )
        }

        val larger = if (b1IsLarger) b1 else b2
        val smaller = if (b1IsLarger) b2 else b1
        val isB1Larger = b1IsLarger

        val W = larger.width
        val H = larger.height
        val w = smaller.width
        val h = smaller.height
        val total = W * H

        val largerPixels = IntArray(W * H)
        val smallerPixels = IntArray(w * h)
        larger.getPixels(largerPixels, 0, W, 0, 0, W, H)
        smaller.getPixels(smallerPixels, 0, w, 0, 0, w, h)

        var bestDx = 0
        var bestDy = 0

        if (W != w || H != h) {
            // Find the best offset (bestDx, bestDy) on JPEG block boundaries (multiples of 8)
            val numSamplesX = minOf(w, 24)
            val numSamplesY = minOf(h, 24)
            val samplePoints = ArrayList<Pair<Int, Int>>(numSamplesX * numSamplesY)
            val stepX = if (numSamplesX > 1) (w - 1) / (numSamplesX - 1) else 1
            val stepY = if (numSamplesY > 1) (h - 1) / (numSamplesY - 1) else 1
            for (i in 0 until numSamplesX) {
                val x = i * stepX
                for (j in 0 until numSamplesY) {
                    val y = j * stepY
                    samplePoints.add(Pair(x, y))
                }
            }

            var minDistance = Long.MAX_VALUE

            for (dy in 0..(H - h) step 8) {
                for (dx in 0..(W - w) step 8) {
                    var distance = 0L
                    for (p in samplePoints) {
                        val sx = p.first
                        val sy = p.second
                        val lx = dx + sx
                        val ly = dy + sy

                        val c1 = smallerPixels[sy * w + sx]
                        val c2 = largerPixels[ly * W + lx]

                        val r1 = (c1 shr 16) and 0xFF
                        val g1 = (c1 shr 8) and 0xFF
                        val b1 = c1 and 0xFF
                        val r2 = (c2 shr 16) and 0xFF
                        val g2 = (c2 shr 8) and 0xFF
                        val b2 = c2 and 0xFF

                        distance += kotlin.math.abs(r1 - r2) + kotlin.math.abs(g1 - g2) + kotlin.math.abs(b1 - b2)
                    }
                    if (distance < minDistance) {
                        minDistance = distance
                        bestDx = dx
                        bestDy = dy
                    }
                }
            }
        }

        val outPixels = IntArray(total)
        var mismatched = 0

        for (ly in 0 until H) {
            for (lx in 0 until W) {
                val idx = ly * W + lx
                if (lx in bestDx until (bestDx + w) && ly in bestDy until (bestDy + h)) {
                    val sx = lx - bestDx
                    val sy = ly - bestDy
                    val c1 = if (isB1Larger) largerPixels[idx] else smallerPixels[sy * w + sx]
                    val c2 = if (isB1Larger) smallerPixels[sy * w + sx] else largerPixels[idx]
                    if (c1 != c2) {
                        mismatched++
                        outPixels[idx] = android.graphics.Color.RED
                    } else {
                        outPixels[idx] = whitenColor(c1)
                    }
                } else {
                    mismatched++
                    outPixels[idx] = android.graphics.Color.RED
                }
            }
        }

        val outBitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(outPixels, 0, W, 0, 0, W, H)

        val matchPercent = ((total - mismatched).toFloat() / total.toFloat()) * 100f

        return@withContext CompareResult(
            success = true,
            resultBitmap = outBitmap,
            totalPixels = total,
            mismatchedPixels = mismatched,
            matchPercentage = matchPercent,
            width = W,
            height = H
        )
    } catch (e: Exception) {
        return@withContext CompareResult(false, "Comparison execution failed: ${e.localizedMessage}")
    }
}

private fun whitenColor(color: Int): Int {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val rW = (r * 0.2f + 255 * 0.8f).toInt().coerceIn(0, 255)
    val gW = (g * 0.2f + 255 * 0.8f).toInt().coerceIn(0, 255)
    val bW = (b * 0.2f + 255 * 0.8f).toInt().coerceIn(0, 255)
    return (color and 0xFF000000.toInt()) or (rW shl 16) or (gW shl 8) or bW
}

private fun saveBitmapToDownloads(context: Context, bitmap: Bitmap): Uri? {
    val filename = "jpegdiff_difference_${System.currentTimeMillis()}.jpg"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                        throw IOException("Failed to compress bitmap")
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                return uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                e.printStackTrace()
            }
        }
    } else {
        // Fallback for API < 29
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, filename)
        try {
            FileOutputStream(file).use { stream ->
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                    // Let the media scanner know about the file
                    val mediaValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DATA, file.absolutePath)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    }
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaValues)
                    return Uri.fromFile(file)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}

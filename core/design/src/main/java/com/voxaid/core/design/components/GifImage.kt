package com.voxaid.core.design.components

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Composable for displaying GIF animations in the app.
 * Optimized for performance with Coil's caching and memory management.
 *
 * **Asset Replacement Guide:**
 * 1. Place your GIF files in `app/src/main/res/raw/`
 * 2. Use naming convention: `anim_[protocol]_[step].gif`
 *    Example: `anim_cpr_compressions.gif`, `anim_heimlich_thrust.gif`
 * 3. Reference using R.raw resource ID
 * 4. See docs/GIF_GUIDELINES.md for optimization tips
 *
 * @param resourceId Raw resource ID of the GIF file (R.raw.*)
 * @param contentDescription Accessibility description
 * @param modifier Modifier for customization
 * @param showPlaceholder If true, shows placeholder with instructions
 * @param contentScale How to scale the image
 */
@Composable
fun GifImage(
    @RawRes resourceId: Int?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    showPlaceholder: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current

    if (showPlaceholder || resourceId == null) {
        // Placeholder UI when no GIF is provided
        GifPlaceholder(
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        // Load actual GIF with Coil
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(resourceId)
                .decoderFactory(
                    ImageDecoderDecoder.Factory()
                )
                .crossfade(true)
                .memoryCacheKey("gif_$resourceId")
                .diskCacheKey("gif_$resourceId")
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            loading = {
                // Show placeholder while loading
                GifPlaceholder(
                    contentDescription = "Loading $contentDescription",
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                // Show error state if GIF fails to load
                GifPlaceholder(
                    contentDescription = "Error loading $contentDescription",
                    modifier = Modifier.fillMaxSize(),
                    isError = true
                )
            }
        )
    }
}

/**
 * Placeholder shown when GIF is not available or loading.
 * Clearly marked for developer reference.
 */
@Composable
private fun GifPlaceholder(
    contentDescription: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isError) "⚠️" else "🎬",
                    style = MaterialTheme.typography.displayMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isError) {
                        "Animation Error"
                    } else {
                        "Loading..."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

//                Spacer(modifier = Modifier.height(4.dp))
//
//                Text(
//                    text = contentDescription,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    textAlign = TextAlign.Center
//                )
//
//                if (!isError) {
//                    Spacer(modifier = Modifier.height(12.dp))
//
//                    Text(
//                        text = "📁 Replace with actual GIF\nSee docs/GIF_GUIDELINES.md",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.primary,
//                        textAlign = TextAlign.Center
//                    )
//                }
            }
        }
    }
}

/**
 * Composable for loading GIF from drawable resources.
 * Alternative to raw resources for easier asset management.
 */
@Composable
fun GifImageFromDrawable(
    drawableResId: Int?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    showPlaceholder: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current

    if (showPlaceholder || drawableResId == null) {
        GifPlaceholder(
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(drawableResId)
                .decoderFactory(
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        ImageDecoderDecoder.Factory()
                    } else {
                        GifDecoder.Factory()
                    }
                )
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            loading = {
                GifPlaceholder(contentDescription = "Loading", modifier = Modifier.fillMaxSize())
            },
            error = {
                GifPlaceholder(contentDescription = "Error", modifier = Modifier.fillMaxSize(), isError = true)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GifImagePreview() {
    VoxAidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GifImage(
                resourceId = null,
                contentDescription = "CPR Chest Compressions Animation",
                showPlaceholder = true,
                modifier = Modifier.fillMaxWidth()
            )

            GifPlaceholder(
                contentDescription = "Heimlich Maneuver Animation",
                isError = true
            )
        }
    }
}
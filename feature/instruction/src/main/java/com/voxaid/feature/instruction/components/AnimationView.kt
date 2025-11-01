package com.voxaid.feature.instruction.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.components.GifImage
import com.voxaid.core.design.theme.VoxAidTheme
import com.voxaid.core.design.util.AnimationConfig

/**
 * Animation view component for instruction visualizations.
 * Replaced Lottie with GIF support for better compatibility and asset management.
 *
 * **Migration from Lottie to GIF:**
 * - Lottie files (.json) have been replaced with GIF files (.gif)
 * - Place GIFs in `app/src/main/res/raw/` directory
 * - Use naming convention: `anim_[protocol]_[action].gif`
 * - Update AnimationConfig object with R.raw resource IDs
 *
 * **GIF Optimization Tips:**
 * 1. Dimensions: 512x512px (square) or 720x480px (16:9)
 * 2. File size: Keep under 500KB for smooth loading
 * 3. Frame rate: 15-24 fps is optimal
 * 4. Color palette: Use 64-128 colors for balance
 * 5. Compression: Use tools like ezgif.com or gifsicle
 * 6. Loop: Set to infinite for continuous actions
 *
 * See docs/GIF_GUIDELINES.md for detailed optimization guide.
 *
 * @param animationResource Name of animation from protocol JSON (e.g., "cpr_compressions.json")
 * @param modifier Modifier for customization
 * @param contentScale How to scale the animation
 */
@Composable
fun AnimationView(
    animationResource: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    // Map animation name to resource ID
    val resourceId = AnimationConfig.getAnimationResource(animationResource)

    GifImage(
        resourceId = resourceId,
        contentDescription = "Animation: ${animationResource.removeSuffix(".json")}",
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp),
        showPlaceholder = resourceId == null,
        contentScale = contentScale
    )
}

/**
 * Legacy component name for backward compatibility.
 * Use AnimationView instead.
 */
@Deprecated(
    message = "Use AnimationView instead",
    replaceWith = ReplaceWith("AnimationView(animationResource, modifier, contentScale)")
)
@Composable
fun LottieAnimationView(
    animationResource: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    iterations: Int = Int.MAX_VALUE
) {
    AnimationView(
        animationResource = animationResource,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun AnimationViewPreview() {
    VoxAidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimationView(
                animationResource = "cpr_compressions.json",
                modifier = Modifier.fillMaxWidth()
            )

            AnimationView(
                animationResource = "heimlich_thrust.json",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
package com.voxaid.feature.instruction.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

/**
 * Lottie animation component for instruction visualizations.
 *
 * To use with actual Lottie files:
 * 1. Place .json Lottie files in assets/lottie/
 * 2. Reference them by filename (e.g., "cpr_compressions.json")
 *
 * Free Lottie animations available at:
 * - https://lottiefiles.com (search for "medical", "CPR", "first aid")
 * - https://lottiefiles.com/featured
 */
@Composable
fun LottieAnimationView(
    animationResource: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    iterations: Int = LottieConstants.IterateForever
) {
    // For production, load from assets:
    // val composition by rememberLottieComposition(
    //     LottieCompositionSpec.Asset("lottie/$animationResource")
    // )

    // Placeholder implementation for development
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Lottie Animation:\n$animationResource\n\n" +
                        "TODO: Add actual Lottie file to assets/lottie/",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    /*
     * Production implementation example:
     *
     * val composition by rememberLottieComposition(
     *     LottieCompositionSpec.Asset("lottie/$animationResource")
     * )
     *
     * val progress by animateLottieCompositionAsState(
     *     composition = composition,
     *     isPlaying = isPlaying,
     *     iterations = iterations,
     *     speed = 1f
     * )
     *
     * LottieAnimation(
     *     composition = composition,
     *     progress = { progress },
     *     modifier = modifier
     * )
     */
}

/**
 * Example Lottie JSON structure for reference.
 * This can be saved as a .json file in assets/lottie/
 *
 * Simple pulse animation example:
 */
const val EXAMPLE_LOTTIE_JSON = """
{
  "v": "5.7.4",
  "fr": 30,
  "ip": 0,
  "op": 60,
  "w": 500,
  "h": 500,
  "nm": "Simple Pulse",
  "ddd": 0,
  "assets": [],
  "layers": [
    {
      "ddd": 0,
      "ind": 1,
      "ty": 4,
      "nm": "Circle",
      "sr": 1,
      "ks": {
        "o": { "a": 0, "k": 100 },
        "r": { "a": 0, "k": 0 },
        "p": { "a": 0, "k": [250, 250, 0] },
        "a": { "a": 0, "k": [0, 0, 0] },
        "s": {
          "a": 1,
          "k": [
            { "t": 0, "s": [100, 100, 100] },
            { "t": 30, "s": [120, 120, 100] },
            { "t": 60, "s": [100, 100, 100] }
          ]
        }
      },
      "ao": 0,
      "shapes": [
        {
          "ty": "gr",
          "it": [
            {
              "d": 1,
              "ty": "el",
              "s": { "a": 0, "k": [100, 100] },
              "p": { "a": 0, "k": [0, 0] }
            },
            {
              "ty": "fl",
              "c": { "a": 0, "k": [0.8, 0.2, 0.2, 1] }
            }
          ]
        }
      ],
      "ip": 0,
      "op": 60,
      "st": 0
    }
  ]
}
"""

/**
 * Instructions for adding real Lottie animations:
 *
 * 1. Download free Lottie animations from lottiefiles.com
 *    Recommended searches:
 *    - "medical emergency"
 *    - "CPR chest compression"
 *    - "first aid"
 *    - "heartbeat"
 *    - "breathing"
 *
 * 2. Place .json files in: app/src/main/assets/lottie/
 *    Example structure:
 *    assets/
 *      lottie/
 *        cpr_compressions.json
 *        cpr_check_response.json
 *        heimlich_thrust.json
 *        bandage_apply.json
 *
 * 3. Update protocol JSON files to reference these animations
 *
 * 4. The LottieAnimationView component will automatically load them
 */
package com.voxaid.core.design.util

import androidx.annotation.RawRes
import com.voxaid.core.design.R

/**
 * Central configuration for all animations in VoxAid.
 * Maps protocol steps to their corresponding GIF resources.
 *
 * **How to Add New GIFs:**
 * 1. Place GIF in `app/src/main/res/raw/`
 * 2. Add entry to appropriate protocol map
 * 3. Use descriptive naming: `anim_[protocol]_[action].gif`
 *
 * **GIF Requirements:**
 * - Dimensions: 512x512px or 720x480px (16:9)
 * - File size: < 500KB (< 1MB max)
 * - Frame rate: 15-24 fps
 * - Loop: Infinite for continuous actions, once for step completions
 * - Format: GIF89a with LZW compression
 * - Colors: 64-128 color palette for optimal size/quality
 *
 * See docs/GIF_GUIDELINES.md for detailed specifications.
 */
object AnimationConfig {

    /**
     * CPR protocol animations.
     * Resource IDs will be added when actual GIF files are provided.
     */
    object CPR {
        // Example: R.raw.anim_cpr_check_response
        @RawRes val CHECK_RESPONSE: Int = R.raw.cpr_pos_1

        // Example: R.raw.anim_cpr_call_911
        @RawRes val CALL_911: Int? = null

        // Example: R.raw.anim_cpr_hand_position
        @RawRes val HAND_POSITION: Int? = null

        // Example: R.raw.anim_cpr_compressions
        @RawRes val COMPRESSIONS: Int? = null

        // Example: R.raw.anim_cpr_rescue_breaths
        @RawRes val RESCUE_BREATHS: Int? = null

        // Example: R.raw.anim_cpr_cycle
        @RawRes val CYCLE: Int? = null

        // Example: R.raw.anim_cpr_aed_power_on
        @RawRes val AED_POWER_ON: Int? = null

        // Example: R.raw.anim_cpr_aed_pad_placement
        @RawRes val AED_PAD_PLACEMENT: Int? = null
    }

    /**
     * Heimlich maneuver animations.
     */
    object Heimlich {
        // Example: R.raw.anim_heimlich_assess
        @RawRes val ASSESS: Int? = null

        // Example: R.raw.anim_heimlich_position
        @RawRes val POSITION: Int? = null

        // Example: R.raw.anim_heimlich_fist
        @RawRes val FIST: Int? = null

        // Example: R.raw.anim_heimlich_thrust
        @RawRes val THRUST: Int? = null

        // Example: R.raw.anim_heimlich_check
        @RawRes val CHECK: Int? = null

        // Example: R.raw.anim_heimlich_self_fist
        @RawRes val SELF_FIST: Int? = null

        // Example: R.raw.anim_heimlich_self_thrust
        @RawRes val SELF_THRUST: Int? = null

        // Example: R.raw.anim_heimlich_self_chair
        @RawRes val SELF_CHAIR: Int? = null
    }

    /**
     * Bandaging animations.
     */
    object Bandaging {
        // Example: R.raw.anim_bandage_assess
        @RawRes val ASSESS: Int? = null

        // Example: R.raw.anim_bandage_pressure
        @RawRes val PRESSURE: Int? = null

        // Example: R.raw.anim_bandage_clean
        @RawRes val CLEAN: Int? = null

        // Example: R.raw.anim_bandage_ointment
        @RawRes val OINTMENT: Int? = null

        // Example: R.raw.anim_bandage_apply
        @RawRes val APPLY: Int? = null

        // Example: R.raw.anim_bandage_triangular_position
        @RawRes val TRIANGULAR_POSITION: Int? = null

        // Example: R.raw.anim_bandage_triangular_tie
        @RawRes val TRIANGULAR_TIE: Int? = null

        // Example: R.raw.anim_bandage_triangular_secure
        @RawRes val TRIANGULAR_SECURE: Int? = null

        // Example: R.raw.anim_bandage_circular_start
        @RawRes val CIRCULAR_START: Int? = null

        // Example: R.raw.anim_bandage_circular_wrap
        @RawRes val CIRCULAR_WRAP: Int? = null

        // Example: R.raw.anim_bandage_circular_secure
        @RawRes val CIRCULAR_SECURE: Int? = null
    }

    /**
     * General animations used across multiple protocols.
     */
    object General {
        // Example: R.raw.anim_wash_hands
        @RawRes val WASH_HANDS: Int? = null

        // Example: R.raw.anim_call_911
        @RawRes val CALL_911: Int? = null
    }

    /**
     * Maps animation resource names (from JSON) to resource IDs.
     * Used to resolve animations dynamically from protocol definitions.
     */
    fun getAnimationResource(animationName: String): Int? {
        return when (animationName) {
            // CPR
            "cpr_check_response.json" -> CPR.CHECK_RESPONSE
            "call_911.json" -> CPR.CALL_911
            "cpr_hand_position.json" -> CPR.HAND_POSITION
            "cpr_compressions.json" -> CPR.COMPRESSIONS
            "cpr_rescue_breaths.json" -> CPR.RESCUE_BREATHS
            "cpr_cycle.json" -> CPR.CYCLE
            "aed_power_on.json" -> CPR.AED_POWER_ON
            "aed_pad_placement.json" -> CPR.AED_PAD_PLACEMENT

            // Heimlich
            "heimlich_assess.json" -> Heimlich.ASSESS
            "heimlich_position.json" -> Heimlich.POSITION
            "heimlich_fist.json" -> Heimlich.FIST
            "heimlich_thrust.json" -> Heimlich.THRUST
            "heimlich_check.json" -> Heimlich.CHECK
            "heimlich_self_fist.json" -> Heimlich.SELF_FIST
            "heimlich_self_thrust.json" -> Heimlich.SELF_THRUST
            "heimlich_self_chair.json" -> Heimlich.SELF_CHAIR

            // Bandaging
            "bandage_assess.json" -> Bandaging.ASSESS
            "bandage_pressure.json" -> Bandaging.PRESSURE
            "bandage_clean.json" -> Bandaging.CLEAN
            "bandage_ointment.json" -> Bandaging.OINTMENT
            "bandage_apply.json" -> Bandaging.APPLY
            "bandage_triangular_position.json" -> Bandaging.TRIANGULAR_POSITION
            "bandage_triangular_tie.json" -> Bandaging.TRIANGULAR_TIE
            "bandage_triangular_secure.json" -> Bandaging.TRIANGULAR_SECURE
            "bandage_circular_start.json" -> Bandaging.CIRCULAR_START
            "bandage_circular_wrap.json" -> Bandaging.CIRCULAR_WRAP
            "bandage_circular_secure.json" -> Bandaging.CIRCULAR_SECURE

            // General
            "wash_hands.json" -> General.WASH_HANDS

            else -> null
        }
    }
}
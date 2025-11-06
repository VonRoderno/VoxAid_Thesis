package com.voxaid.core.design.util

import androidx.annotation.RawRes
import com.voxaid.core.design.R

/**
 * Central configuration for all animations in VoxAid.
 * Maps protocol steps to their corresponding GIF resources.
 *
 * Updated: Added new GIF mappings for restructured CPR and Heimlich protocols
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
     * CPR Learning Mode animations - New 21-step flow
     */
    object CPR {
        @RawRes
        val SURVEY: Int? = R.raw.survey_gif // survey_gif.gif
        @RawRes
        val CHECK_PATIENT_1: Int = R.raw.cpr_1 // cpr_1.gif (using existing as placeholder)
        @RawRes
        val CALL_EMERGENCY: Int? = R.raw.call_emergency // call_emergency.gif
        @RawRes
        val CHECK_PATIENT_2: Int? = R.raw.cpr_2 // cpr_2.gif
        @RawRes
        val CPR_CHECKLIST: Int? = R.raw.cpr_checklist // CPR Checklist.gif
        @RawRes
        val DONT_CPR: Int? = R.raw.dont_cpr // dont_cpr.gif
        @RawRes
        val TIPS: Int? = R.raw.tips_gif // tips_gif.gif
        @RawRes
        val POSITION_1: Int = R.raw.cpr_pos_1 // cpr_pos_1.gif
        @RawRes
        val POSITION_2: Int? = R.raw.cpr_pos_2 // cpr_pos_2.gif
        @RawRes
        val POSITION_3: Int? = R.raw.cpr_pos_3 // cpr_pos_3.gif
        @RawRes
        val CPR_PROPER: Int? = R.raw.cpr_proper // cpr_proper.gif
        @RawRes
        val RESCUE_BREATHS: Int? = R.raw.cpr_rescuebreaths // cpr_rescuebreaths.gif
        @RawRes
        val CHECK_AGAIN: Int? = R.raw.cpr_checkagain // cpr_checkagain.gif
        @RawRes
        val CPR_SWITCH: Int? = R.raw.cpr_switch // cpr_switch.gif
        @RawRes
        val AED_1: Int? = R.raw.aed_1 // aed_1.gif
        @RawRes
        val AED_2: Int? = R.raw.aed_2 // aed_2.gif
        @RawRes
        val AED_3: Int? = R.raw.aed_3 // aed_3.gif
        @RawRes
        val AED_4: Int? = R.raw.aed_4 // aed_4.gif
        @RawRes
        val DISCLAIMER: Int? = R.raw.disclaimer // disclaimer.gif
    }

    /**
     * Heimlich maneuver animations - Updated
     */
    object Heimlich {
        // Self Heimlich
        @RawRes
        val SELF_1: Int? = R.raw.heimlich_yself_1 // heimlich_yself_1.gif
        @RawRes
        val SELF_2: Int? = R.raw.heimlich_yself_2 // heimlich_yself_2.gif
        @RawRes
        val SELF_3: Int? = R.raw.heimlich_yself_3 // heimlich_yself_3.gif

        // Heimlich for Others
        @RawRes
        val OTHER_1: Int? = R.raw.heimlich_other_1 // heimlich_other_1.gif
        @RawRes
        val OTHER_2: Int? = R.raw.heimlich_other_2 // heimlich_other_2.gif
        @RawRes
        val OTHER_3: Int? = R.raw.heimlich_other_3 // heimlich_other_3.gif
        @RawRes
        val OTHER_4: Int? = R.raw.heimlich_other_4 // heimlich_other_4.gif
    }

    /**
     * Bandaging animations (unchanged, awaiting new content)
     */
    object Bandaging {
        @RawRes
        val ASSESS: Int? = null
        @RawRes
        val PRESSURE: Int? = null
        @RawRes
        val CLEAN: Int? = null
        @RawRes
        val OINTMENT: Int? = null
        @RawRes
        val APPLY: Int? = null
        @RawRes
        val TRIANGULAR_POSITION: Int? = null
        @RawRes
        val TRIANGULAR_TIE: Int? = null
        @RawRes
        val TRIANGULAR_SECURE: Int? = null
        @RawRes
        val CIRCULAR_START: Int? = null
        @RawRes
        val CIRCULAR_WRAP: Int? = null
        @RawRes
        val CIRCULAR_SECURE: Int? = null
    }

    /**
     * Maps animation resource names (from JSON) to resource IDs.
     * Used to resolve animations dynamically from protocol definitions.
     */
    fun getAnimationResource(animationName: String): Int? {
        return when (animationName) {
            // CPR Learning Mode
            "survey_gif.gif" -> CPR.SURVEY
            "cpr_1.gif" -> CPR.CHECK_PATIENT_1
            "call_emergency.gif" -> CPR.CALL_EMERGENCY
            "cpr_2.gif" -> CPR.CHECK_PATIENT_2
            "cpr_checklist.gif" -> CPR.CPR_CHECKLIST
            "dont_cpr.gif" -> CPR.DONT_CPR
            "tips_gif.gif" -> CPR.TIPS
            "cpr_pos_1.gif" -> CPR.POSITION_1
            "cpr_pos_2.gif" -> CPR.POSITION_2
            "cpr_pos_3.gif" -> CPR.POSITION_3
            "cpr_proper.gif" -> CPR.CPR_PROPER
            "cpr_rescuebreaths.gif" -> CPR.RESCUE_BREATHS
            "cpr_checkagain.gif" -> CPR.CHECK_AGAIN
            "cpr_switch.gif" -> CPR.CPR_SWITCH
            "aed_1.gif" -> CPR.AED_1
            "aed_2.gif" -> CPR.AED_2
            "aed_3.gif" -> CPR.AED_3
            "aed_4.gif" -> CPR.AED_4
            "disclaimer.gif" -> CPR.DISCLAIMER

            // Heimlich Self
            "heimlich_yself_1.gif" -> Heimlich.SELF_1
            "heimlich_yself_.gif" -> Heimlich.SELF_2
            "heimlich_yself_3.gif" -> Heimlich.SELF_3
            // Heimlich Others
            "heimlich_other_1.gif" -> Heimlich.OTHER_1
            "heimlich_other_2.gif" -> Heimlich.OTHER_2
            "heimlich_other_3.gif" -> Heimlich.OTHER_3
            "heimlich_other_4.gif" -> Heimlich.OTHER_4

            // Bandaging (unchanged)
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

            else -> null
        }
    }
}
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
        val ALTERNATIVE: Int? = R.raw.alternative

        // Head bandaging
        @RawRes
        val HEAD_1: Int? = R.raw.headbandage_1
        @RawRes
        val HEAD_2: Int? = R.raw.headbandage_2
        @RawRes
        val HEAD_3: Int? = R.raw.headbandage_3
        @RawRes
        val HEAD_4: Int? = R.raw.headbandage_4
        @RawRes
        val HEAD_5: Int? = R.raw.headbandage_5
        @RawRes
        val HEAD_EXTRA: Int? = R.raw.headbandage_extra

        // Arm sling
        @RawRes
        val ARM_SLING_1: Int? = R.raw.armsling_1
        @RawRes
        val ARM_SLING_2: Int? = R.raw.armsling_2
        @RawRes
        val ARM_SLING_3: Int? = R.raw.armsling_3
        @RawRes
        val ARM_SLING_4: Int? = R.raw.armsling_4
        @RawRes
        val ARM_SLING_5: Int? = R.raw.armsling_5
        @RawRes
        val ARM_SLING_6: Int? = R.raw.armsling_6
        @RawRes
        val ARM_SLING_7: Int? = R.raw.armsling_7

        // Narrow cravat
        @RawRes
        val NARROW_CRAVAT_1: Int? = R.raw.narrowcravat_1
        @RawRes
        val NARROW_CRAVAT_2: Int? = R.raw.narrowcravat_2
        @RawRes
        val NARROW_CRAVAT_3: Int? = R.raw.narrowcravat_3
        @RawRes
        val NARROW_CRAVAT_4: Int? = R.raw.narrowcravat_4
        @RawRes
        val NARROW_CRAVAT_5: Int? = R.raw.narrowcravat_5
        @RawRes
        val NARROW_CRAVAT_6: Int? = R.raw.narrowcravat_6
        @RawRes
        val NARROW_CRAVAT_7: Int? = R.raw.narrowcravat_7
        @RawRes
        val NARROW_CRAVAT_8: Int? = R.raw.narrowcravat_8

        // Hand Bandaging (Minor Burns)
        @RawRes
        val HAND_BANDAGING_MINOR_BURNS_1: Int? = R.raw.handbandaging_1
        @RawRes
        val HAND_BANDAGING_MINOR_BURNS_2: Int? = R.raw.handbandaging_2
        @RawRes
        val HAND_BANDAGING_MINOR_BURNS_3: Int? = R.raw.handbandaging_3
        @RawRes
        val HAND_BANDAGING_MINOR_BURNS_4: Int? = R.raw.handbandaging_4
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
            "heimlich_yself_2.gif" -> Heimlich.SELF_2
            "heimlich_yself_3.gif" -> Heimlich.SELF_3
            // Heimlich Others
            "heimlich_other_1.gif" -> Heimlich.OTHER_1
            "heimlich_other_2.gif" -> Heimlich.OTHER_2
            "heimlich_other_3.gif" -> Heimlich.OTHER_3
            "heimlich_other_4.gif" -> Heimlich.OTHER_4

            // Shared bandaging
            "alternative.gif" -> Bandaging.ALTERNATIVE

            // Head bandaging
            "headbandage_1.gif" -> Bandaging.HEAD_1
            "headbandage_2.gif" -> Bandaging.HEAD_2
            "headbandage_3.gif" -> Bandaging.HEAD_3
            "headbandage_4.gif" -> Bandaging.HEAD_4
            "headbandage_5.gif" -> Bandaging.HEAD_5
            "headbandage_extra.gif" -> Bandaging.HEAD_EXTRA

            // Arm sling
            "armsling_1.gif" -> Bandaging.ARM_SLING_1
            "armsling_2.gif" -> Bandaging.ARM_SLING_2
            "armsling_3.gif" -> Bandaging.ARM_SLING_3
            "armsling_4.gif" -> Bandaging.ARM_SLING_4
            "armsling_5.gif" -> Bandaging.ARM_SLING_5
            "armsling_6.gif" -> Bandaging.ARM_SLING_6
            "armsling_7.gif" -> Bandaging.ARM_SLING_7

            // Narrow cravat
            "narrowcravat_1.gif" -> Bandaging.NARROW_CRAVAT_1
            "narrowcravat_2.gif" -> Bandaging.NARROW_CRAVAT_2
            "narrowcravat_3.gif" -> Bandaging.NARROW_CRAVAT_3
            "narrowcravat_4.gif" -> Bandaging.NARROW_CRAVAT_4
            "narrowcravat_5.gif" -> Bandaging.NARROW_CRAVAT_5
            "narrowcravat_6.gif" -> Bandaging.NARROW_CRAVAT_6
            "narrowcravat_7.gif" -> Bandaging.NARROW_CRAVAT_7
            "narrowcravat_8.gif" -> Bandaging.NARROW_CRAVAT_8

            // Hand Bandaging (Minor Burns)

            "handbandaging_1.gif" -> Bandaging.HAND_BANDAGING_MINOR_BURNS_1
            "handbandaging_2.gif" -> Bandaging.HAND_BANDAGING_MINOR_BURNS_2
            "handbandaging_3.gif" -> Bandaging.HAND_BANDAGING_MINOR_BURNS_3
            "handbandaging_4.gif" -> Bandaging.HAND_BANDAGING_MINOR_BURNS_4

            //TODO - ADD NEW GIF MAPPINGS FOR THE BANDAGING OF HANDS HERE


            else -> null
        }
    }
}
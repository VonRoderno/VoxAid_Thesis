package com.voxaid.core.common.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-based preferences manager.
 * Handles persistent app settings like first-run disclaimer flag.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voxaid_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val KEY_DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        private val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        private val KEY_AUTO_ADVANCE_ENABLED = booleanPreferencesKey("auto_advance_enabled")
    }

    /**
     * Flow that emits true if user has accepted the disclaimer.
     */
    val disclaimerAccepted: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_DISCLAIMER_ACCEPTED] ?: false
        }

    /**
     * Flow that emits TTS enabled state.
     */
    val ttsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_TTS_ENABLED] ?: true // Default enabled
        }

    /**
     * Flow that emits auto-advance enabled state (for emergency mode).
     */
    val autoAdvanceEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_AUTO_ADVANCE_ENABLED] ?: true // Default enabled
        }

    /**
     * Marks the disclaimer as accepted.
     */
    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DISCLAIMER_ACCEPTED] = accepted
        }
    }

    /**
     * Enables or disables text-to-speech.
     */
    suspend fun setTtsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_TTS_ENABLED] = enabled
        }
    }

    /**
     * Enables or disables auto-advance in emergency mode.
     */
    suspend fun setAutoAdvanceEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_ADVANCE_ENABLED] = enabled
        }
    }
}
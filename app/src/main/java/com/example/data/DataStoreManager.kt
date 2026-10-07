package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.TimerState
import com.example.model.UserAvailability
import com.example.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prostuti_preferences")

class DataStoreManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_AVAILABILITY = stringPreferencesKey("user_availability")
        private val KEY_SETTINGS = stringPreferencesKey("user_settings")
        private val KEY_TIMER_STATE = stringPreferencesKey("timer_state")
    }

    val availabilityFlow: Flow<UserAvailability> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_AVAILABILITY]
        if (raw != null) {
            try {
                json.decodeFromString<UserAvailability>(raw)
            } catch (e: Exception) {
                UserAvailability()
            }
        } else {
            UserAvailability()
        }
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SETTINGS]
        if (raw != null) {
            try {
                json.decodeFromString<UserSettings>(raw)
            } catch (e: Exception) {
                UserSettings()
            }
        } else {
            UserSettings()
        }
    }

    val timerStateFlow: Flow<TimerState> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_TIMER_STATE]
        if (raw != null) {
            try {
                json.decodeFromString<TimerState>(raw)
            } catch (e: Exception) {
                TimerState()
            }
        } else {
            TimerState()
        }
    }

    suspend fun saveAvailability(availability: UserAvailability) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AVAILABILITY] = json.encodeToString(availability)
        }
    }

    suspend fun saveSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SETTINGS] = json.encodeToString(settings)
        }
    }

    suspend fun saveTimerState(timerState: TimerState) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TIMER_STATE] = json.encodeToString(timerState)
        }
    }
}

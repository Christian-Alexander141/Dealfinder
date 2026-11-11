package com.dealfinder.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dealfinder.data.models.UserType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore for app preferences
 */
class AppPreferences(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dealfinder_prefs")
        
        // Preference keys
        private val KEY_CURRENT_USER_ID = longPreferencesKey("current_user_id")
        private val KEY_CURRENT_USER_NAME = stringPreferencesKey("current_user_name")
        private val KEY_CURRENT_USER_EMAIL = stringPreferencesKey("current_user_email")
        private val KEY_CURRENT_USER_TYPE = stringPreferencesKey("current_user_type")
        private val KEY_IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        private val KEY_LAST_LOCATION_LAT = longPreferencesKey("last_location_lat")
        private val KEY_LAST_LOCATION_LON = longPreferencesKey("last_location_lon")
    }
    
    private val dataStore = context.dataStore
    
    // Current user
    val currentUserId: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[KEY_CURRENT_USER_ID]
    }
    
    val currentUserName: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_CURRENT_USER_NAME]
    }
    
    val currentUserEmail: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_CURRENT_USER_EMAIL]
    }
    
    val currentUserType: Flow<UserType?> = dataStore.data.map { prefs ->
        prefs[KEY_CURRENT_USER_TYPE]?.let { UserType.valueOf(it) }
    }
    
    val isOnboarded: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_IS_ONBOARDED] ?: false
    }
    
    // Location
    val lastLocationLat: Flow<Double?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_LOCATION_LAT]?.let { java.lang.Double.longBitsToDouble(it) }
    }
    
    val lastLocationLon: Flow<Double?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_LOCATION_LON]?.let { java.lang.Double.longBitsToDouble(it) }
    }
    
    /**
     * Set current user
     */
    suspend fun setCurrentUser(id: Long, name: String, email: String, userType: UserType) {
        dataStore.edit { prefs ->
            prefs[KEY_CURRENT_USER_ID] = id
            prefs[KEY_CURRENT_USER_NAME] = name
            prefs[KEY_CURRENT_USER_EMAIL] = email
            prefs[KEY_CURRENT_USER_TYPE] = userType.name
        }
    }
    
    /**
     * Clear current user
     */
    suspend fun clearCurrentUser() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_CURRENT_USER_ID)
            prefs.remove(KEY_CURRENT_USER_NAME)
            prefs.remove(KEY_CURRENT_USER_EMAIL)
            prefs.remove(KEY_CURRENT_USER_TYPE)
        }
    }
    
    /**
     * Mark user as onboarded
     */
    suspend fun markAsOnboarded() {
        dataStore.edit { prefs ->
            prefs[KEY_IS_ONBOARDED] = true
        }
    }
    
    /**
     * Set last known location
     */
    suspend fun setLastLocation(lat: Double, lon: Double) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_LOCATION_LAT] = java.lang.Double.doubleToLongBits(lat)
            prefs[KEY_LAST_LOCATION_LON] = java.lang.Double.doubleToLongBits(lon)
        }
    }
    
    /**
     * Clear all preferences
     */
    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

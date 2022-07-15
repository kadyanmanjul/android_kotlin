package com.joshtalks.badebhaiya.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.joshtalks.badebhaiya.core.AppObjectController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bb_datastore")

object BbDatastore {
    private val ROOM_REQUEST_COUNT = intPreferencesKey("room_request_count")

    val roomRequestCount: Flow<Int> = AppObjectController.joshApplication.dataStore.data
        .map { preferences ->
            // No type safety.
            preferences[ROOM_REQUEST_COUNT] ?: 0
        }

    suspend fun updateRoomRequestCount(count: Int){
            AppObjectController.joshApplication.dataStore.edit { pref ->
                pref[ROOM_REQUEST_COUNT] = count
            }

    }
}
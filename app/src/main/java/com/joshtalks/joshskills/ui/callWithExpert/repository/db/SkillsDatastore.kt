package com.joshtalks.joshskills.ui.callWithExpert.repository.db

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.joshtalks.joshskills.core.AppObjectController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "skills_datastore")

object SkillsDatastore {

    private val WALLET_CREDITS = intPreferencesKey("wallet_credits")

    val walletCredits: Flow<Int> = AppObjectController.joshApplication.dataStore.data
        .map { preferences ->
            // No type safety.
            preferences[WALLET_CREDITS] ?: 0
        }

    suspend fun updateWalletCredits(credits: Int) {
        AppObjectController.joshApplication.dataStore.edit { pref ->
            pref[WALLET_CREDITS] = credits
        }

    }


}
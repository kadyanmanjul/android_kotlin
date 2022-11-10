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

    private val WALLET_AMOUNT = intPreferencesKey("wallet_credits")
    private val EXPERT_CREDITS = intPreferencesKey("expert_credits")

    val walletAmount: Flow<Int> = AppObjectController.joshApplication.dataStore.data.map { preferences ->
            // No type safety.
            preferences[WALLET_AMOUNT] ?: 0
        }

    val expertCredits: Flow<Int> = AppObjectController.joshApplication.dataStore.data.map {
        it[EXPERT_CREDITS] ?: -1
    }

    suspend fun updateWalletAmount(amount: Int) {
        AppObjectController.joshApplication.dataStore.edit { pref ->
            pref[WALLET_AMOUNT] = amount
        }
    }

    suspend fun updateExpertCredits(credits: Int) {
        AppObjectController.joshApplication.dataStore.edit { pref ->
            pref[EXPERT_CREDITS] = credits
        }
    }
}
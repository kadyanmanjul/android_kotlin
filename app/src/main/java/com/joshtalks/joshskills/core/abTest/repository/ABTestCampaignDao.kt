package com.joshtalks.joshskills.core.abTest.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.google.firebase.inject.Deferred
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData

@Dao
interface ABTestCampaignDao {

    @Insert
    suspend fun insertCampaign(data: ABTestCampaignData)

    @Insert
    suspend fun insertCampaigns(data: List<ABTestCampaignData>)

    @Update
    suspend fun updateCampaign(campaign: ABTestCampaignData)

    @Transaction
    @Query("SELECT * from ab_test_campaigns")
    suspend fun getAllABTestCampaigns(): List<ABTestCampaignData>?

    @Transaction
    @Query("SELECT * from ab_test_campaigns WHERE `campaign_key` =:key ")
    suspend fun getABTestCampaign(key:String): ABTestCampaignData?

    @Delete
    suspend fun deleteSingleCampaign(campaign: ABTestCampaignData)

    @Query("DELETE FROM ab_test_campaigns")
    suspend fun deleteAllCampaigns() : Deferred<Any>
}

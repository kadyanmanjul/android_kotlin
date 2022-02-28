package com.joshtalks.joshskills.core.abTest
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.VariableMapConverters

@Entity(tableName = "ab_test_campaigns")
data class ABTestCampaignData(
    @ColumnInfo(name = "is_campaign_active")
    @SerializedName("is_campaign_active")
    val isCampaignActive: Boolean,
    @PrimaryKey
    @ColumnInfo(name = "campaign_key")
    @SerializedName("campaign_key")
    val campaignKey: String,
    @ColumnInfo(name = "variant_key")
    @SerializedName("variant_key")
    val variantKey: String?,

    @TypeConverters(VariableMapConverters::class)
    @ColumnInfo(name = "variable_map")
    @SerializedName("variable_map")
    val variableMap: VariableMap?
)

data class VariableMap(
    @SerializedName("color")
    val color: String?,
    @SerializedName("is_enabled")
    val isEnabled: Boolean?,
    @SerializedName("position")
    val position: String?
)
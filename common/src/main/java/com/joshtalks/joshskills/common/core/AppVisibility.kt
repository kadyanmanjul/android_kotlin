package com.joshtalks.joshskills.common.core

interface ApplicationDetails {
    fun isAppVisual() : Boolean
    fun versionName() : String
    fun versionCode() : Int
    fun applicationId() : String
}
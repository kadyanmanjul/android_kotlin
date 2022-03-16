package com.joshtalks.badebhaiya.core

import android.app.Application

class JoshApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        AppObjectController.init(this)
    }
}
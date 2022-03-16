package com.joshtalks.badebhaiya.core

import android.content.Context
import com.joshtalks.badebhaiya.repository.service.initStethoLibrary

class AppObjectController {
    companion object {
        @JvmStatic
        lateinit var joshApplication: JoshApplication
            private set

        fun init(context: Context) {
            joshApplication = context as JoshApplication
            initStethoLibrary(context)
        }
    }
}
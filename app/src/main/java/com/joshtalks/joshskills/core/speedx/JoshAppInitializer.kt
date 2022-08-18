package com.joshtalks.joshskills.core.speedx

import android.content.Context
import androidx.startup.Initializer
import com.joshtalks.joshskills.core.AppObjectController

class JoshAppInitializer : Initializer<AppObjectController> {

    override fun create(context: Context): AppObjectController {
        return AppObjectController.initLibrary(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        mutableListOf(JoshWorkManagerInitializer::class.java)

}
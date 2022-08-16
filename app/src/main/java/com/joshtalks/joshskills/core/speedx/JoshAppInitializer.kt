package com.joshtalks.joshskills.core.speedx

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.joshtalks.joshskills.core.AppObjectController

class JoshAppInitializer : Initializer<AppObjectController> {

    override fun create(context: Context): AppObjectController {
        Log.e("SukeshTest", "Initializing Initializer")
        return AppObjectController.initLibrary(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        emptyList()

}
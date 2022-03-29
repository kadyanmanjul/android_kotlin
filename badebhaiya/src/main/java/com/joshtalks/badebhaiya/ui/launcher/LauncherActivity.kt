package com.joshtalks.badebhaiya.ui.launcher

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.workers.WorkManagerAdmin
import io.branch.referral.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity(), LifecycleObserver {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_launcher)
    }

    private fun initApp() {
        lifecycleScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(applicationContext).cancelAllWork()
            WorkManagerAdmin.appInitWorker()
            Branch.getInstance(applicationContext).resetUserSession()
        }
    }
}

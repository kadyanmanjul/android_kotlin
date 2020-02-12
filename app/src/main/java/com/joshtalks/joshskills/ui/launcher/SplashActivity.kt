package com.joshtalks.joshskills.ui.launcher

import android.accounts.Account
import android.accounts.AccountManager
import com.joshtalks.joshskills.core.BaseActivity

class SplashActivity : BaseActivity(){

    fun on(){
        val am: AccountManager = AccountManager.get(this) // "this" references the current Context

        val accounts: Array<out Account> = am.getAccountsByType("com.google")
    }

}
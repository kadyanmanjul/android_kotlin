package com.joshtalks.joshskills.base

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

//fun AppCompatActivity.onMultipleBackPress() {
//    if(backPressMutex.isLocked) {
//        Log.d(TAG, "onBackPressed: backPressMutex?.isLocked")
//        viewModel.saveImpression(IMPRESSION_SEARCHING_SCREEN_BACK_PRESS)
//        stopSearching(DISCONNECT.BACK_BUTTON_FAILURE)
//    } else {
//        Toast.makeText(this,"Please press back again", Toast.LENGTH_SHORT).show()
//        CoroutineScope(Dispatchers.Main).launch {
//            backPressMutex.withLock {
//                delay(1000)
//            }
//        }
//    }
//}
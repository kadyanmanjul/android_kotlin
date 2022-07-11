package com.joshtalks.badebhaiya.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity

fun Activity.open(
    context: Context
) {
    Intent(context, this::class.java).also {
       context.startActivity(it)
    }
}
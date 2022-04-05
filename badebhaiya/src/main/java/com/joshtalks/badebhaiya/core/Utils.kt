package com.joshtalks.badebhaiya.core

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.muddzdev.styleabletoast.StyleableToast
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil

fun hideKeyboard(activity: Activity, view: View) {
    val inputManager: InputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Handler(Looper.getMainLooper()).post {
        StyleableToast.Builder(AppObjectController.joshApplication)
            .gravity(Gravity.BOTTOM)
            .text(message)
            .cornerRadius(16)
            .length(length)
            .solidBackground()
            .show()
    }
}

fun isValidFullNumber(countryCode: String, number: String? = EMPTY): Boolean {
    return try {
        val phoneUtil = PhoneNumberUtil.createInstance(AppObjectController.joshApplication)
        val phoneNumber = phoneUtil.parse(countryCode + number, null)
        phoneUtil.isValidNumber(phoneNumber)
    } catch (ex: Exception) {
        ex.printStackTrace()
        true
    }
}
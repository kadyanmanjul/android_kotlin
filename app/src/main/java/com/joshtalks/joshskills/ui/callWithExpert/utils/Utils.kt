package com.joshtalks.joshskills.ui.callWithExpert.utils

import android.view.View

fun String.toRupees() = "₹ $this"

fun String.toPlusRupees() = "+₹ $this"

fun String.toMinusRupees() = "-₹ $this"

fun Int.toRupees() = "₹ $this"

fun Long.toRupees() = "₹ $this"

fun String.removeRupees() = this.removePrefix("₹ ")

fun String.removeNegative() = this.removePrefix("-")

fun View.visible(){
    this.visibility = View.VISIBLE
}

fun View.gone(){
    this.visibility = View.GONE
}
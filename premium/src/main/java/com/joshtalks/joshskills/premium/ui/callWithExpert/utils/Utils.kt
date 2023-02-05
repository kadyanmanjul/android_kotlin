package com.joshtalks.joshskills.premium.ui.callWithExpert.utils

import android.view.View

fun String.toRupees() = "₹ $this"

fun String.toPlusRupees() = "+₹ $this"

fun String.toMinusRupees() = "-₹ $this"

fun Int.toRupees() = "₹ $this"

fun Long.toRupees() = "₹ $this"

fun String.removeRupees() = this.removePrefix("₹ ")

fun String.removeNegative() = this.removePrefix("-")

fun String.replaceRupees() = this.replace("₹","")

fun String.toRupeesWithoutSpace() = "₹$this"

fun View.visible(){
    this.visibility = View.VISIBLE
}

fun View.gone(){
    this.visibility = View.GONE
}
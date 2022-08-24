package com.joshtalks.joshskills.ui.callWithExpert.utils

fun String.toRupees() = "₹ $this"

fun Int.toRupees() = "₹ $this"

fun Long.toRupees() = "₹ $this"

fun String.removeRupees() = this.removePrefix("₹ ")
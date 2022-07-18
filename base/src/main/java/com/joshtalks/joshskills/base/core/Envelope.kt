package com.joshtalks.joshskills.base.core

data class Envelope<T : Enum<T>>(val type: T, val data: Any? = null)
package com.joshtalks.joshskills.util

import androidx.databinding.Bindable
import java.util.*

class BindableString : BaseObservable() {
    private var value: String? = null
        @Bindable get


    val isEmpty: Boolean
        get() = value == null || value!!.isEmpty()

    fun get(): String {
        return value ?: ""
    }

    fun set(value: String) {
        if (!Objects.equals(this.value, value)) {
            this.value = value
            notifyChange()
        }
    }
}
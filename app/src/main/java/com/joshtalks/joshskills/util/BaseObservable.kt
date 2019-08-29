package com.joshtalks.joshskills.util

import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.PropertyChangeRegistry


open class BaseObservable : Observable {

    @Transient
    private var mCallbacks: PropertyChangeRegistry? = null

    @Synchronized
    override fun addOnPropertyChangedCallback(listener: OnPropertyChangedCallback) {
        if (this.mCallbacks == null) {
            this.mCallbacks = PropertyChangeRegistry()
        }

        this.mCallbacks!!.add(listener)
    }

    @Synchronized
    override fun removeOnPropertyChangedCallback(listener: OnPropertyChangedCallback) {
        if (this.mCallbacks != null) {
            this.mCallbacks!!.remove(listener)
        }
    }

    @Synchronized
    fun notifyChange() {
        if (this.mCallbacks != null) {
            this.mCallbacks!!.notifyCallbacks(this, 0, null)
        }
    }

    fun notifyPropertyChanged(fieldId: Int) {
        if (this.mCallbacks != null) {
            this.mCallbacks!!.notifyCallbacks(this, fieldId, null)
        }
    }
}
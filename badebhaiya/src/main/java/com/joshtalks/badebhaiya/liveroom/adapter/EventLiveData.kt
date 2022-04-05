package com.joshtalks.badebhaiya.liveroom.adapter

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation.
 */

import android.os.Message
import androidx.annotation.MainThread
import androidx.collection.ArraySet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

object EventLiveData : MediatorLiveData<Message>() {

    private val observers = ArraySet<ObserverWrapper<in Message>>()

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in Message>) {
        val wrapper = ObserverWrapper(observer)
        observers.add(wrapper)
        super.observe(owner, wrapper)
    }

    @MainThread
    override fun removeObserver(observer: Observer<in Message>) {
        if (observer is ObserverWrapper && observers.remove(observer)) {
            super.removeObserver(observer)
            return
        }

        val iterator = observers.iterator()
        while (iterator.hasNext()) {
            val wrapper = iterator.next()
            if (wrapper.observer == observer) {
                iterator.remove()
                super.removeObserver(wrapper)
                break
            }
        }
    }

    @MainThread
    override fun setValue(t: Message?) {
        observers.forEach { it.newValue() }
        super.postValue(t)
    }

    private class ObserverWrapper<T>(val observer: Observer<T>) : Observer<T> {

        private var pending = false

        override fun onChanged(t: T?) {
            if (pending) {
                pending = false
                observer.onChanged(t)
            }
        }

        fun newValue() {
            pending = true
        }
    }
}
package com.joshtalks.badebhaiya.core

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

object RxBus2 {
    private val publisher = PublishSubject.create<Any>()

    @JvmStatic
    fun publish(event: Any) {
        publisher.toSerialized().onNext(event)
    }

    // Listen should return an Observable and not the publisher
    // Using ofType we filter only events that match that class type
    fun <T> listen(eventType: Class<T>): Observable<T> =
        publisher.ofType(eventType).debounce(500, TimeUnit.MILLISECONDS)

    fun <T> listenWithoutDelay(eventType: Class<T>): Observable<T> =
        publisher.ofType(eventType)
}

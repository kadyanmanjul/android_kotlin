package com.joshtalks.joshskills.messaging

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

object RxBus2 {
    private val publisher = PublishSubject.create<Any>()

    @JvmStatic
    fun publish(event: Any) {
        publisher.toSerialized().onNext(event)
        // publisher.onNext(event)
    }

    // Listen should return an Observable and not the publisher
    // Using ofType we filter only events that match that class type
    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}
package com.joshtalks.joshskills.messaging;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;


public final class RxBus {
    private final PublishSubject<Object> bus = PublishSubject.create();

    //private final Relay<Object> bus = PublishRelay.create().toSerialized();
    private static volatile RxBus defaultInstance;

    private RxBus() {
    }

    public static RxBus getDefault() {
        if (defaultInstance == null) {
            synchronized (RxBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new RxBus();
                }
            }
        }
        return defaultInstance;
    }


    public void send(Object event) {
        //bus.accept(event);
        bus.onNext(event);
    }

    public Observable<Object> toObservable() {
        return bus;
    }

    public boolean hasObservers() {
        return bus.hasObservers();
    }
}
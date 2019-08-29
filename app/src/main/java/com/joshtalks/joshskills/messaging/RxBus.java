package com.joshtalks.joshskills.messaging;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import io.reactivex.Observable;


public final class RxBus {
    private final Relay<Object> bus = PublishRelay.create().toSerialized();
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
        bus.accept(event);
    }

    public Observable<Object> toObservable() {
        return bus;
    }

    public boolean hasObservers() {
        return bus.hasObservers();
    }
}
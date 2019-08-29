package com.joshtalks.joshskills.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.joshtalks.joshskills.messaging.RxBus
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    private var compositeDisposable = CompositeDisposable()


    init {
        addObserver()
    }


    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    private fun addObserver() {
        compositeDisposable.add(
            RxBus.getDefault().toObservable()
                .subscribeOn(Schedulers.io()).subscribe({


                }, {


                })
        )
    }


}
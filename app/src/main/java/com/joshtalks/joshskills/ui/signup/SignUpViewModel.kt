package com.joshtalks.joshskills.ui.signup

import android.app.Application
import android.text.Editable
import android.text.TextWatcher
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.SignUpStepStatus
import com.joshtalks.joshskills.util.BindableString


class SignUpViewModel : AndroidViewModel {

    var context: JoshApplication = getApplication()
    var text = BindableString()

    var phoneNumber = ""
    var phoneNumberObservable = ObservableField<String>()



    var phoneNumberTextWatcher: TextWatcher = object : TextWatcher {

        override fun afterTextChanged(s: Editable?) {

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }


    }
    val signUpStatus = MutableLiveData<SignUpStepStatus>()


    constructor(application: Application) : super(application) {
    }


    fun registerPhone() {
        signUpStatus.postValue(SignUpStepStatus.SignUpStepSecond)

    }

    fun signUp() {
        signUpStatus.postValue(SignUpStepStatus.SignUpStepFirst)
    }



}

package com.fxn.pixeditor.imageeditengine.interfaces

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import java.io.Serializable

open interface AddMoreImagesListener : Serializable {
    fun addMore(list: ArrayList<String>, requestCodePix: Int)
}
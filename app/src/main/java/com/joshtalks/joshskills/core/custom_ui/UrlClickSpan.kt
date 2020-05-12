package com.joshtalks.joshskills.core.custom_ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.provider.Browser
import android.text.ParcelableSpan
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.View
import com.joshtalks.joshskills.core.interfaces.OnUrlClickSpanListener


class UrlClickSpan : ClickableSpan, ParcelableSpan {
    /**
     * Get the url string for this span.
     *
     * @return the url string.
     */
    val uRL: String?

    private var onUrlClickSpanListener: OnUrlClickSpanListener? = null

    /**
     * Constructs a [URLSpan] from a url string.
     *
     * @param url the url string
     */
    constructor(
        url: String?,
        listener: OnUrlClickSpanListener? = null
    ) {
        uRL = url
        this.onUrlClickSpanListener = listener
    }

    /**
     * Constructs a [URLSpan] from a parcel.
     */
    constructor(src: Parcel) {
        uRL = src.readString()
    }

    override fun getSpanTypeId(): Int {
        return spanTypeIdInternal
    }

    /** @hide
     */
    val spanTypeIdInternal: Int
        get() = 11

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        writeToParcelInternal(dest, flags)
    }

    /** @hide
     */
    fun writeToParcelInternal(dest: Parcel, flags: Int) {
        dest.writeString(uRL)
    }

    override fun onClick(widget: View) {
        val uri = Uri.parse(uRL)

        if (onUrlClickSpanListener != null) {
            onUrlClickSpanListener?.onClick(uri)
        } else {
            val context = widget.context
            val intent =
                Intent(Intent.ACTION_VIEW, uri)
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.w("URLSpan", "Actvity was not found for intent, $intent")
            }
        }
    }
}

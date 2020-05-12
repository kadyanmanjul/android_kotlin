package com.joshtalks.joshskills.core.interfaces

import android.net.Uri
import com.joshtalks.joshskills.repository.server.CertificateDetail

interface OnDismissDialog {
    fun onDismiss()
}

interface OnDismissClaimCertificateDialog {
    fun onDismiss(certificateDetail: CertificateDetail?)
}

interface OnUrlClickSpanListener {
    fun onClick(uri: Uri)
}

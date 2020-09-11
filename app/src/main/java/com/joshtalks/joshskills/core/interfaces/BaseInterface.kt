package com.joshtalks.joshskills.core.interfaces

import android.net.Uri
import com.joshtalks.joshskills.core.VerificationVia
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

interface OnSelectVerificationMethodListener {
    fun onSelect(verificationVia: VerificationVia)
}

interface OnConversationPractiseSubmit {
    fun onDone()
    fun onCancel()
}

interface OnDismissWithDialog : OnDismissDialog {
    fun onSuccessDismiss()
    fun onCancel()
}
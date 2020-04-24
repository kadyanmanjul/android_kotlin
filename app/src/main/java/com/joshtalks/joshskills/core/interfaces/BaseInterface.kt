package com.joshtalks.joshskills.core.interfaces

import com.joshtalks.joshskills.repository.server.CertificateDetail

interface OnDismissDialog {
    fun onDismiss()
}

interface OnDismissClaimCertificateDialog {
    fun onDismiss(certificateDetail: CertificateDetail?)
}

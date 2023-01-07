package com.joshtalks.joshskills.common.core.interfaces

import com.joshtalks.joshskills.common.core.VerificationVia
import com.joshtalks.joshskills.common.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.common.repository.server.CertificateDetail

interface OnDismissDialog {
    fun onDismiss() {}
}

interface OnDismissClaimCertificateDialog {
    fun onDismiss(certificateDetail: CertificateDetail?)
}

interface OnSelectVerificationMethodListener {
    fun onSelect(verificationVia: VerificationVia)
}

interface OnConversationPractiseSubmit {
    fun onDone()
    fun onCancel()
}

interface OnDismissWithDialog : OnDismissDialog {
    fun onSuccessDismiss() {}
    fun onCancel() {}
}

interface OnDismissWithSuccess : OnDismissDialog {
    fun onSuccessDismiss()
}

interface CertificationExamListener {
    fun onPauseExit()
    fun onFinishExam()
    fun onClose()
    fun onGoToQuestion(position: Int)
}

interface OnOpenCourseListener {
    fun onClick(inboxEntity: InboxEntity)
    fun onStartTrialTimer(startTimeInMilliSeconds: Long)
    fun onStopTrialTimer()
    fun onFreeTrialEnded()
}

interface FileDownloadCallback : OnDismissWithDialog {
    fun downloadedFile(path: String) {}
    fun webURL(path: String, localUrl: String) {}
}

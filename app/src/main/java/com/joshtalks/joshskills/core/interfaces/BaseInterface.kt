package com.joshtalks.joshskills.core.interfaces

import android.net.Uri
import android.view.View
import com.joshtalks.joshskills.core.VerificationVia
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
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

interface OnDismissWithSuccess : OnDismissDialog {
    fun onSuccessDismiss()
}


interface CertificationExamListener {
    fun onPauseExit()
    fun onFinishExam()
    fun onClose()
    fun onGoToQuestion(position: Int)
}

interface OnOpenCourseListener{
    fun onClick(inboxEntity: InboxEntity)
}

interface RecyclerViewItemClickListener {
    fun onItemClick(view: View?, position: Int)
    fun onItemLongClick(view: View?, position: Int)
}


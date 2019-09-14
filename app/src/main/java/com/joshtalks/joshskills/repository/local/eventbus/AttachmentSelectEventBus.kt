package com.joshtalks.joshskills.repository.local.eventbus


data class AttachmentSelectEventBus(var attachmentType: AttachmentType)


enum class AttachmentType {
    IMAGE, AUDIO,GALLERY

}



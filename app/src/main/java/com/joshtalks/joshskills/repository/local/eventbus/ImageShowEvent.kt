package com.joshtalks.joshskills.repository.local.eventbus

data class ImageShowEvent (var localPath:String?,var serverPath:String?,var imageId:String?=null)
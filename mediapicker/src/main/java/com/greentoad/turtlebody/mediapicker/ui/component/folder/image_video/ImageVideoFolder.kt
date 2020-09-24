package com.greentoad.turtlebody.mediapicker.ui.component.folder.image_video

data class ImageVideoFolder(var id: String = "",
                            var name: String = "",
                            var coverImageFilePath: String = "",
                            var contentCount: Int = 0,
                            var mediaType: Int = 0
                            ) {

    companion object {
        val FOLDER_ID = "folderId"
    }
}

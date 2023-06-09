package com.greentoad.turtlebody.mediapicker.core

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider.getUriForFile
import com.greentoad.turtlebody.mediapicker.MediaPicker
import com.greentoad.turtlebody.mediapicker.ui.component.folder.audio.AudioFolder
import com.greentoad.turtlebody.mediapicker.ui.component.folder.image_video.ImageVideoFolder
import com.greentoad.turtlebody.mediapicker.ui.component.media.audio.AudioModel
import com.greentoad.turtlebody.mediapicker.ui.component.media.audiovideo.DefaultModel
import com.greentoad.turtlebody.mediapicker.ui.component.media.image.ImageModel
import com.greentoad.turtlebody.mediapicker.ui.component.media.video.VideoModel
import java.io.File
import java.util.*


private const val MEDIA_PATTERN = "([^\\s]+(\\.(?i)(jpg|png|gif|bmp|mp4|MP4)|WEBP|webp|JPEG|PNG|Jpeg)$)"
private val regex = Regex(pattern = MEDIA_PATTERN)

object FileManager {

    fun fetchImageAndVideoFolders(context: Context): ArrayList<ImageVideoFolder> {
        val folders = ArrayList<ImageVideoFolder>()
        val folderFileCountMap = HashMap<String, Int>()
        val projection = MediaConstants.Projection.IMAGE_VIDEO_FOLDER
        val cursor = CursorHelper.getImageAndVideoFolderCursor(context)

        cursor?.let {
            val columnIndexFolderId = it.getColumnIndexOrThrow(projection[0])
            val columnIndexFolderName = it.getColumnIndexOrThrow(projection[1])
            val columnIndexFilePath = it.getColumnIndexOrThrow(projection[2])

            while (it.moveToNext()) {
                val folderId = it.getString(columnIndexFolderId)
                if (folderFileCountMap.containsKey(folderId)) {
                    folderFileCountMap[folderId] = folderFileCountMap[folderId]!! + 1
                } else {
                    val folder = ImageVideoFolder(
                        folderId,
                        it.getString(columnIndexFolderName),
                        it.getString(columnIndexFilePath),
                        0
                    )
                    folders.add(folder)
                    folderFileCountMap[folderId] = 1
                }
            }
            for (fdr in folders) {
                fdr.contentCount = folderFileCountMap[fdr.id]!!
            }
            cursor.close()
        }
        return folders
    }

    fun getImageVideoFilesInFolder(context: Context, folderId: String): ArrayList<DefaultModel> {
        val fileItems = ArrayList<DefaultModel>()
        val cursor: Cursor? = CursorHelper.getImageAndVideoFileCursor(context, folderId)

        cursor?.let {

            val columnIndexFileId = it.getColumnIndex("_id")
            val columnIndexFileName = it.getColumnIndex("_display_name")
            val columnIndexFileSize = it.getColumnIndex("_size")
            val columnIndexFilePath = it.getColumnIndex("_data")
            val columnIndexFileThumbPath = it.getColumnIndex("_data")
            val columnIndexFileDuration = it.getColumnIndex("duration")
            val columnIndexFileMimeType = it.getColumnIndex("mime_type")
            val columnIndexFileTitle = it.getColumnIndex("title")

            while (it.moveToNext()) {
                try {
                    val fileItem = DefaultModel(
                        it.getString(columnIndexFileId),
                        it.getString(columnIndexFileName),
                        it.getInt(columnIndexFileSize),
                        it.getString(columnIndexFilePath),
                        it.getString(columnIndexFileThumbPath)
                    )

                    if (fileItem.name.isNullOrEmpty()) {
                        fileItem.name = it.getString(columnIndexFileTitle)
                    }

                    try {
                        if (columnIndexFileDuration > -1) {
                            fileItem.duration = it.getInt(columnIndexFileDuration)
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }

                    if (columnIndexFileMimeType > -1) {
                        fileItem.fileType = it.getString(columnIndexFileMimeType)
                    }
                    if (regex.matches(fileItem.name!!)) {
                        fileItems.add(fileItem)
                    }

                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            cursor.close()
        }
        return fileItems
    }


    /**
     *   For ImageModel
     */
    fun fetchImageFolders(context: Context): ArrayList<ImageVideoFolder> {
        val folders = ArrayList<ImageVideoFolder>()
        val folderFileCountMap = HashMap<String, Int>()

        val projection = MediaConstants.Projection.IMAGE_FOLDER

        //Create the cursor pointing to the SDCard
        val cursor = CursorHelper.getImageVideoFolderCursor(context, MediaPicker.MediaTypes.IMAGE)

        cursor?.let {
            val columnIndexFolderId = it.getColumnIndexOrThrow(projection[0])
            val columnIndexFolderName = it.getColumnIndexOrThrow(projection[1])
            val columnIndexFilePath = it.getColumnIndexOrThrow(projection[2])

            while (it.moveToNext()) {
                val folderId = it.getString(columnIndexFolderId)
                if (folderFileCountMap.containsKey(folderId)) {
                    folderFileCountMap[folderId] = folderFileCountMap[folderId]!! + 1
                } else {
                    val folder = ImageVideoFolder(
                        folderId,
                        it.getString(columnIndexFolderName),
                        it.getString(columnIndexFilePath),
                        0
                    )
                    folder.mediaType = 501
                    folders.add(folder)
                    folderFileCountMap[folderId] = 1
                }
            }
            for (fdr in folders) {
                fdr.contentCount = folderFileCountMap[fdr.id]!!
            }
            cursor.close()
        }
        return folders
    }

    fun getImageFilesInFolder(context: Context, folderId: String): ArrayList<ImageModel> {
        val fileItems = ArrayList<ImageModel>()
        val projection = MediaConstants.Projection.IMAGE_FILE

        //Create the cursor pointing to the SDCard
        val cursor: Cursor? =
            CursorHelper.getImageVideoFileCursor(context, folderId, MediaPicker.MediaTypes.IMAGE)

        cursor?.let {
            val columnIndexFileId = it.getColumnIndexOrThrow(projection[0])
            val columnIndexFileName = it.getColumnIndexOrThrow(projection[1])
            val columnIndexFileSize = it.getColumnIndexOrThrow(projection[2])
            val columnIndexFilePath = it.getColumnIndexOrThrow(projection[3])
            val columnIndexFileThumbPath = it.getColumnIndexOrThrow(projection[5])

            while (it.moveToNext()) {
                val fileItem = ImageModel(
                    it.getString(columnIndexFileId),
                    it.getString(columnIndexFileName),
                    it.getInt(columnIndexFileSize),
                    it.getString(columnIndexFilePath),
                    it.getString(columnIndexFileThumbPath), false
                )

                fileItems.add(fileItem)

            }
            cursor.close()
        }
        return fileItems
    }


    /**
     * For VideoModel
     */
    fun fetchVideoFolders(context: Context): ArrayList<ImageVideoFolder> {
        val folders = ArrayList<ImageVideoFolder>()
        val folderFileCountMap = HashMap<String, Int>()

        val projection = MediaConstants.Projection.VIDEO_FOLDER

        //Create the cursor pointing to the SDCard
        val cursor = CursorHelper.getImageVideoFolderCursor(context, MediaPicker.MediaTypes.VIDEO)

        cursor?.let {
            val columnIndexFolderId = it.getColumnIndexOrThrow(projection[0])
            val columnIndexFolderName = it.getColumnIndexOrThrow(projection[1])
            val columnIndexFilePath = it.getColumnIndexOrThrow(projection[2])

            while (it.moveToNext()) {
                val folderId = it.getString(columnIndexFolderId)
                if (folderFileCountMap.containsKey(folderId)) {
                    folderFileCountMap[folderId] = folderFileCountMap[folderId]!! + 1
                } else {
                    val folder = ImageVideoFolder(
                        folderId,
                        it.getString(columnIndexFolderName),
                        it.getString(columnIndexFilePath),
                        0
                    )
                    folder.mediaType = 502

                    folders.add(folder)
                    folderFileCountMap[folderId] = 1
                }
            }
            for (fdr in folders) {
                fdr.contentCount = folderFileCountMap[fdr.id]!!
            }
            cursor.close()
        }
        return folders
    }

    fun getVideoFilesInFolder(context: Context, folderId: String): ArrayList<VideoModel> {
        val fileItems = ArrayList<VideoModel>()
        val projection = MediaConstants.Projection.VIDEO_FILE

        //Create the cursor pointing to the SDCard
        val cursor: Cursor? =
            CursorHelper.getImageVideoFileCursor(context, folderId, MediaPicker.MediaTypes.VIDEO)

        cursor?.let {
            val columnIndexFileId = it.getColumnIndexOrThrow(projection[0])
            val columnIndexFileName = it.getColumnIndexOrThrow(projection[1])
            val columnIndexFileSize = it.getColumnIndexOrThrow(projection[2])
            val columnIndexFilePath = it.getColumnIndexOrThrow(projection[3])
            val columnIndexFileThumbPath = it.getColumnIndexOrThrow(projection[5])
            val columnIndexFileDuration = it.getColumnIndexOrThrow(projection[6])

            while (it.moveToNext()) {
                val fileItem = VideoModel(
                    it.getString(columnIndexFileId),
                    it.getString(columnIndexFileName),
                    it.getInt(columnIndexFileSize),
                    it.getString(columnIndexFilePath),
                    it.getString(columnIndexFileThumbPath),
                    it.getString(columnIndexFileDuration), false
                )
                fileItems.add(fileItem)
            }
            cursor.close()
        }
        return fileItems
    }


    /**
     * For audio
     */
    fun fetchAudioFolderList(context: Context): ArrayList<AudioFolder> {
        val folders = ArrayList<AudioFolder>()
        val folderFileCountMap = HashMap<String, Int>()

        val projection = MediaConstants.Projection.AUDIO_FOLDER

        // Create the cursor pointing to the SDCard
        val cursor = CursorHelper.getAudioFolderCursor(context)

        cursor?.let {
            val columnIndexFolderId = it.getColumnIndexOrThrow(projection[0])
            val columnIndexCount = it.getColumnIndexOrThrow("dataCount")
            val columnIndexFolderParent = it.getColumnIndexOrThrow(projection[1])
            val columnIndexFilePath = it.getColumnIndexOrThrow(projection[2])
            val columnIndexDisplayName = it.getColumnIndexOrThrow(projection[3])

            while (it.moveToNext()) {
                val folderId = it.getString(columnIndexFolderId)
                val audioCounts = it.getInt(columnIndexCount)
                if (folderFileCountMap.containsKey(folderId)) {
                    folderFileCountMap[folderId] = folderFileCountMap[folderId]!! + audioCounts
                } else {
                    val folder = AudioFolder(
                        folderId, File(it.getString(columnIndexFilePath)).parentFile.name,
                        File(it.getString(columnIndexFilePath)).parentFile.path, 0
                    )
                    folders.add(folder)
                    folderFileCountMap[folderId] = audioCounts
                }
            }
            for (fdr in folders) {
                fdr.contentCount = folderFileCountMap[fdr.id]!!
            }
            cursor.close()
        }
        return folders
    }


    fun getAudioFilesInFolder(context: Context, folderPath: String): ArrayList<AudioModel> {
        val fileItems = ArrayList<AudioModel>()
        val projection = MediaConstants.Projection.AUDIO_FILE

        // Create the cursor pointing to the SDCard
        val cursor: Cursor? = CursorHelper.getAudioFilesInFolderCursor(context, folderPath)

        cursor?.let {
            Log.e("infgo", "audio query size: " + cursor.count)
            val columnIndexAudioId = it.getColumnIndexOrThrow(projection[0])
            val columnIndexAudioTitle = it.getColumnIndexOrThrow(projection[1])
            val columnIndexAudioName = it.getColumnIndexOrThrow(projection[2])
            val columnIndexAudioSize = it.getColumnIndexOrThrow(projection[3])
            val columnIndexAudioPath = it.getColumnIndexOrThrow(projection[4])
            val columnIndexAudioMimeType = it.getColumnIndexOrThrow(projection[5])


            while (it.moveToNext()) {
                var name = it.getString(columnIndexAudioName)
                if (name == null) {
                    name = it.getString(columnIndexAudioTitle)
                }
                if (name == null) {
                    name = ""
                }
                val fileItem = AudioModel(
                    it.getString(columnIndexAudioId),
                    name,
                    it.getInt(columnIndexAudioSize),
                    it.getString(columnIndexAudioPath),
                    //it.getString(columnIndexAudioArtist),
                    it.getString(columnIndexAudioMimeType),
                    false
                )
                fileItems.add(fileItem)

            }
            cursor.close()
        }
        return fileItems
    }


    @Deprecated("worg implemented")
    fun fetchAudioFolders(context: Context): ArrayList<AudioFolder> {
        val folders = ArrayList<AudioFolder>()
        val folderFileCountMap = HashMap<String, Int>()

        val projection = MediaConstants.Projection.ALBUM_FOLDER

        // Create the cursor pointing to the SDCard
        val cursor = CursorHelper.getAudioFolderCursor(context)

        cursor?.let {
            val columnIndexFolderId = it.getColumnIndexOrThrow(projection[0])
            val columnIndexFolderName = it.getColumnIndexOrThrow(projection[1])
            val columnIndexFilePath = it.getColumnIndexOrThrow(projection[2])

            while (it.moveToNext()) {
                val folderId = it.getString(columnIndexFolderId)
                if (folderFileCountMap.containsKey(folderId)) {
                    folderFileCountMap[folderId] = folderFileCountMap[folderId]!! + 1
                } else {
                    val folder = AudioFolder(
                        folderId, it.getString(columnIndexFolderName),
                        it.getString(columnIndexFilePath), 0
                    )
                    folders.add(folder)
                    folderFileCountMap[folderId] = 1
                }
            }
            for (fdr in folders) {
                fdr.contentCount = folderFileCountMap[fdr.id]!!
            }
            cursor.close()
        }
        return folders
    }

    fun getContentUri(context: Context, newFile: File): Uri {
        val SHARED_PROVIDER_AUTHORITY = context.packageName + ".greentoad.turtlebody.mediaprovider"
        try{
            return getUriForFile(context, SHARED_PROVIDER_AUTHORITY, newFile)

        }catch (ex:Exception){
            return Uri.fromFile(newFile)
        }

    }
}


package com.joshtalks.joshskills.core.agora_dynamic

import android.util.Log
import com.tonyodev.fetch2core.server.FileResourceTransporter.Companion.BUFFER_SIZE
import java.io.*
import java.util.zip.ZipFile


/**
 * UnzipUtils class extracts files and sub-directories of a standard zip file to
 * a destination directory.
 *
 */
object UnzipUtils {
    /**
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    @Throws(IOException::class)
    fun unzip(zipFilePath: File, destDirectory: String) : Boolean {
        try {

            File(destDirectory).run {
                if (!exists()) {
                    mkdirs()
                }
            }

            ZipFile(zipFilePath).use { zip ->

                zip.entries().asSequence().forEach { entry ->
                    Log.d("Manjul", "unzip() called with: entry =  $entry")
                    if (entry.name.contains("MACOSX").not()) {

                        zip.getInputStream(entry).use { input ->

                            val filePath = destDirectory + File.separator + entry.name

                            if (!entry.isDirectory) {
                                // if the entry is a file, extracts it
                                extractFile(input, filePath)
                            } else {
                                // if the entry is a directory, make the directory
                                val dir = File(filePath)
                                dir.mkdir()
                            }

                        }
                    }

                }
            }
        }
        catch (ex:Exception){
            return false
        }
        return true
    }

    /**
     * Extracts a zip entry (file entry)
     * @param inputStream
     * @param destFilePath
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun extractFile(inputStream: InputStream, destFilePath: String) {
        val bos = BufferedOutputStream(FileOutputStream(destFilePath))
        val bytesIn = ByteArray(BUFFER_SIZE)
        var read: Int
        while (inputStream.read(bytesIn).also { read = it } != -1) {
            bos.write(bytesIn, 0, read)
        }
        bos.close()
    }

    /**
     * Size of the buffer to read/write data
     */
    private const val BUFFER_SIZE = 4096

}
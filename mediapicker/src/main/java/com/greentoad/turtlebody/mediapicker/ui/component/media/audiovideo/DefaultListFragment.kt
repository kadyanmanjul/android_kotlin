package com.greentoad.turtlebody.mediapicker.ui.component.media.audiovideo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.greentoad.turtlebody.mediapicker.ui.ActivityLibMain
import com.greentoad.turtlebody.mediapicker.core.FileManager
import com.greentoad.turtlebody.mediapicker.ui.common.MediaListFragment
import com.greentoad.turtlebody.mediapicker.ui.component.folder.image_video.ImageVideoFolder
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.tb_media_picker_frame_progress.*
import kotlinx.android.synthetic.main.tb_media_picker_file_fragment.*
import org.jetbrains.anko.info
import java.io.File
import java.util.*

/**
 * Created by niraj on 12-04-2019.
 */
class DefaultListFragment : MediaListFragment(), DefaultAdapter.OnMediaSelectClickListener {


    companion object {

        @JvmStatic
        fun newInstance(b: Bundle?): Fragment {
            val bf: Bundle = b ?: Bundle()
            val fragment = DefaultListFragment()
            fragment.arguments = bf
            return fragment
        }

    }

    private var mDefaultAdapter: DefaultAdapter = DefaultAdapter()
    private var mImageModelList: MutableList<DefaultModel> = arrayListOf()
    private var mSelectedImageModelList: MutableList<DefaultModel> = arrayListOf()


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initAdapter()
    }

    override fun onRestoreState(savedInstanceState: Bundle?, args: Bundle?) {
        arguments?.let {
            mFolderId = it.getString(ImageVideoFolder.FOLDER_ID, "")
            info { "fileId: $mFolderId" }
        }
    }


    override fun getAllUris() {
        if (mSelectedImageModelList.isNotEmpty()) {
            for (i in mSelectedImageModelList) {
                info { "audio path: ${i.filePath}" }
                mUriList.add(FileManager.getContentUri(context!!, File(i.filePath)))
            }
            (activity as ActivityLibMain).sendBackData(mUriList)
        }
    }


    override fun onSelectMedia(pData: DefaultModel) {
        if (!mMediaPickerConfig.mAllowMultiSelection) {
            if (mMediaPickerConfig.mShowConfirmationDialog) {
                val simpleAlert = AlertDialog.Builder(context!!)
                simpleAlert.setMessage("Are you sure to select ${pData.name}")
                    .setCancelable(false)
                    .setPositiveButton("OK") { dialog, which ->
                        (activity as ActivityLibMain).sendBackData(
                            arrayListOf(
                                FileManager.getContentUri(
                                    context!!,
                                    File(pData.filePath)
                                )
                            )
                        )
                    }
                    .setNegativeButton("Cancel") { dialog, which -> dialog.dismiss() }
                simpleAlert.show()
            } else {

                if (pData.fileType.toLowerCase(Locale.getDefault()) == "video/mp4") {
                    (activity as ActivityLibMain).processVideo(
                        FileManager.getContentUri(
                            context!!,
                            File(pData.filePath)),pData.filePath
                    )
                } else {
                    (activity as ActivityLibMain).processImage(pData.filePath)
                }


                // (activity as ActivityLibMain).sendBackData(arrayListOf(FileManager.getContentUri(context!!, File(pData.filePath))))
            }
        } else {
            val selectedIndex = mImageModelList.indexOf(pData)

            if (selectedIndex >= 0) {
                //toggle
                mImageModelList[selectedIndex].isSelected =
                    !(mImageModelList[selectedIndex].isSelected)
                //update ui
                mDefaultAdapter.updateIsSelected(mImageModelList[selectedIndex])

                //update selectedList
                if (mImageModelList[selectedIndex].isSelected) {
                    mSelectedImageModelList.add(mImageModelList[selectedIndex])
                } else {
                    mSelectedImageModelList.removeAt(mSelectedImageModelList.indexOf(pData))
                }
            }
            (activity as ActivityLibMain).updateCounter(mSelectedImageModelList.size)
            file_fragment_btn_done.isEnabled = mSelectedImageModelList.size > 0
        }
    }


    private fun initAdapter() {
        mDefaultAdapter.setListener(this)
        mDefaultAdapter.mShowCheckBox = mMediaPickerConfig.mAllowMultiSelection
        file_fragment_recycler_view.layoutManager = GridLayoutManager(context, 2)
        file_fragment_recycler_view.adapter = mDefaultAdapter
        fetchImageFiles()

    }

    private fun fetchImageFiles() {
        val fileItems = Single.fromCallable<Boolean> {
            mImageModelList.clear()
            val tempArray = FileManager.getImageVideoFilesInFolder(context!!, mFolderId)

            //include only valid files
            for (i in tempArray) {
                info { "size: ${i.size}" }
                if (i.size > 0) {
                    mImageModelList.add(i)
                }
            }
            true
        }

        fileItems.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Boolean> {
                override fun onSubscribe(@NonNull d: Disposable) {
                    frame_progress.visibility = View.VISIBLE
                }

                override fun onSuccess(t: Boolean) {

                    mDefaultAdapter.setData(mImageModelList)
                    if (frame_progress!=null) {
                        frame_progress.visibility = View.GONE
                    }
                }

                override fun onError(@NonNull e: Throwable) {
                    if (frame_progress!=null) {
                        frame_progress.visibility = View.GONE
                    }
                    info { "error: ${e.message}" }
                }
            })
    }




}
package com.greentoad.turtlebody.mediapicker.ui.component.media.image

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.greentoad.turtlebody.mediapicker.core.FileManager
import com.greentoad.turtlebody.mediapicker.ui.ActivityLibMain
import com.greentoad.turtlebody.mediapicker.ui.common.MediaListFragment
import com.greentoad.turtlebody.mediapicker.ui.component.folder.image_video.ImageVideoFolder
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import kotlinx.android.synthetic.main.tb_media_picker_file_fragment.file_fragment_btn_done
import kotlinx.android.synthetic.main.tb_media_picker_file_fragment.file_fragment_recycler_view
import kotlinx.android.synthetic.main.tb_media_picker_frame_progress.frame_progress

class ImageListFragment : MediaListFragment(), ImageAdapter.OnImageClickListener {


    companion object {

        @JvmStatic
        fun newInstance(key: Int, b: Bundle?): Fragment {
            val bf: Bundle = b ?: Bundle()
            bf.putInt("fragment.key", key)
            val fragment = ImageListFragment()
            fragment.arguments = bf
            return fragment
        }

    }

    private var mImageAdapter: ImageAdapter = ImageAdapter()
    private var mImageModelList: MutableList<ImageModel> = arrayListOf()
    private var mSelectedImageModelList: MutableList<ImageModel> = arrayListOf()


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initAdapter()
    }

    override fun onRestoreState(savedInstanceState: Bundle?, args: Bundle?) {
        arguments?.let {
            mFolderId = it.getString(ImageVideoFolder.FOLDER_ID, "")
        }
    }


    override fun getAllUris() {
        if (mSelectedImageModelList.isNotEmpty()) {
            for (i in mSelectedImageModelList) {
                mUriList.add(FileManager.getContentUri(requireContext(), File(i.filePath)))
            }
            (activity as ActivityLibMain).sendBackData(mUriList)
        }
    }


    override fun onImageCheck(pData: ImageModel) {
        if (!mMediaPickerConfig.mAllowMultiSelection) {
            if (mMediaPickerConfig.mShowConfirmationDialog) {
                val simpleAlert = AlertDialog.Builder(requireContext())
                simpleAlert.setMessage("Are you sure to select ${pData.name}")
                    .setCancelable(false)
                    .setPositiveButton("OK") { dialog, which ->
                        (activity as ActivityLibMain).sendBackData(
                            arrayListOf(
                                FileManager.getContentUri(
                                    requireContext(),
                                    File(pData.filePath)
                                )
                            )
                        )
                    }
                    .setNegativeButton("Cancel") { dialog, which -> dialog.dismiss() }
                simpleAlert.show()
            } else {
                (activity as ActivityLibMain).sendBackData(
                    arrayListOf(
                        FileManager.getContentUri(
                            requireContext(),
                            File(pData.filePath)
                        )
                    )
                )
            }
        } else {
            val selectedIndex = mImageModelList.indexOf(pData)

            if (selectedIndex >= 0) {
                //toggle
                mImageModelList[selectedIndex].isSelected =
                    !(mImageModelList[selectedIndex].isSelected)
                //update ui
                mImageAdapter.updateIsSelected(mImageModelList[selectedIndex])

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
        mImageAdapter.setListener(this)
        mImageAdapter.mShowCheckBox = mMediaPickerConfig.mAllowMultiSelection
        file_fragment_recycler_view.layoutManager = GridLayoutManager(context, 2)
        file_fragment_recycler_view.adapter = mImageAdapter
        fetchImageFiles()

    }

    private fun fetchImageFiles() {
        val fileItems = Single.fromCallable<Boolean> {
            mImageModelList.clear()
            val tempArray = FileManager.getImageFilesInFolder(requireContext(), mFolderId)

            //include only valid files
            for (i in tempArray) {
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
                    mImageAdapter.setData(mImageModelList)
                    frame_progress.visibility = View.GONE
                }

                override fun onError(@NonNull e: Throwable) {
                    frame_progress.visibility = View.GONE
                }
            })
    }

}
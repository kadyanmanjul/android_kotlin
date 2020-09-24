package com.greentoad.turtlebody.mediapicker.ui.component.media.video

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

class VideoListFragment : MediaListFragment(), VideoAdapter.OnVideoClickListener {

    companion object {
        @JvmStatic
        fun newInstance(key: Int, b: Bundle?): Fragment {
            val bf: Bundle = b ?: Bundle()
            bf.putInt("fragment.key", key)
            val fragment = VideoListFragment()
            fragment.arguments = bf
            return fragment
        }
    }


    private var mVideoAdapter: VideoAdapter = VideoAdapter()
    private var mVideoModelList: MutableList<VideoModel> = arrayListOf()
    private var mSelectedVideoModelList: MutableList<VideoModel> = arrayListOf()


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
        if (mSelectedVideoModelList.isNotEmpty()) {
            for (i in mSelectedVideoModelList) {
                mUriList.add(FileManager.getContentUri(requireContext(), File(i.filePath)))
            }
            (activity as ActivityLibMain).sendBackData(mUriList)
        }
    }

    override fun onVideoCheck(pData: VideoModel) {
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
            val selectedIndex = mVideoModelList.indexOf(pData)

            if (selectedIndex >= 0) {
                //toggle
                mVideoModelList[selectedIndex].isSelected =
                    !(mVideoModelList[selectedIndex].isSelected)
                //update ui
                mVideoAdapter.updateIsSelected(mVideoModelList[selectedIndex])

                //update selectedList
                if (mVideoModelList[selectedIndex].isSelected) {
                    mSelectedVideoModelList.add(mVideoModelList[selectedIndex])
                } else {
                    mSelectedVideoModelList.removeAt(mSelectedVideoModelList.indexOf(pData))
                }
            }
            (activity as ActivityLibMain).updateCounter(mSelectedVideoModelList.size)
            file_fragment_btn_done.isEnabled = mSelectedVideoModelList.size > 0
        }
    }


    private fun initAdapter() {
        mVideoAdapter.setListener(this)
        mVideoAdapter.mShowCheckBox = mMediaPickerConfig.mAllowMultiSelection

        file_fragment_recycler_view.layoutManager = GridLayoutManager(context, 2)
        file_fragment_recycler_view.adapter = mVideoAdapter
        fetchVideoFiles()

    }

    private fun fetchVideoFiles() {
        val fileItems = Single.fromCallable<Boolean> {
            mVideoModelList.clear()
            val tempArray = FileManager.getVideoFilesInFolder(requireContext(), mFolderId)

            //include only valid files
            for (i in tempArray) {
                if (i.size > 0) {
                    mVideoModelList.add(i)
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
                    mVideoAdapter.setData(mVideoModelList)
                    frame_progress.visibility = View.GONE
                }

                override fun onError(@NonNull e: Throwable) {
                    frame_progress.visibility = View.GONE
                }
            })
    }
}
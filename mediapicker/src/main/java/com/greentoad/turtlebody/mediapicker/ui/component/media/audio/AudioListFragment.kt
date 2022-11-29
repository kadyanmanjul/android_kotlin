package com.greentoad.turtlebody.mediapicker.ui.component.media.audio

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.greentoad.turtlebody.mediapicker.R
import com.greentoad.turtlebody.mediapicker.core.FileManager
import com.greentoad.turtlebody.mediapicker.ui.ActivityLibMain
import com.greentoad.turtlebody.mediapicker.ui.common.MediaListFragment
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File

class AudioListFragment : MediaListFragment(), AudioAdapter.OnAudioClickListener {


    companion object {

        @JvmStatic
        fun newInstance(key: Int, b: Bundle?): Fragment {
            val bf: Bundle = b ?: Bundle()
            bf.putInt("fragment.key", key)
            val fragment = AudioListFragment()
            fragment.arguments = bf
            return fragment
        }

        const val B_ARG_FOLDER_PATH = "args.folder.path"
    }

    private var mFolderPath: String = ""

    private var mAudioAdapter: AudioAdapter = AudioAdapter()
    private var mAudioModelList: MutableList<AudioModel> = arrayListOf()
    private var mSelectedAudioModelList: MutableList<AudioModel> = arrayListOf()
    private val frameProgress by lazy {
        view?.findViewById<FrameLayout>(R.id.frame_progress)
    }
    private val recyclerView by lazy {
        view?.findViewById<RecyclerView>(R.id.file_fragment_recycler_view)
    }
    private val doneBtn by lazy {
        view?.findViewById<Button>(R.id.file_fragment_btn_done)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initAdapter()
    }

    override fun onRestoreState(savedInstanceState: Bundle?, args: Bundle?) {
        arguments?.let {
            mFolderPath = it.getString(B_ARG_FOLDER_PATH, "")
        }
    }


    override fun getAllUris() {
        if (mSelectedAudioModelList.isNotEmpty()) {
            for (i in mSelectedAudioModelList) {
                mUriList.add(FileManager.getContentUri(requireContext(), File(i.filePath)))
            }
            (activity as ActivityLibMain).sendBackData(mUriList)
        }
    }


    override fun onAudioCheck(pData: AudioModel) {
        try {
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
                val selectedIndex = mAudioModelList.indexOf(pData)

                if (selectedIndex >= 0) {
                    //toggle
                    mAudioModelList[selectedIndex].isSelected =
                        !(mAudioModelList[selectedIndex].isSelected)
                    //update ui
                    mAudioAdapter.updateIsSelected(mAudioModelList[selectedIndex])

                    //update selectedList (add/remove audio)
                    if (mAudioModelList[selectedIndex].isSelected) {
                        mSelectedAudioModelList.add(mAudioModelList[selectedIndex])
                    } else {
                        mSelectedAudioModelList.removeAt(mSelectedAudioModelList.indexOf(pData))
                    }
                }
                (activity as ActivityLibMain).updateCounter(mSelectedAudioModelList.size)
                doneBtn?.isEnabled = mSelectedAudioModelList.size > 0
            }
        } catch (ex: Exception) {
            Toast.makeText(context, "Some error occur to select file", Toast.LENGTH_SHORT).show()
            activity?.finish()
        }
    }


    private fun initAdapter() {
        mAudioAdapter.setListener(this)
        mAudioAdapter.mShowCheckBox = mMediaPickerConfig.mAllowMultiSelection

        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = mAudioAdapter
        fetchAudioFiles()

    }

    private fun fetchAudioFiles() {
        val fileItems = Single.fromCallable<Boolean> {
            mAudioModelList.clear()
            val tempArray = FileManager.getAudioFilesInFolder(requireContext(), mFolderPath)

            //include only valid files
            for (i in tempArray) {
                if (i.size > 0) {
                    mAudioModelList.add(i)
                }
            }
            true
        }

        fileItems.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Boolean> {
                override fun onSubscribe(@NonNull d: Disposable) {
                    frameProgress?.visibility = View.VISIBLE
                }

                override fun onSuccess(t: Boolean) {
                    mAudioAdapter.setData(mAudioModelList)
                    frameProgress?.visibility = View.GONE
                }

                override fun onError(@NonNull e: Throwable) {
                    frameProgress?.visibility = View.GONE
                }
            })
    }

}
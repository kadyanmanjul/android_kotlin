package com.greentoad.turtlebody.mediapicker.ui.component.folder.image_video


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.greentoad.turtlebody.mediapicker.MediaPicker
import com.greentoad.turtlebody.mediapicker.R
import com.greentoad.turtlebody.mediapicker.core.FileManager
import com.greentoad.turtlebody.mediapicker.ui.ActivityLibMain
import com.greentoad.turtlebody.mediapicker.ui.base.FragmentBase
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


class ImageVideoFolderFragment : FragmentBase() {

    companion object {

        @JvmStatic
        fun newInstance(key: Int, b: Bundle?): Fragment {
            val bf: Bundle = b ?: Bundle()
            bf.putInt("fragment.key", key)
            val fragment = ImageVideoFolderFragment()
            fragment.arguments = bf
            return fragment
        }

    }

    private var mImageVideoFolderAdapter: ImageVideoFolderAdapter = ImageVideoFolderAdapter()
    private var mImageVideoFolderList: MutableList<ImageVideoFolder> = arrayListOf()
    private var mFileType = MediaPicker.MediaTypes.IMAGE
    private val frameProgress by lazy {
        view?.findViewById<FrameLayout>(R.id.frame_progress)
    }
    private val recyclerView by lazy {
        view?.findViewById<RecyclerView>(R.id.folder_fragment_recycler_view)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.tb_media_picker_folder_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        arguments?.let {
            mFileType = it.getInt(ActivityLibMain.B_ARG_FILE_TYPE)
        }
        initAdapter()
    }

    private fun initAdapter() {
        mImageVideoFolderAdapter.setListener(object :
            ImageVideoFolderAdapter.OnFolderClickListener {
            override fun onFolderClick(pData: ImageVideoFolder) {
                (activity as ActivityLibMain).startMediaListFragment(pData.id, 0)
            }
        })
        recyclerView?.layoutManager = GridLayoutManager(context, 2)
        // folder_fragment_recycler_view.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = mImageVideoFolderAdapter
        fetchImageVideoFolders()


    }

    private fun fetchImageVideoFolders() {
        val bucket: Single<ArrayList<ImageVideoFolder>> =
            Single.fromCallable<ArrayList<ImageVideoFolder>> {
                FileManager.fetchVideoFolders(
                    requireContext()
                )
            }
        bucket.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<ImageVideoFolder>> {

                override fun onSubscribe(@NonNull d: Disposable) {
                    frameProgress?.visibility = View.VISIBLE
                }

                override fun onSuccess(@NonNull imageVideoFolders: ArrayList<ImageVideoFolder>) {
                    frameProgress?.visibility = View.VISIBLE
                    mImageVideoFolderList = imageVideoFolders
                    mImageVideoFolderAdapter.setDataMultiple(mImageVideoFolderList)
                    frameProgress?.visibility = View.GONE
                }

                override fun onError(@NonNull e: Throwable) {
                    frameProgress?.visibility = View.GONE
                    e.printStackTrace()
                }
            })
    }

}

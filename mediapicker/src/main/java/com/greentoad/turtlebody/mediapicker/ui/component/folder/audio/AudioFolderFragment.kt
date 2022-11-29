package com.greentoad.turtlebody.mediapicker.ui.component.folder.audio


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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


class AudioFolderFragment : FragmentBase() {

    companion object {

        @JvmStatic
        fun newInstance(key: Int, b: Bundle?): Fragment {
            val bf: Bundle = b ?: Bundle()
            bf.putInt("fragment.key", key)
            val fragment = AudioFolderFragment()
            fragment.arguments = bf
            return fragment
        }

    }

    private var mAudioFolderAdapter: AudioFolderAdapter = AudioFolderAdapter()
    private var mAudioFolderList: MutableList<AudioFolder> = arrayListOf()
    private val frameProgress by lazy {
        view?.findViewById<FrameLayout>(R.id.frame_progress)
    }
    private val folderFragmentRV by lazy {
        view?.findViewById<RecyclerView>(R.id.folder_fragment_recycler_view)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.tb_media_picker_folder_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initAdapter()
    }

    private fun initAdapter() {
        mAudioFolderAdapter.setListener(object : AudioFolderAdapter.OnAudioFolderClickListener {
            override fun onFolderClick(pData: AudioFolder) {
                (activity as ActivityLibMain).startMediaListFragment(pData.path, MediaPicker.MediaTypes.AUDIO)
            }
        })

        folderFragmentRV?.layoutManager = LinearLayoutManager(context)
        folderFragmentRV?.adapter = mAudioFolderAdapter
        fetchAudioFolders()
    }


    private fun fetchAudioFolders() {
        val bucketFetch = Single.fromCallable {
            FileManager.fetchAudioFolderList(requireContext())
        }
        bucketFetch
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<AudioFolder>> {
                override fun onSubscribe(@NonNull d: Disposable) {
                    frameProgress?.visibility = View.VISIBLE
                }

                override fun onSuccess(@NonNull audioFolders: ArrayList<AudioFolder>) {
                    mAudioFolderList = audioFolders
                    mAudioFolderAdapter.setData(mAudioFolderList)
                    frameProgress?.visibility = View.GONE
                }

                override fun onError(@NonNull e: Throwable) {
                    frameProgress?.visibility = View.GONE
                    e.printStackTrace()
                }
            })
    }
}

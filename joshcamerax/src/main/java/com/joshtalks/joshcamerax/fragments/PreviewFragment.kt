package com.joshtalks.joshcamerax.fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import com.joshtalks.joshcamerax.R
import com.joshtalks.joshcamerax.adapter.PicturesAdapter
import com.joshtalks.joshcamerax.databinding.FragmentPreviewBinding
import com.joshtalks.joshcamerax.utils.*
import org.apache.commons.io.comparator.LastModifiedFileComparator
import java.util.*

class PreviewFragment : BaseFragment<FragmentPreviewBinding>(R.layout.fragment_preview) {
    private lateinit var picturesAdapter: PicturesAdapter
    private var currentPage = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fragment = this // setting the variable for XML
        adjustInsets()

        // Check for the permissions and show files
        if (allPermissionsGranted()) {
            outputDirectory.listFiles()?.let {
                Arrays.sort(it,LastModifiedFileComparator.LASTMODIFIED_REVERSE)
                picturesAdapter = PicturesAdapter(it.toMutableList()) { isVideo, _ ->
                    if (!isVideo) {
                        binding.groupPreviewActions.visibility =
                            if (binding.groupPreviewActions.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                }
                binding.pagerPhotos.apply {
                    adapter = picturesAdapter
                    onPageSelected { page -> currentPage = page }
                }
            }
        }
    }

    /**
     * This methods adds all necessary margins to some views based on window insets and screen orientation
     * */
    private fun adjustInsets() {

        binding.layoutRoot.fitSystemWindows()
        binding.imageBack.onWindowInsets { _, _ ->
            //view.topMargin = windowInsets.systemWindowInsetTop
        }
        binding.imageShare.onWindowInsets { view, windowInsets ->
            view.bottomMargin = windowInsets.systemWindowInsetBottom
        }
    }

    fun sendImage() {
        if (!::picturesAdapter.isInitialized) return

        picturesAdapter.sendImage(currentPage) { send(it) }
    }

    fun deleteImage() {
        if (!::picturesAdapter.isInitialized) return

        picturesAdapter.deleteImage(currentPage) {
            if (outputDirectory.listFiles().isNullOrEmpty()) onBackPressed()
        }
    }

    override fun onBackPressed() {
        view?.let { Navigation.findNavController(it).popBackStack() }
    }



}
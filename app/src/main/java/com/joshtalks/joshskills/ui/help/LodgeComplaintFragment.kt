package com.joshtalks.joshskills.ui.help


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentLodgeComplaintBinding


class LodgeComplaintFragment : Fragment() {

    private lateinit var lodgeComplaintBinding: FragmentLodgeComplaintBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        lodgeComplaintBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_lodge_complaint, container, false)
        lodgeComplaintBinding.lifecycleOwner = this
        lodgeComplaintBinding.handler = this
        return lodgeComplaintBinding.root
    }

    fun lodgeComplaint() {

    }

    fun attachMedia() {
        val pickPhoto = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        // Sets the type as image/*. This ensures only components of type image are selected
        // Sets the type as image/*. This ensures only components of type image are selected
        pickPhoto.type = "image/*"
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        val mimeTypes =
            arrayOf("image/jpeg", "image/png")
        pickPhoto.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(pickPhoto, 1)
    }

    @SuppressLint("Recycle")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 1) {
            val selectedImage: Uri = data?.data!!
            val filePathColumn =
                arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor =activity?.contentResolver?.query(selectedImage, filePathColumn, null, null, null)!!
            cursor.moveToFirst()
            val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
            val imgDecodableString: String = cursor.getString(columnIndex)
            cursor.close()

        }

    }

    companion object {
        @JvmStatic
        fun newInstance() = LodgeComplaintFragment().apply {}
    }
}

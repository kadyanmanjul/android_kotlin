package com.joshtalks.badebhaiya.liveroom.bottomsheet

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.models.FormRequest
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception

class EnterBioBottomSheet : BottomSheetDialogFragment() {

    private var roomUserClickAction: ConversationRoomBottomSheetAction? = null
    private var submitBtn:TextView?=null
    private var bioText:EditText?=null
    private var userId:String?=null
    private var size:TextView?=null


    companion object {
        fun newInstance(
            user: String
        ): EnterBioBottomSheet {
            return EnterBioBottomSheet().apply {
                userId = user
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getActivity()?.getWindow()?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setStyle(STYLE_NORMAL, R.style.BaseBottomSheetDialog)

    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.li_bottom_sheet_update_bio, null)
        dialog.setContentView(contentView)

        dialog.setOnShowListener {

            val bottomSheetDialog = it as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { it ->
                val behaviour = BottomSheetBehavior.from(it)
                setupFullHeight(it)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        submitBtn=contentView.findViewById(R.id.submit)
        bioText=contentView.findViewById(R.id.bio_text)
        size=contentView.findViewById(R.id.size)

        bioText?.addTextChangedListener {
            submitBtn?.isEnabled = !it.toString().trim().isEmpty()
            size?.text= "${bioText?.text?.length.toString()}/70"
            if(bioText?.text?.length!! >70)
                size?.setTextColor(context?.let { it1 -> AppCompatResources.getColorStateList(it1,R.color.update_bio_text) })
            else
                size?.setTextColor(context?.let { it1 -> AppCompatResources.getColorStateList(it1,R.color.default_bio_text) })


        }
        submitBtn?.setOnClickListener{
            showToast("Updated")
            val msg:String
            if(bioText.toString().isNotBlank()) {
                msg = bioText?.text.toString()
                val obj= FormRequest(User.getInstance().userId,msg,userId!!)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val resp= CommonRepository().sendRequest(obj)
                        if(resp.isSuccessful)
                            showToast("Bio Updated")
                    } catch (e: Exception){
                        Log.i("REQUESTMSG", "requestRoomPopup: ${e.message}")
                    }
                }
                dialog.dismiss()
            }
        }


    }

    override fun onDismiss(dialog: DialogInterface) {
        roomUserClickAction?.onDismiss()
        super.onDismiss(dialog)
    }

}

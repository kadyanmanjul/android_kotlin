package com.joshtalks.joshskills.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.showToast
import com.joshtalks.joshskills.databinding.FragmentDebugHomeBinding
import com.joshtalks.joshskills.ui.BottomAlertDialog
import com.joshtalks.joshskills.ui.DebugActivity
import java.io.File
import java.util.*

class DebugHomeFragment : Fragment() {
    private lateinit var binding: FragmentDebugHomeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_debug_home, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.handler = this
    }

    fun viewDeviceInfo(v: View) {
        findNavController().navigate(DebugHomeFragmentDirections.actionDebugHomeFragmentToDeviceInfoFragment())
    }

    fun viewDatabase(v: View) {
        findNavController().navigate(DebugHomeFragmentDirections.actionDebugHomeFragmentToDatabaseFragment())
    }

    fun viewSharedPreferences(v: View) {
        findNavController().navigate(DebugHomeFragmentDirections.actionDebugHomeFragmentToSharedPreferencesFragment())
    }

    fun viewApiRequests(v: View) {
        findNavController().navigate(DebugHomeFragmentDirections.actionDebugHomeFragmentToApiRequestFragment())
    }

    fun clearData(v: View) {
        (requireActivity() as DebugActivity).clearData()
    }

    fun deleteGaid(v: View) {
        (requireActivity() as DebugActivity).deleteGaid()
    }

    fun deleteUser(v: View) {
        (requireActivity() as DebugActivity).deleteUser()
    }

    fun loginMentor(v: View) {
        val dialog = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text, null)
        dialog.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val textInputLayout = dialog.findViewById<TextInputLayout>(R.id.layout_base_url)
        val textInputEditText = dialog.findViewById<TextInputEditText>(R.id.et_base_url)
        textInputLayout.setEndIconDrawable(R.drawable.ic_round_content_paste_24)
        textInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
        textInputLayout.hint = "Mentor Id"
        textInputLayout.setEndIconOnClickListener {
            val clipboard =
                requireActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                textInputEditText.setText(clip.getItemAt(0).text)
            }
        }
        textInputLayout.refreshEndIconDrawableState()
        BottomAlertDialog()
            .setTitle("Enter Mentor ID")
            .setCustomView(dialog)
            .setPositiveButton("Login") { d ->
                val mentorId = textInputEditText.text.toString()
                if (mentorId.isNotEmpty()) {
                    // TODO@yashkasera: 07/12/22 Login mentor
                } else {
                    showToast("Please enter mentor id")
                }
            }
            .setNegativeButton("Cancel") { d ->
                d.dismiss()
            }
            .show(childFragmentManager)
    }

    fun openActivity(v: View) {
        findNavController().navigate(DebugHomeFragmentDirections.actionDebugHomeFragmentToRedirectFragment())
    }

    fun viewLogs(v: View) {
        try {
            AppObjectController.joshApplication.applicationContext.getExternalFilesDir(null)
                ?.let { publicAppDirectory ->
                    val logDirectory = File("${publicAppDirectory.absolutePath}/logs")
                    val files = logDirectory.listFiles()
                    if (files != null) {
                        Arrays.sort(files) { file1, file2 -> file2.lastModified().compareTo(file1.lastModified()) }
                        files.firstOrNull()?.let {
                            val externalFile = File(
                                requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                                it.name
                            )
                            externalFile.createNewFile()
                            it.copyTo(externalFile, true)
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(Uri.fromFile(externalFile), "text/plain")
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                    } else
                        throw Exception("No files found")
                } ?: throw Exception("No files found")
        } catch (e: Exception) {
            showToast("An error occurred while fetching logs: ${e.message}")
        }
    }

    fun abTest(v: View) {
        findNavController().navigate(DebugHomeFragmentDirections.actionDebugHomeFragmentToABTestFragment())
    }
}
package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.databinding.FragmentSelectCourseHeadingBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.CourseHeadingSelectedEvent
import com.joshtalks.joshskills.repository.server.onboarding.CourseHeading
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.ui.newonboarding.viewholder.CourseHeadingViewHolder
import com.joshtalks.joshskills.ui.newonboarding.viewmodel.OnBoardViewModel
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SelectCourseHeadingFragment : Fragment() {

    lateinit var binding: FragmentSelectCourseHeadingBinding
    lateinit var viewmodel: OnBoardViewModel
    var headingIds = ArrayList<Int>()
    var contentData: List<CourseHeading>? = null
    var compositeDisposable = CompositeDisposable()


    companion object {
        const val TAG = "SelectCourseHeadingFragment"
        fun newInstance(
        ): SelectCourseHeadingFragment {
            val args = Bundle()
            val fragment = SelectCourseHeadingFragment()
            fragment.arguments = args
            return fragment
        }
    }

    fun openHelpActivity() {
        (requireActivity() as BaseActivity).openHelpActivity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewmodel = ViewModelProvider(requireActivity()).get(OnBoardViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_select_course_heading,
                container,
                false
            )
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        disableBtn()
        initRV()
        initView()
    }

    private fun initView() {
        if (VersionResponse.getInstance().hasVersion()) {
            val versionData = VersionResponse.getInstance()
            binding.desc.text = versionData.v5Description
            binding.title.text = versionData.v5Title
            contentData = versionData.course_headings.apply { this?.sortedBy { it.sortOrder } }
            contentData?.let {
                it.forEach { data ->
                    binding.recyclerView.addView(CourseHeadingViewHolder(data))
                }
            }
        }
        binding.getDetailBtn.setOnClickListener {
            if (headingIds.size > 0) {
                (requireActivity() as BaseActivity).replaceFragment(
                    R.id.onboarding_container,
                    CourseEnrolledDetailFragment.newInstance(headingIds),
                    CourseEnrolledDetailFragment.TAG
                )
            }
        }
    }

    private fun initRV() {
        val linearLayoutManager = SmoothLinearLayoutManager(requireContext())
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.recyclerView.builder
            .setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    requireContext(),
                    4f
                )
            )
        )
    }

    override fun onResume() {
        super.onResume()
        subscribeRxBus()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    private fun subscribeRxBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(CourseHeadingSelectedEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.isSelected) {
                        headingIds.remove(it.id)
                    } else {
                        headingIds.add(it.id)
                    }
                    if (headingIds.size > 0) {
                        enableBtn()
                    } else {
                        disableBtn()
                    }
                })
    }

    private fun enableBtn() {
        binding.getDetailBtn.isEnabled = true
        binding.getDetailBtn.isClickable = true
        binding.getDetailBtn.backgroundTintList = ContextCompat.getColorStateList(
            AppObjectController.joshApplication,
            R.color.button_color
        )
    }

    private fun disableBtn() {
        binding.getDetailBtn.isEnabled = false
        binding.getDetailBtn.isClickable = false
        binding.getDetailBtn.backgroundTintList = ContextCompat.getColorStateList(
            AppObjectController.joshApplication,
            R.color.light_grey
        )
    }
}

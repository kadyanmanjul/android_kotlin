package com.joshtalks.joshskills.common.ui.course_progress_new

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.custom_ui.decorator.SmoothScrollingLinearLayoutManager
import com.joshtalks.joshskills.common.core.custom_ui.decorator.StickHeaderItemDecoration
import com.joshtalks.joshskills.common.core.extension.deepEquals
import com.joshtalks.joshskills.common.databinding.CourseProgressActivityNewBinding
import com.joshtalks.joshskills.common.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.common.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.common.repository.local.model.User
import com.joshtalks.joshskills.common.repository.server.course_overview.CourseOverviewItem
import com.joshtalks.joshskills.common.repository.server.course_overview.CourseOverviewResponse
import com.joshtalks.joshskills.common.track.CONVERSATION_ID
import com.joshtalks.joshskills.common.ui.assessment.view.Stub
import com.joshtalks.joshskills.common.ui.chat.CHAT_ROOM_ID
import com.joshtalks.joshskills.common.ui.chat.vh.PdfCourseProgressView
import com.joshtalks.joshskills.common.util.CustomDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val COURSE_ID = "course_id"

class CourseProgressActivityNew : CourseProgressAdapter.ProgressItemClickListener,ThemedBaseActivity() {
    private var pdfViewStub: Stub<PdfCourseProgressView>? = null
    private var courseOverviewResponse: List<CourseOverviewResponse>? = null

    private var lastAvailableLessonNo: Int? = null
    lateinit var binding: CourseProgressActivityNewBinding
    private val adapter: ProgressActivityAdapter by lazy {
        ProgressActivityAdapter(
            this,
            this,
            intent.getStringExtra(CONVERSATION_ID) ?: "0",
            lastAvailableLessonNo
        )
    }

    private lateinit var navigator: Navigator

    var courseId: Int = -1

    val activityListener: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { intent ->
                    if (intent.hasExtra(CHAT_ROOM_ID) && intent.getStringExtra(CHAT_ROOM_ID)
                            .isNullOrBlank().not()
                    ) {
                        //binding.progressLayout.visibility=View.VISIBLE
                        viewModel.getCourseOverview(courseId)
                    }
                }
            }
        }

    private val viewModel: CourseOverviewViewModel by lazy {
        ViewModelProvider(this).get(CourseOverviewViewModel::class.java)
    }

    companion object {
        fun getCourseProgressActivityNew(
            context: Context,
            conversationId: String,
            courseId: Int
        ) = Intent(context, CourseProgressActivityNew::class.java).apply {
            putExtra(CONVERSATION_ID, conversationId)
            putExtra(COURSE_ID, courseId)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.course_progress_activity_new)
        binding.handler = this
        if (intent.hasExtra(COURSE_ID).not())
            finish()

//        navigator = intent.getSerializableExtra(NAVIGATOR) as Navigator
        navigator = AppObjectController.navigator
        courseId = intent.getIntExtra(COURSE_ID, 0)
        setupToolbar()
        setWhiteStatusBar()
        initRV()
        addObservers()
        setupUi()
        getData()

    }

    private fun setWhiteStatusBar(){
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.white)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        AppObjectController.screenHeight = displayMetrics.heightPixels
        AppObjectController.screenWidth = displayMetrics.widthPixels
    }

    fun addObservers() {
        viewModel.progressLiveData.observe(
            this
        ) { response ->

            if (response.isCourseBought.not() &&
                response.expiryDate != null &&
                response.expiryDate.time < System.currentTimeMillis()
            ) {
                binding.freeTrialExpiryLayout.visibility = View.VISIBLE
            } else {
                binding.freeTrialExpiryLayout.visibility = View.GONE
            }

            binding.progressLayout.visibility = View.GONE
            val isAnyDifference = courseOverviewResponse?.deepEquals(response.responseData!!)
            if (isAnyDifference == false) {

                courseOverviewResponse?.forEachIndexed { index, courseOverviewResponse ->
                    if ((courseOverviewResponse == response.responseData?.get(index)).not()
                    ) {

                        val viewHolder =
                            binding.progressRv.findViewHolderForAdapterPosition(
                                (index * 2).plus(
                                    1
                                )
                            )
                        if (viewHolder is ProgressActivityAdapter.ProgressViewHolder) {
                            viewHolder.adapter.updateDataList(response.responseData?.get(index)?.data)
                            viewHolder.updateItem(courseOverviewResponse, (index * 2).plus(1))
                        }
                    }
                    return@forEachIndexed
                }
                /*val diffResponse = courseOverviewResponse?.minus(response.responseData!!)
                courseOverviewResponse.co
                val responseDifference = response.responseData?.filter {
                    it.title.equals(diffResponse?.get(0)?.title) && it.data.isNullOrEmpty()
                        .not()
                }
                responseDifference?.get(0)
                    ?.let { adapter.updateItem(it, diffResponse?.get(0)?.title) }
                responseDifference?.get(0)
                    ?.let { binding.progressRv.findViewHolderForAdapterPosition() }*/

            } else {
                /*courseOverviewResponse?.forEachIndexed { index, courseOverviewResponse ->
                courseOverviewResponse.conta(response.responseData)
            }*/
                courseOverviewResponse = response.responseData
                pdfViewStub?.let { view ->
                    view.resolved().let {
                        view.get()?.visibility = View.VISIBLE
                        view.get()?.setup(
                            response.pdfInfo,
                            courseId.toString(),
                            getConversationId() ?: EMPTY
                        )
                        view.get().addCallback(object : PdfCourseProgressView.Callback {
                            override fun showDialog(idString: Int) {
                                when (idString) {
                                    -1 -> {
                                        PermissionUtils.permissionPermanentlyDeniedDialog(
                                            this@CourseProgressActivityNew
                                        )
                                    }
                                    else -> {
                                        PermissionUtils.permissionPermanentlyDeniedDialog(
                                            this@CourseProgressActivityNew,
                                            idString
                                        )
                                    }
                                }
                            }
                        })
                    }
                }

                val data = ArrayList<CourseOverviewResponse>()
                response.responseData?.forEach { courseOverview ->
                    val courseOverviewResponse = CourseOverviewResponse()
                    courseOverviewResponse.title = courseOverview.title
                    courseOverviewResponse.unLockCount = courseOverview.unLockCount
                    courseOverviewResponse.type = 10
                    data.add(courseOverviewResponse)
                    data.add(courseOverview)
                }
                adapter.addItems(data)
            }
        }
    }

    fun getData() {
        CoroutineScope(Dispatchers.IO).launch {
            lastAvailableLessonNo = viewModel.getLastLessonForCourse(courseId)
        }

        viewModel.getCourseOverview(courseId)
    }

    private fun initRV() {
        val linearLayoutManager = SmoothScrollingLinearLayoutManager(this, false)
        linearLayoutManager.stackFromEnd = false
        linearLayoutManager.isItemPrefetchEnabled = true
        linearLayoutManager.initialPrefetchItemCount = 4
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.progressRv.layoutManager = linearLayoutManager

        //adapter.setHasStableIds(true)
        binding.progressRv.adapter = adapter
        binding.progressRv.setHasFixedSize(true)
        binding.progressRv.setItemViewCacheSize(4)
        val stickHeaderDecoration = StickHeaderItemDecoration(adapter.getListner())
        binding.progressRv.addItemDecoration(stickHeaderDecoration)
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun setupUi() {
        pdfViewStub = Stub(findViewById(R.id.pdf_view_stub))
    }

    private fun setupToolbar() {
        findViewById<ImageView>(R.id.back_iv).setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
            onBackPressed()
        }
    }

    override fun onProgressItemClick(item: CourseOverviewItem, previousItem: CourseOverviewItem?) {
        CoroutineScope(Dispatchers.IO).launch {
            val lessonModel = viewModel.getLesson(item.lessonId)
            withContext(Dispatchers.Main) {
                if (viewModel.progressLiveData.value?.isCourseBought == false &&
                    viewModel.progressLiveData.value?.expiryDate != null &&
                    viewModel.progressLiveData.value?.expiryDate!!.time < System.currentTimeMillis()
                ) {
                    val nameArr = User.getInstance().firstName?.split(" ")
                    val firstName = if (nameArr != null) nameArr[0] else EMPTY
                    showToast(getFeatureLockedText(courseId.toString(), firstName))
                } else if (lessonModel != null) {
                    activityListener.launch(
                        navigator.with(this@CourseProgressActivityNew).getIntentForActivity(
                            object : LessonContract {
                                override val lessonId = item.lessonId
                                override val conversationId = intent.getStringExtra(CONVERSATION_ID)
                                override val isLessonCompleted = lessonModel.status == LESSON_STATUS.CO
                                override val navigator = this@CourseProgressActivityNew.navigator
                            }
                        )
                    )
                } else {
                    if (!isFinishing) {
                        try {
                            showAlertMessage(
                                AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.INCOMPLETE_LESSONS_TITLE),
                                AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.PROGRESS_MESSAGE)
                            )
                        }catch (ex:Exception){}
                    }
                }
            }
        }
    }

    override fun onCertificateExamClick(
        previousLesson: CourseOverviewItem,
        conversationId: String,
        chatMessageId: String,
        certificationId: Int,
        cExamStatus: CExamStatus,
        parentPosition: Int,
        title: String

    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val lessonModel = viewModel.getLesson(previousLesson.lessonId)
            runOnUiThread {
                if (lessonModel == null) {
                    courseOverviewResponse?.let {
                        ExamUnlockDialogFragment(
                            it[parentPosition].examInstructions,
                            it[parentPosition].ceMarks,
                            it[parentPosition].ceQue,
                            it[parentPosition].ceMin,
                            it[parentPosition].totalCount,
                            it[parentPosition].unLockCount,
                            title
                        ).show(
                            supportFragmentManager,
                            "ExamUnlockDialogFragment"
                        )
                    }
                } else {
                    navigator.with(this@CourseProgressActivityNew).navigate(object : CertificateContract{
                        override val conversationId = conversationId
                        override val chatMessageId = chatMessageId
                        override val certificationId = certificationId
                        override val cExamStatus =  cExamStatus
                        override val navigator = this@CourseProgressActivityNew.navigator
                    })
                }
            }
        }
    }

    private fun showAlertMessage(title: String, message: String) {
        CustomDialog(this, title, message).show()
    }

    fun showFreeTrialPaymentScreen() {
        navigator.with(this).navigate(object : BuyPageContract {
            override val flowFrom = "COURSE_OVERVIEW"
            override val navigator = this@CourseProgressActivityNew.navigator
        })
        // finish()
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        val resultIntent = Intent()
        resultIntent.putExtra(COURSE_ID, courseId)
        setResult(RESULT_OK, resultIntent)
        this.finish()
    }
}

package com.joshtalks.joshskills.ui.course_progress_new

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.decorator.SmoothScrollingLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.decorator.StickHeaderItemDecoration
import com.joshtalks.joshskills.databinding.CourseProgressActivityNewBinding
import com.joshtalks.joshskills.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewItem
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewResponse
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.certification_exam.CertificationBaseActivity
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_ID
import com.joshtalks.joshskills.ui.chat.vh.PdfCourseProgressView
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.util.CustomDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val COURSE_ID = "course_id"

class CourseProgressActivityNew :
    WebRtcMiddlewareActivity(),
    CourseProgressAdapter.ProgressItemClickListener {
    private var  pdfViewStub: Stub<PdfCourseProgressView>? = null
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
                        binding.progressLayout.visibility=View.VISIBLE
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

        if (intent.hasExtra(COURSE_ID).not())
            finish()

        courseId = intent.getIntExtra(COURSE_ID, 0)
        setupToolbar()
        initRV()
        addObservers()
        setupUi()
        getData()
    }

    fun addObservers() {
        viewModel.progressLiveData.observe(
            this,
            { response->
                binding.progressLayout.visibility = View.GONE
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
                response.responseData?.forEach { courseOverview->
                    val courseOverviewResponse = CourseOverviewResponse()
                    courseOverviewResponse.title = courseOverview.title
                    courseOverviewResponse.unLockCount = courseOverview.unLockCount
                    courseOverviewResponse.type = 10
                    data.add(courseOverviewResponse)
                    data.add(courseOverview)
                }
                adapter.addItems(data)
            }
        )
    }

    fun getData() {
        CoroutineScope(Dispatchers.IO).launch {
            lastAvailableLessonNo = viewModel.getLastLessonForCourse(courseId)
        }

        viewModel.getCourseOverview(courseId)
    }

    private fun initRV() {
        val linearLayoutManager = SmoothScrollingLinearLayoutManager(this, true)
        linearLayoutManager.stackFromEnd = true
        linearLayoutManager.isItemPrefetchEnabled = true
        linearLayoutManager.initialPrefetchItemCount = 6
        linearLayoutManager.isSmoothScrollbarEnabled = true
        //binding.progressRv.layoutManager = linearLayoutManager

        adapter.setHasStableIds(true)
        binding.progressRv.adapter = adapter
        binding.progressRv.setHasFixedSize(true)
        binding.progressRv.setItemViewCacheSize(6)
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
            onBackPressed()
        }
    }

    override fun onProgressItemClick(item: CourseOverviewItem, previousItem: CourseOverviewItem?) {
        CoroutineScope(Dispatchers.IO).launch {
            val lessonModel = viewModel.getLesson(item.lessonId)
            runOnUiThread {
                if (lessonModel != null) {
                    activityListener.launch(
                        LessonActivity.getActivityIntent(
                            this@CourseProgressActivityNew,
                            item.lessonId,
                            conversationId = intent.getStringExtra(CONVERSATION_ID)
                        )
                    )
                } else {
                    showAlertMessage(
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.INCOMPLETE_LESSONS_TITLE),
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.PROGRESS_MESSAGE)
                    )
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
                    activityListener.launch(
                        CertificationBaseActivity.certificationExamIntent(
                            this@CourseProgressActivityNew,
                            conversationId = conversationId,
                            chatMessageId = chatMessageId,
                            certificationId = certificationId,
                            cExamStatus = cExamStatus
                        )
                    )
                }
            }
        }
    }

    private fun showAlertMessage(title: String, message: String) {

        CustomDialog(
            this,
            title,
            message
        ).show()
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra(COURSE_ID, courseId)
        setResult(RESULT_OK, resultIntent)
        this.finish()
    }
}

package com.joshtalks.joshskills.ui.chat.adapter


//import com.joshtalks.joshskills.ui.groupchat.utils.Utils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils.dateHeaderDateFormat
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.Sender
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.chat.vh.AssessmentViewHolder
import com.joshtalks.joshskills.ui.chat.vh.AudioViewHolder
import com.joshtalks.joshskills.ui.chat.vh.BaseViewHolder
import com.joshtalks.joshskills.ui.chat.vh.BestStudentPerformerViewHolder
import com.joshtalks.joshskills.ui.chat.vh.CertificationExamViewHolder
import com.joshtalks.joshskills.ui.chat.vh.DateItemHolder
import com.joshtalks.joshskills.ui.chat.vh.ImageViewHolder
import com.joshtalks.joshskills.ui.chat.vh.LessonViewHolder
import com.joshtalks.joshskills.ui.chat.vh.NewMessageViewHolder
import com.joshtalks.joshskills.ui.chat.vh.PdfViewHolder
import com.joshtalks.joshskills.ui.chat.vh.PracticeOldViewHolder
import com.joshtalks.joshskills.ui.chat.vh.TextViewHolder
import com.joshtalks.joshskills.ui.chat.vh.UnlockNextClassViewHolder
import com.joshtalks.joshskills.ui.chat.vh.VideoViewHolder
import com.joshtalks.joshskills.util.StickyHeaderAdapter
import com.joshtalks.joshskills.util.Utils
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Locale
import timber.log.Timber


class ConversationAdapter(private val activityRef: WeakReference<FragmentActivity>) :
    RecyclerView.Adapter<BaseViewHolder>(),
    StickyHeaderAdapter<DateItemHolder> {
    private val userId = Mentor.getInstance().getId()
    private var messageList: ArrayList<ChatModel> = arrayListOf()
    private val slowList: MutableSet<ChatModel> = mutableSetOf()
    private val uiHandler = AppObjectController.uiHandler

    init {
        setHasStableIds(true)
    }

    private fun checkListIsChange(newList: List<ChatModel>): Boolean {
        val size = slowList.size
        slowList.addAll(newList)

        if (size == slowList.size) {
            return false
        }
        return true
    }

    fun addMessagesList(newList: List<ChatModel>) {
        if (newList.isEmpty()) {
            return
        }
        if (checkListIsChange(newList)) {
            val oldPos = messageList.size
            this.messageList.addAll(newList)
            notifyItemRangeInserted(oldPos, newList.size)
        }
    }

    fun addMessageAboveMessage(newList: List<ChatModel>): Boolean {
        if (newList.isEmpty()) {
            return false
        }
        if (checkListIsChange(newList)) {
            this.messageList.addAll(0, newList)
            notifyItemRangeInserted(0, newList.size)
            return true
        }
        return false

    }


    fun addMessage(message: ChatModel) {
        uiHandler.post {
            messageList.add(message)
            notifyItemInserted(messageList.size - 1)
        }
    }

    fun getLastItem(): ChatModel {
        return try {
            messageList.last()
        } catch (ex: NoSuchElementException) {
            ChatModel()
        }
    }

    fun getFirstItem(): ChatModel {
        return messageList.first()
    }

    fun getItemAtPosition(position: Int): ChatModel {
        return messageList[position]
    }

    fun getMessagePositionById(id: String): Int {
        return messageList.indexOfLast { it.chatId == id }
    }

    fun updateItem(newMessage: ChatModel): Boolean {
        val position: Int = getMessagePositionById(newMessage.chatId)
        Timber.tag("ConversationAdapter").e("%s", position)
        return if (position >= 0) {
            uiHandler.post {
                messageList[position] = newMessage
                notifyItemChanged(position)
            }
            true
        } else {
            false
        }
    }

    fun notifyItem(chatId: String) {
        val index: Int = getMessagePositionById(chatId)
        notifyItemChanged(index)
    }


    fun addUnlockClassMessage(message: ChatModel): Boolean {
        val index = messageList.indexOfLast { it.type == BASE_MESSAGE_TYPE.UNLOCK }
        if (index == -1) {
            addMessage(message)
            return true
        }
        return false
    }


    fun removeUnlockMessage() {
        val index = messageList.indexOfLast { it.type == BASE_MESSAGE_TYPE.UNLOCK }
        if (index > -1) {
            messageList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun isLessonType(): Boolean {
        return messageList.stream().anyMatch { it.type == BASE_MESSAGE_TYPE.LESSON }
    }

    fun removeNewClassCard() {
        val index = messageList.indexOfLast { it.type == BASE_MESSAGE_TYPE.NEW_CLASS }
        if (index > -1) {
            messageList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun getLastLesson(): LessonModel? {
        return messageList.lastOrNull { it.type == BASE_MESSAGE_TYPE.LESSON }?.lesson
    }

    fun isUserAttemptedLesson(): Boolean {
        val count = messageList.stream()
            .filter { it.lesson != null && it.lesson?.status != LESSON_STATUS.NO }.count()
        if (count <= 2) {
            return true
        }
        return false
    }
    fun firstUnseenMessage(): Int {
        return messageList.indexOfFirst { it.isSeen.not() }
    }


    @MainThread
    fun initializePool(@NonNull pool: RecyclerView.RecycledViewPool) {
        pool.setMaxRecycledViews(LESSON_MESSAGE, 15)
        /*      pool.setMaxRecycledViews(MESSAGE_TYPE_INCOMING_MULTIMEDIA, 15)
            pool.setMaxRecycledViews(MESSAGE_TYPE_OUTGOING_TEXT, 15)
            pool.setMaxRecycledViews(MESSAGE_TYPE_OUTGOING_MULTIMEDIA, 15)
            pool.setMaxRecycledViews(MESSAGE_TYPE_PLACEHOLDER, 15)
            pool.setMaxRecycledViews(MESSAGE_TYPE_HEADER, 1)
            pool.setMaxRecycledViews(MESSAGE_TYPE_FOOTER, 1)
            pool.setMaxRecycledViews(MESSAGE_TYPE_UPDATE, 5)*/
    }

    override fun getItemId(position: Int): Long {
        return messageList[position].chatId.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val view: View

        return when (viewType) {
            LEFT_TEXT_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_text_message_left, parent, false)
                view.tag = LEFT_TEXT_MESSAGE
                TextViewHolder(view, userId)
            }
            RIGHT_TEXT_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_text_message_right, parent, false)
                view.tag = RIGHT_TEXT_MESSAGE
                TextViewHolder(view, userId)
            }
            LEFT_IMAGE_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_image_message_left, parent, false)
                view.tag = LEFT_IMAGE_MESSAGE
                ImageViewHolder(view, userId)
            }
            RIGHT_IMAGE_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_image_message_right, parent, false)
                view.tag = RIGHT_IMAGE_MESSAGE
                ImageViewHolder(view, userId)
            }
            LEFT_AUDIO_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_audio_message_left, parent, false)
                view.tag = LEFT_AUDIO_MESSAGE
                AudioViewHolder(view, activityRef, userId)
            }
            RIGHT_AUDIO_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_audio_message_right, parent, false)
                view.tag = RIGHT_AUDIO_MESSAGE
                AudioViewHolder(view, activityRef, userId)
            }
            LEFT_VIDEO_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_video_message_left, parent, false)
                view.tag = LEFT_VIDEO_MESSAGE
                VideoViewHolder(view, activityRef, userId)
            }
            RIGHT_VIDEO_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_video_message_left, parent, false)
                view.tag = RIGHT_VIDEO_MESSAGE
                VideoViewHolder(view, activityRef, userId)
            }
            PDF_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_pdf_message, parent, false)
                view.tag = PDF_MESSAGE
                PdfViewHolder(view, activityRef, userId)
            }
            CERTIFICATION_EXAM_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.certification_exam_layout, parent, false)
                view.tag = CERTIFICATION_EXAM_MESSAGE
                CertificationExamViewHolder(view, userId)
            }
            PRACTICE_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_practice_message, parent, false)
                view.tag = PRACTICE_MESSAGE
                PracticeOldViewHolder(view, userId)
            }
            ASSESSMENT_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.assessment_item_layout, parent, false)
                view.tag = ASSESSMENT_MESSAGE
                AssessmentViewHolder(view, userId)
            }
            LESSON_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_lesson_item, parent, false)
                view.tag = LESSON_MESSAGE
                LessonViewHolder(view, userId)
            }
            BEST_PERFORMER_EXAM_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_best_performer_item, parent, false)
                view.tag = BEST_PERFORMER_EXAM_MESSAGE
                BestStudentPerformerViewHolder(view, userId)
            }
            NEW_CLASS_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.new_message_layout, parent, false)
                view.tag = NEW_CLASS_MESSAGE
                NewMessageViewHolder(view, userId)
            }
            UNLOCK_CLASS_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.unlock_class_item_layout, parent, false)
                view.tag = UNLOCK_CLASS_MESSAGE
                UnlockNextClassViewHolder(view, userId)
            }

            //LessonViewHolder

            else -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_text_message_left, parent, false)
                view.tag = RIGHT_TEXT_MESSAGE
                TextViewHolder(view, userId)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (messageList[position].type == BASE_MESSAGE_TYPE.LESSON) {
            holder.setIsRecyclable(false)
        }
        holder.bind(messageList[position], getPreviousMessage(position))
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.unBind()
    }

    override fun getItemViewType(position: Int): Int {
        val type: BASE_MESSAGE_TYPE?
        val item = messageList[position]
        val prevSender = getPreviousSender(position)
        val questionType = item.question?.type ?: BASE_MESSAGE_TYPE.TX
        val questionMaterialType = item.question?.type ?: BASE_MESSAGE_TYPE.TX


        if (item.type == BASE_MESSAGE_TYPE.Q) {

            type = when (questionType) {
                BASE_MESSAGE_TYPE.P2P,
                BASE_MESSAGE_TYPE.PR,
                BASE_MESSAGE_TYPE.OTHER,
                BASE_MESSAGE_TYPE.QUIZ,
                BASE_MESSAGE_TYPE.TEST,
                BASE_MESSAGE_TYPE.CE,
                BASE_MESSAGE_TYPE.CP,
                BASE_MESSAGE_TYPE.BEST_PERFORMER -> {
                    questionType
                }
                else -> {
                    item.question?.material_type
                }
            }
        } else {
            type = item.type
        }
        return getViewResourceId(type, item.sender, prevSender)
    }

    private fun getViewResourceId(
        materialType: BASE_MESSAGE_TYPE?,
        cSender: Sender?,
        oSender: Sender?
    ): Int {
        when (materialType) {
            BASE_MESSAGE_TYPE.TX ->
                return type(cSender, oSender, LEFT_TEXT_MESSAGE, RIGHT_TEXT_MESSAGE)
            BASE_MESSAGE_TYPE.IM ->
                return type(cSender, oSender, LEFT_IMAGE_MESSAGE, RIGHT_IMAGE_MESSAGE)
            BASE_MESSAGE_TYPE.AU ->
                return type(cSender, oSender, LEFT_AUDIO_MESSAGE, RIGHT_AUDIO_MESSAGE)
            BASE_MESSAGE_TYPE.PD ->
                return PDF_MESSAGE
            BASE_MESSAGE_TYPE.VI ->
                return type(cSender, oSender, LEFT_VIDEO_MESSAGE, RIGHT_VIDEO_MESSAGE)
            BASE_MESSAGE_TYPE.PR ->
                return PRACTICE_MESSAGE
            BASE_MESSAGE_TYPE.QUIZ, BASE_MESSAGE_TYPE.TEST ->
                return ASSESSMENT_MESSAGE
            BASE_MESSAGE_TYPE.CE ->
                return CERTIFICATION_EXAM_MESSAGE
            BASE_MESSAGE_TYPE.UNLOCK -> {
                return UNLOCK_CLASS_MESSAGE
            }
            BASE_MESSAGE_TYPE.LESSON -> {
                return LESSON_MESSAGE
            }
            BASE_MESSAGE_TYPE.BEST_PERFORMER ->
                return BEST_PERFORMER_EXAM_MESSAGE
            BASE_MESSAGE_TYPE.NEW_CLASS -> {
                return NEW_CLASS_MESSAGE
            }
            else -> {
                return -1
            }
        }
    }

    private fun type(cSender: Sender?, lastSender: Sender?, lType: Int, rType: Int): Int {
        if (lastSender == null) {
            return if (cSender?.id.equals(userId, ignoreCase = true)) {
                rType
            } else {
                lType
            }
        } else {
            return if (lastSender.id == cSender?.id || lastSender.id == userId) {
                if (cSender?.id.equals(userId, ignoreCase = true)) {
                    rType
                } else {
                    lType
                }
            } else { // balloon bg
                if (cSender?.id.equals(userId, ignoreCase = true)) {
                    rType
                } else {
                    lType
                }
            }
        }
    }

    private fun getPreviousSender(position: Int): Sender? {
        return try {
            messageList[position - 1].sender
        } catch (ex: Exception) {
            null
        }
    }

    private fun getPreviousMessage(position: Int): ChatModel? {
        return try {
            messageList[position - 1]
        } catch (ex: Exception) {
            null
        }
    }

    override fun getHeaderId(position: Int): Long {
        val baseMessage: ChatModel = messageList[position]
        return Utils.getDateId(baseMessage.created.time).toLong()
    }

    override fun onCreateHeaderViewHolder(var1: ViewGroup): DateItemHolder {
        val view = LayoutInflater.from(var1.context).inflate(
            R.layout.cell_date_layout,
            var1, false
        )

        return DateItemHolder(view)
    }

    override fun onBindHeaderViewHolder(
        var1: DateItemHolder,
        position: Int,
        var3: Long
    ) {
        val baseMessage: ChatModel = messageList[position]
        val formattedDate = dateHeaderDateFormat(baseMessage.created)
        var1.txtMessageDate.text = formattedDate.toUpperCase(Locale.getDefault())
    }

    override fun getItemCount(): Int {
        return messageList.size
    }
}


private const val LEFT_TEXT_MESSAGE = 1
private const val RIGHT_TEXT_MESSAGE = 2

private const val LEFT_IMAGE_MESSAGE = 3
private const val RIGHT_IMAGE_MESSAGE = 4

private const val LEFT_AUDIO_MESSAGE = 5
private const val RIGHT_AUDIO_MESSAGE = 6

private const val PDF_MESSAGE = 7
//private const val RIGHT_PDF_MESSAGE = 8

private const val LEFT_VIDEO_MESSAGE = 9
private const val RIGHT_VIDEO_MESSAGE = 10

private const val PRACTICE_MESSAGE = 11

private const val LEFT_QUIZ_TEST_MESSAGE = 13
private const val RIGHT_QUIZ_TEST_MESSAGE = 14

private const val CERTIFICATION_EXAM_MESSAGE = 15
//private const val RIGHT_CERTIFICATION_EXAM_MESSAGE = 16

private const val ASSESSMENT_MESSAGE = 17

private const val LESSON_MESSAGE = 19


private const val BEST_PERFORMER_EXAM_MESSAGE = 21
//private const val BEST_PERFORMER_EXAM_MESSAGE = 21

private const val NEW_CLASS_MESSAGE = 40
private const val UNLOCK_CLASS_MESSAGE = 41


private const val TEMP = 0



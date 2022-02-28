package com.joshtalks.joshskills.repository.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.minimalentity.CourseContentEntity
import java.util.Date

@Dao
interface ChatDao {

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND is_delete_message=0 ORDER BY created ASC,question_id   ")
    suspend fun getLastChats(conversationId: String): List<ChatModel>

    @Query(value = "SELECT * FROM chat_table  where chat_id=:chatId")
    suspend fun getChatObject(chatId: String): ChatModel

    @Query(value = "SELECT * FROM chat_table  where chat_id=:chatId")
    suspend fun getNullableChatObject(chatId: String): ChatModel?

    @Query("UPDATE chat_table SET downloadStatus = :status , downloadedLocalPath = :path, duration = :duration   where chat_id=:chatId")
    fun updateDownloadStatus(
        chatId: String,
        status: DOWNLOAD_STATUS,
        path: String = EMPTY,
        duration: Int = 0
    )

    @Query("UPDATE PdfTable SET downloadedLocalPath = :path  where id=:pdfId")
    fun updatePdfPath(pdfId: String, path: String)

    @Query("UPDATE AudioTable SET downloadedLocalPath = :path  where audioId=:audioId")
    fun updateAudioPath(audioId: String, path: String)

    @Query("UPDATE VideoTable SET downloadedLocalPath = :path  where id=:videoId")
    fun updateVideoDownloadStatus(videoId: String, path: String)

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND created > :compareTime AND is_delete_message=0  ORDER BY created ASC,question_id ASC")
    suspend fun getRecentChatAfterTime(conversationId: String, compareTime: Date?): List<ChatModel>

    @Query(value = "SELECT COUNT(chat_id) FROM chat_table where conversation_id= :conversationId ")
    suspend fun getTotalCountOfRows(conversationId: String): Long

    @Query("UPDATE chat_table SET message_deliver_status = :messageDeliverStatus where created <= :compareTime ")
    fun updateSeenMessages(
        messageDeliverStatus: MESSAGE_DELIVER_STATUS = MESSAGE_DELIVER_STATUS.READ,
        compareTime: Date
    )

    @Query("UPDATE chat_table SET is_sync =1 where chat_id <= :id ")
    suspend fun forceFullySync(id: String)

    @Query(value = "SELECT * FROM chat_table where  is_sync= 0")
    suspend fun getUnSyncMessage(): List<ChatModel>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReadingVideoDownloadedPath(readingVideo: ReadingVideo)

    @Query("SELECT path FROM reading_video where id= :questionId")
    suspend fun getDownloadedVideoPath(questionId: String): String

    @Query("SELECT isDownloaded FROM reading_video where id= :questionId")
    suspend fun getDownloadedVideoStatus(questionId: String): Boolean

    @Insert
    suspend fun insertCompressedVideo(compressedVideo: CompressedVideo)

    @Query("SELECT path FROM compressed_video where id= :questionId")
    suspend fun getCompressedVideo(questionId: String): String

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAMessage(chat: ChatModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateChatMessage(chat: ChatModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateChatMessageOnAnyThread(chat: ChatModel)

    @Delete
    suspend fun deleteChatMessage(chat: ChatModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageList(chatModelList: List<ChatModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatQuestion(question: Question)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatQuestions(question: List<Question>)

    @Query("SELECT * FROM question_table WHERE chatId= :chatId")
    suspend fun getQuestion(chatId: String): Question?

    @Query("SELECT * FROM question_table WHERE questionId= :questionId")
    suspend fun getQuestionByQuestionId(questionId: String): Question?

    @Query("SELECT * FROM question_table WHERE lesson_id= :lessonId")
    fun getQuestionsForLesson(lessonId: String): LiveData<List<Question>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioMessageList(audioList: List<AudioType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoMessageList(audioList: List<VideoType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdfMessageList(pdfList: List<PdfType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptionTypeMessageList(audioList: List<OptionType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageTypeMessageList(audioList: List<ImageType>)

    @Query("SELECT * FROM ImageTable WHERE questionId= :questionId")
    suspend fun getImagesOfQuestion(questionId: String): List<ImageType>

    @Query("SELECT * FROM VideoTable WHERE questionId= :questionId")
    suspend fun getVideosOfQuestion(questionId: String): List<VideoType>

    @Query("SELECT * FROM AudioTable WHERE questionId= :questionId")
    suspend fun getAudiosOfQuestion(questionId: String): List<AudioType>

    @Query("SELECT * FROM PdfTable WHERE questionId= :questionId")
    suspend fun getPdfOfQuestion(questionId: String): List<PdfType>

    @Update
    fun updateAudioObject(vararg audioList: AudioType)

    @Update
    fun updateImageObject(vararg imageObj: ImageType)

    @Update
    fun updateVideoObject(vararg videoObj: VideoType)

    @Update
    fun updatePdfObject(vararg pdfObj: PdfType)

    @Query("UPDATE chat_table SET downloadStatus = :status where downloadStatus == :whereStatus")
    suspend fun updateDownloadVideoStatusFailed(
        status: DOWNLOAD_STATUS = DOWNLOAD_STATUS.NOT_START,
        whereStatus: DOWNLOAD_STATUS = DOWNLOAD_STATUS.DOWNLOADING
    )

    @Query(value = "UPDATE PdfTable SET total_view = :total_view where id= :id ")
    suspend fun updateTotalViewForPdf(id: String, total_view: Int)

    @Query(value = "UPDATE reading_video SET path = :new_path, isDownloaded = :downloaded where id= :questionId ")
    suspend fun updateReadingTable(questionId: String, new_path: String, downloaded: Boolean)

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId ORDER BY ID DESC LIMIT 1")
    suspend fun getLastOneChat(conversationId: String): ChatModel?

    suspend fun getMaxIntervalForVideo(conversationId: String): Int {
        val chatModel: ChatModel? = getLastQuestionInterval(conversationId)
        if (chatModel?.type == BASE_MESSAGE_TYPE.Q) {
            val question: Question? = getQuestion(chatModel.chatId)
            if (question != null) {
                return question.interval.plus(1)
            }
        }
        return 0
    }

    @Query("SELECT * FROM question_table WHERE course_id= :course_id AND interval=:interval")
    suspend fun getQuestionForNextInterval(course_id: String, interval: Int): Question?

    @Query(value = "SELECT COUNT(questionId) FROM question_table where course_id= :course_id  AND interval>:interval")
    suspend fun nextQuestionIntervalExist(course_id: String, interval: Int): Long

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND type= :type ")
    suspend fun getUnlockChatModel(
        conversationId: String,
        type: BASE_MESSAGE_TYPE = BASE_MESSAGE_TYPE.UNLOCK
    ): List<ChatModel?>?

    @Query(value = "SELECT * FROM chat_table where question_id IS NOT NULL AND conversation_id= :conversationId ORDER BY created DESC LIMIT 1; ")
    suspend fun getLastQuestionInterval(conversationId: String): ChatModel?

    @Query(value = "DELETE FROM chat_table where conversation_id= :conversationId AND type=:type")
    suspend fun deleteSpecificTypeChatModel(conversationId: String, type: BASE_MESSAGE_TYPE)

    @Query(value = "UPDATE chat_table  SET is_seen = 1 where conversation_id= :conversationId")
    suspend fun readAllChatBYUser(conversationId: String)

    @Query("UPDATE chat_table SET is_delete_message =1 WHERE chat_id IN (:ids)")
    suspend fun changeStatusForDeleteMessage(ids: List<String>)

    @Query(value = "SELECT * FROM chat_table where is_delete_message=1 ")
    suspend fun getUnsyncDeletesMessage(): List<ChatModel>

    @Query("DELETE FROM chat_table where  chat_id IN (:ids)")
    suspend fun deleteUserMessages(ids: List<String>)

    @Query("UPDATE chat_table SET download_progress = :progress where id= :conversationId ")
    suspend fun videoProgressUpdate(conversationId: String, progress: Int)

    @Query("SELECT * FROM chat_table where conversation_id= :conversationId  ORDER BY created ASC LIMIT 1;")
    suspend fun getLastRecord(conversationId: String): ChatModel

    @Query("UPDATE chat_table SET status = :status WHERE chat_id IN (:ids)")
    suspend fun updateMessageStatus(status: MESSAGE_STATUS, ids: List<String>)

    @Query(value = "SELECT chat_id FROM chat_table where status=:status")
    suspend fun getSeenByUserMessages(status: MESSAGE_STATUS = MESSAGE_STATUS.SEEN_BY_USER): List<String>

    @Update
    suspend fun updateQuestionObject(vararg question: Question)

    @Query("UPDATE question_table set status = :questionStatus WHERE questionId=:questionId")
    suspend fun updateQuestionStatus(questionId: String, questionStatus: QUESTION_STATUS)

    @Query("UPDATE question_table set status = :questionStatus AND lesson_status = :lessonStatus WHERE questionId=:questionId")
    suspend fun updateQuestionAndLessonStatus(
        questionId: String,
        questionStatus: QUESTION_STATUS,
        lessonStatus: LESSON_STATUS
    )

    @Query("SELECT lesson_id FROM question_table WHERE questionId = :questionId")
    fun getLessonIdOfQuestion(questionId: String): Int

    @Query("UPDATE question_table SET practice_engagements = :practiseEngagement  WHERE questionId= :questionId")
    suspend fun updatePractiseObject(
        questionId: String,
        practiseEngagement: List<PracticeEngagement>
    )

    @Query("SELECT practice_engagements FROM question_table  WHERE questionId= :questionId")
    suspend fun getPractiseObject(
        questionId: String
    ): String?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query(value = "SELECT * FROM (SELECT *,qt.type AS 'question_type' FROM chat_table ct LEFT JOIN question_table qt ON ct.chat_id = qt.chatId where qt.type= :typeO AND  title IS NOT NULL ) inbox  where type= :typeO AND conversation_id= :conversationId  ORDER BY created ASC;")
    suspend fun getRegisterCourseMinimal22(
        conversationId: String,
        typeO: BASE_MESSAGE_TYPE = BASE_MESSAGE_TYPE.Q
    ): List<CourseContentEntity>

    @Query("SELECT * FROM  question_table  WHERE questionId= :questionId")
    suspend fun getQuestionOnId(questionId: String): Question?

    @Query("SELECT * FROM  question_table  WHERE questionId= :questionId")
    fun getQuestionOnIdV2(questionId: String): Question?

    @Transaction
    suspend fun getPractiseFromQuestionId(id: String): ChatModel? {
        val question: Question? = getQuestionOnId(id)
        return if (question == null) null
        else {
            getUpdatedChatObjectViaId(question.chatId)
        }
    }

    @Transaction
    suspend fun getChatFromQuestionId(chatId: String): ChatModel? {
        return getUpdatedChatObjectViaId(chatId)
    }

    @Query("SELECT * FROM  PdfTable  WHERE id= :pdfId")
    suspend fun getPdfById(pdfId: String): PdfType

    @Query("UPDATE chat_table SET last_use_time = :date where chat_id=:chatId ")
    fun lastUsedBy(chatId: String, date: Date = Date())

    @Query(value = "SELECT * FROM chat_table where last_use_time ORDER BY last_use_time ASC")
    suspend fun getAllRecentDownloadMedia(): List<ChatModel>

    @Query("SELECT * FROM  question_table  WHERE certificate_exam_id= :certificateExamId")
    suspend fun getQuestionUsingCExamId(certificateExamId: Int): Question?

    @Transaction
    suspend fun insertCertificateExamDetail(
        certificateExamId: Int,
        obj: CertificationExamDetailModel
    ) {
        getQuestionUsingCExamId(certificateExamId)?.apply {
            cexamDetail = obj
            updateQuestionObject(this)
        }
    }

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("SELECT  * FROM chat_table LEFT JOIN question_table ON chat_id = chatId")
    fun testApi(): List<ChatModel>

    @Query(value = "SELECT COUNT(chat_id) FROM chat_table where conversation_id= :conversationId AND is_delete_message=0 AND is_seen= 0")
    suspend fun unreadMessageCount(conversationId: String): Long

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND is_delete_message=0 AND is_seen= 0 ORDER BY message_time ASC LIMIT 1")
    suspend fun getLastUnreadReadMessage(conversationId: String): ChatModel?

    suspend fun getOneShotMessageList(conversationId: String): List<ChatModel> {
        return getLastChatsV2 { getOneShotMessage(conversationId) }
    }

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND is_delete_message=0  ORDER BY message_time DESC,question_id DESC  LIMIT 15")
    suspend fun getOneShotMessage(conversationId: String): List<ChatModel>

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND message_time < :messageTime AND is_delete_message=0  ORDER BY message_time DESC,question_id ASC  LIMIT :limit")
    suspend fun getOldPagingMessage(
        conversationId: String,
        messageTime: Double,
        limit: Int
    ): List<ChatModel>

    suspend fun getPagingMessage(
        conversationId: String,
        compareTime: Double,
        limit: Int = 15
    ): List<ChatModel> {
        return getLastChatsV2 {
            getOldPagingMessage(
                conversationId,
                messageTime = compareTime,
                limit = limit
            )
        }
    }

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND  message_time >= :messageTime AND  is_delete_message=0 AND is_seen= 0  ORDER BY message_time ASC,question_id ASC ")
    suspend fun getUnreadReadMessages(conversationId: String, messageTime: Double): List<ChatModel>

    suspend fun getUnreadMessageList(conversationId: String, messageTime: Double): List<ChatModel> {
        return getLastChatsV2 {
            getUnreadReadMessages(
                conversationId,
                messageTime = messageTime
            )
        }
    }

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND  message_time > :messageTime AND  is_delete_message=0 ORDER BY message_time ASC,question_id ASC ")
    suspend fun getNewMessages(conversationId: String, messageTime: Double): List<ChatModel>

    suspend fun getNewFetchMessages(conversationId: String, messageTime: Double): List<ChatModel> {
        return getLastChatsV2 {
            getNewMessages(
                conversationId,
                messageTime = messageTime
            )
        }
    }

    @Transaction
    suspend fun getLastChatsV2(callback: suspend () -> List<ChatModel>): List<ChatModel> {
        val listOfChat: List<ChatModel> = callback.invoke()
        if (listOfChat.isEmpty()) {
            return emptyList()
        }

        listOfChat.forEach { chatModel ->
            chatModel.awardMentorModel = getAwardMentor(chatModel)

            chatModel.lesson = getLesson(chatModel) // Add Lesson

            chatModel.question = getQuestion(chatModel) // Add Question
        }
        return listOfChat
    }

    @Transaction
    suspend fun getUpdatedChatObjectViaId(id: String): ChatModel {
        val chatModel: ChatModel = getChatObject(chatId = id)
        try {
            chatModel.awardMentorModel = getAwardMentor(chatModel)

            chatModel.lesson = getLesson(chatModel) // Add Lesson

            chatModel.question = getQuestion(chatModel) // Add Question
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }

        return chatModel
    }

    private fun getAwardMentor(chatModel: ChatModel): AwardMentorModel? {
        // Add Award
        return chatModel.awardUserId?.let {
            AppObjectController.appDatabase.awardMentorModelDao().getAwardMentorModel(it)
        }
    }

    private fun getLesson(chatModel: ChatModel): LessonModel? {
        if (chatModel.type == BASE_MESSAGE_TYPE.LESSON) {
            //  val courseId=getCourseId(chatModel.conversationId)
            return AppObjectController.appDatabase.lessonDao()
                .getLessonFromChatId(chatModel.chatId)
        }
        return null
    }

    private fun getCourseId(conversationId: String): String {
        return AppObjectController.appDatabase.courseDao()
            .getCourseIdFromConversationId(conversationId)
    }

    private suspend fun getQuestion(chatModel: ChatModel): Question? {
        if (chatModel.type == BASE_MESSAGE_TYPE.Q) {
            val question: Question? = getQuestion(chatModel.chatId)
            if (null != question) {
                when (question.material_type) {
                    BASE_MESSAGE_TYPE.IM ->
                        question.imageList =
                            getImagesOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.VI ->
                        question.videoList =
                            getVideosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.AU ->
                        question.audioList =
                            getAudiosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.PD ->
                        question.pdfList =
                            getPdfOfQuestion(questionId = question.questionId)
                }

                // Add Pr V2
                /*  if (question.type == BASE_MESSAGE_TYPE.PR) {
                      question.practiseEngagementV2 =
                          AppObjectController.appDatabase.practiceEngagementDao()
                              .getPractice(question.questionId)
                      question.imageList = AppObjectController.appDatabase.chatDao()
                          .getImagesOfQuestion(questionId = question.questionId)
                  }*/

                if (question.assessmentId != null) {
                    question.vAssessmentCount = AppObjectController.appDatabase.assessmentDao()
                        .countOfAssessment(question.assessmentId)
                }
            }
            return question
        }
        return null
    }
}

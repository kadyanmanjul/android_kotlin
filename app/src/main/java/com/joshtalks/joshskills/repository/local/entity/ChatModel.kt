package com.joshtalks.joshskills.repository.local.entity

import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.util.RandomString
import java.io.Serializable
import java.util.*


@Entity(tableName = "chat_table", indices = [Index(value = ["chat_id", "conversation_id"])])
data class ChatModel(
    @PrimaryKey
    @ColumnInfo(name = "chat_id")
    @SerializedName("id") var chatId: String = "",

    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversation") var conversationId: String = "",

    @ColumnInfo()
    @SerializedName("created") var created: Date,

    @ColumnInfo(name = "is_seen")
    @SerializedName("is_seen") var isSeen: Boolean = false,

    @Ignore
    @SerializedName("question")
    var question: Question? = null,


    @Embedded
    @SerializedName("sender")
    var sender: Sender? = Sender(),

    @ColumnInfo()
    @SerializedName("text") var text: String? = "",

    @ColumnInfo()
    @SerializedName("type") var type: BASE_MESSAGE_TYPE? = BASE_MESSAGE_TYPE.TX,

    @ColumnInfo()
    @SerializedName("url") var url: String?,

    @ColumnInfo()
    var mediaDuration: Long? = 0,


    @ColumnInfo(name = "message_deliver_status")
    var messageDeliverStatus: MESSAGE_DELIVER_STATUS = MESSAGE_DELIVER_STATUS.READ,

    @ColumnInfo(name = "is_sync")
    var isSync: Boolean = true,

    @ColumnInfo(name = "chat_local_id")
    var chatLocalId: String? = RandomString().nextString()


) : DataBaseClass(), Serializable {

    constructor() : this(
        chatId = "",
        conversationId = "",
        created = Date(),
        isSeen = false,
        question = Question(),
        sender = Sender(),
        type = BASE_MESSAGE_TYPE.TX,
        url = "",
        text = "",
        messageDeliverStatus = MESSAGE_DELIVER_STATUS.READ,
        mediaDuration = 0,
        isSync = true,
        chatLocalId = null
    )
}


@Entity(
    tableName = "question_table"
)
data class Question(

    @PrimaryKey
    @ColumnInfo(name = "questionId")
    @SerializedName("id") var questionId: String = "",


    @ColumnInfo()
    @Expose var chatId: String = "",

    @Ignore
    @SerializedName("audios") var audioList: List<AudioType>? = null,

    @ColumnInfo()
    @SerializedName("course_id") var course_id: Int = 0,


    @Ignore
    @SerializedName("images") var imageList: List<ImageType>? = null,

    @ColumnInfo()
    @SerializedName("is_deleted") var isDeleted: Boolean = false,


    @ColumnInfo()
    @SerializedName("material_type") var material_type: BASE_MESSAGE_TYPE = BASE_MESSAGE_TYPE.TX,

    @Ignore
    @SerializedName("options") var optionsList: List<OptionType>? = null,

    @ColumnInfo()
    @SerializedName("parent_id") var parent_id: String? = "",

    @Ignore
    @SerializedName("pdf") var pdfList: List<PdfType>? = null,

    @ColumnInfo(name = "qText")
    @SerializedName("text") var qText: String? = "",

    @ColumnInfo()
    @SerializedName("title") var title: String? = "",

    @ColumnInfo(name = "question_type")
    @SerializedName("type") var questionType: String = "",

    @Ignore
    @SerializedName("videos") var videoList: List<VideoType>? = emptyList()
) : Serializable


data class Sender(
    @SerializedName("id") var id: String = "",

    @SerializedName("user") var user: User = User(),

    @SerializedName("user_type") var user_type: String = ""
) : Serializable


data class User(
    @SerializedName("first_name") var first_name: String = "",

    @SerializedName("id") var id: String = "",

    @SerializedName("last_name") var last_name: String = "",

    @SerializedName("photo_url") var photo_url: String = "",

    @SerializedName("user_type") var user_type: String = ""
) : Serializable


@Entity(tableName = "VideoTable")
data class VideoType(
    @ColumnInfo()
    @SerializedName("video_url") var video_url: String? = "",

    @PrimaryKey
    @ColumnInfo()
    @SerializedName("id") var id: String = "",

    @ColumnInfo()
    @SerializedName("thumbnail_url") var video_image_url: String = "",

    @ColumnInfo()
    @SerializedName("duration") var duration: Int = 0,

    @ColumnInfo()
    @SerializedName("video_height") var video_height: Int = 0,

    @ColumnInfo()
    @SerializedName("video_width") var video_width: Int = 0,

    @ColumnInfo()
    @SerializedName("thumbnail_height") var thumbnail_height: Int = 0,

    @ColumnInfo()
    @SerializedName("thumbnail_width") var thumbnail_width: Int = 0,

    @ColumnInfo()
    @SerializedName("bit_rate") var bit_rate: Int = 0,

    @ColumnInfo()
    @SerializedName("is_deleted") var is_deleted: Boolean = false

) : DataBaseClass(), Serializable


@Entity(tableName = "AudioTable")
data class AudioType(
    @ColumnInfo()
    @SerializedName("audio_url") var audio_url: String = "",

    @PrimaryKey
    @ColumnInfo(name = "audioId")
    @SerializedName("id") var id: String = "",


    @ColumnInfo()
    @SerializedName("duration") var duration: Int = 0,

    @ColumnInfo()
    @SerializedName("bit_rate") var bit_rate: Int = 0,

    @ColumnInfo()
    @SerializedName("is_deleted") var is_deleted: Boolean = false

) : DataBaseClass(), Serializable


@Entity(tableName = "OptionTable")
data class OptionType(

    @PrimaryKey
    @ColumnInfo()
    @SerializedName("id") var id: String = "",


    @ColumnInfo()
    @SerializedName("type") var type: String = "",

    @ColumnInfo()
    @SerializedName("text") var text: String = "",


    @ColumnInfo()
    @SerializedName("order") var order: Int = 0

) : DataBaseClass(), Serializable


@Entity(tableName = "PdfTable")
data class PdfType(

    @PrimaryKey
    @ColumnInfo()
    @SerializedName("id") var id: String = "",


    @ColumnInfo()
    @SerializedName("url") var url: String = "",

    @ColumnInfo()
    @SerializedName("is_deleted") var is_deleted: Boolean = false,

    @ColumnInfo(name = "total_view")
    var totalView: Int = 0

) : DataBaseClass(), Serializable


@Entity(tableName = "ImageTable")
data class ImageType(

    @PrimaryKey
    @ColumnInfo()
    @SerializedName("id") var id: String = "",

    @ColumnInfo()
    @SerializedName("image_url") var imageUrl: String = "",

    @ColumnInfo()
    @SerializedName("is_deleted") var is_deleted: Boolean = false,

    @ColumnInfo()
    @SerializedName("height") var height: Int = 0,

    @ColumnInfo()
    @SerializedName("width") var width: Int = 0

) : DataBaseClass(), Serializable

open class DataBaseClass : Serializable {

    @ColumnInfo()
    @Expose
    var questionId: String = ""

    @ColumnInfo()
    @Expose
    var downloadStatus: DOWNLOAD_STATUS = DOWNLOAD_STATUS.DOWNLOADED

    @ColumnInfo()
    @Expose
    var downloadedLocalPath: String? = ""

    @ColumnInfo()
    @Expose
    var lastDownloadStartTime: Long = 0

    @ColumnInfo()
    @Expose
    var thumbnailUrl: String? = ""
}


@Dao
interface ChatDao {

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId  ORDER BY created ASC ")
    suspend fun getLastChats(conversationId: String): List<ChatModel>


    @Query(value = "SELECT * FROM chat_table  where chat_id=:chatId")
    suspend fun getChatObject(chatId: String): ChatModel


    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND created > :compareTime ORDER BY created ASC")
    suspend fun getRecentChatAfterTime(conversationId: String, compareTime: Date?): List<ChatModel>


    @Query("UPDATE chat_table SET message_deliver_status = :messageDeliverStatus where created <= :compareTime ")
    suspend fun updateSeenMessages(
        messageDeliverStatus: MESSAGE_DELIVER_STATUS = MESSAGE_DELIVER_STATUS.READ,
        compareTime: Date
    )

    @Query(value = "SELECT * FROM chat_table where  is_sync= 0")
    suspend fun getUnSyncMessage(): List<ChatModel>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAMessage(chat: ChatModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateChatMessage(chat: ChatModel)

    @Delete
    fun deleteChatMessage(chat: ChatModel)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageList(chatModelList: List<ChatModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatQuestion(question: Question)

    @Query("SELECT * FROM question_table WHERE chatId= :chatId")
    suspend fun getQuestion(chatId: String): Question?


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
    suspend fun updateAudioObject(vararg audioList: AudioType)

    @Update
    suspend fun updateImageObject(vararg imageObj: ImageType)

    @Update
    suspend fun updateVideoObject(vararg videoObj: VideoType)

    @Update
    suspend fun updatePdfObject(vararg pdfObj: PdfType)

    @Transaction
    suspend fun getUpdatedChatObject(chat: ChatModel): ChatModel {
        val chatModel: ChatModel = getChatObject(chatId = chat.chatId)
        if (chatModel.type == BASE_MESSAGE_TYPE.Q) {
            val question: Question? = getQuestion(chat.chatId)
            if (question != null) {
                when {
                    question.material_type == BASE_MESSAGE_TYPE.IM ->
                        question.imageList =
                            getImagesOfQuestion(questionId = question.questionId)
                    question.material_type == BASE_MESSAGE_TYPE.VI -> question.videoList =

                        getVideosOfQuestion(questionId = question.questionId)
                    question.material_type == BASE_MESSAGE_TYPE.AU -> question.audioList =
                        getAudiosOfQuestion(questionId = question.questionId)

                    question.material_type == BASE_MESSAGE_TYPE.PD -> question.pdfList =
                        getPdfOfQuestion(questionId = question.questionId)
                }
                chatModel.question = question
            }
        }
        return chatModel

    }

    @Query(value = "UPDATE PdfTable SET total_view = :total_view where id= :id ")
    suspend fun updateTotalViewForPdf(id: String, total_view: Int)


}


enum class OPTION_TYPE(val type: String) {
    OPTION("O"), POLL("P")
}

enum class BASE_MESSAGE_TYPE(val type: String) {
    TX("TX"), VI("VI"), AU("AU"), IM("IM"), Q("Q"), PD("PD")

}


enum class MESSAGE_DELIVER_STATUS(val type: Int) {
    SENT(0), SENT_RECEIVED(1), READ(2)
}


enum class DOWNLOAD_STATUS {
    DOWNLOADED, DOWNLOADING, FAILED, NOT_START, UPLOADING, UPLOADED
}

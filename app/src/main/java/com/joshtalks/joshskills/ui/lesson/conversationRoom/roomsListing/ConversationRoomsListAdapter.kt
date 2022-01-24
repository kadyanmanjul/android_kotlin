package com.joshtalks.joshskills.ui.lesson.conversationRoom.roomsListing

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.DEFAULT_NAME
import com.joshtalks.joshskills.core.interfaces.ConversationRoomListAction
import com.joshtalks.joshskills.core.setUserImageRectOrInitials
import com.joshtalks.joshskills.databinding.LiConversionRoomsIlistingItemBinding
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.lesson.conversationRoom.model.RoomListResponseItem

class ConversationRoomsListAdapter(
    val action: ConversationRoomListAction,
) : RecyclerView.Adapter<ConversationRoomsListAdapter.ConversationRoomViewHolder>() {
    val listRooms: ArrayList<RoomListResponseItem> = arrayListOf()

    fun isRoomEmpty() = if (listRooms.isNullOrEmpty()) true else false

    fun clearAllRooms() {
        listRooms.clear()
        notifyDataSetChanged()
    }

    fun refreshList(newList: List<RoomListResponseItem>) {
        listRooms.clear()
        listRooms.addAll(newList)
        notifyDataSetChanged()
    }

    fun addItems(newList: List<RoomListResponseItem>) {
        Log.d("ABC", "addItems() called with: newList = $newList")
        val diffCallback = ConversationRoomDiffCallback(listRooms, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        listRooms.clear()
        listRooms.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun addSingleItem(newItem: RoomListResponseItem) {
       /* if (listRooms.isEmpty()){
            listRooms.add(newItem)
            notifyDataSetChanged()
            return
        }*/
        val newList: ArrayList<RoomListResponseItem> = ArrayList(listRooms)
        newList.add(newItem)
        Log.d("ABC", "addSingleItem() called with: newItem = $newList ${listRooms}")
        val diffCallback = ConversationRoomDiffCallback(listRooms, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        listRooms.clear()
        listRooms.addAll(newList)
        Log.d("ABC", "addSingleItem() called with: newList = $newList ${listRooms}")
        diffResult.dispatchUpdatesTo(this)
    }

    fun removeSingleItem(newItem: RoomListResponseItem) {
        Log.d("ABC", "removeSingleItem() called with: newItem = $newItem")
        val list = ArrayList(listRooms).filter { it.roomId == newItem.roomId }
        val newList: ArrayList<RoomListResponseItem> = ArrayList(listRooms)
        newList.removeAll(list)
        Log.d("ABC", "removeSingleItem() called with: newList = $newList listRooms = $listRooms")
        val diffCallback = ConversationRoomDiffCallback(listRooms, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        listRooms.clear()
        listRooms.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateItemWithoutPosition(room: RoomListResponseItem, isUserLeaving: Boolean) {
        Log.d(
            "ABC",
            "updateItemWithoutPosition() called with: room = $room, isUserLeaving = $isUserLeaving"
        )
        val newList: ArrayList<RoomListResponseItem> = ArrayList(listRooms)
        val isRoomPresent = newList.any { it.channelId == room.channelId }
        if(isRoomPresent){
            val roomFiltered = newList.filter { it.channelId == room.channelId }
            val roomToBeUpdated = roomFiltered[0]
            newList.remove(roomToBeUpdated)
            room.apply {
                roomToBeUpdated.audienceCount = this.audienceCount ?: roomToBeUpdated.audienceCount
                roomToBeUpdated.speakerCount = this.speakerCount ?: roomToBeUpdated.speakerCount
                when (isUserLeaving && this.liveRoomUserList.isNullOrEmpty().not()) {
                    true -> roomToBeUpdated.liveRoomUserList?.removeAll(this.liveRoomUserList!!)
                    false -> roomToBeUpdated.liveRoomUserList?.addAll(this.liveRoomUserList!!)
                }
            }
            newList.add(roomToBeUpdated)
            val diffCallback = ConversationRoomDiffCallback(listRooms, newList)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            listRooms.clear()
            listRooms.addAll(newList)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    fun updateItem(room: RoomListResponseItem, position: Int) {
        Log.d("ABC", "updateItem() called with: room = $room, position = $position")
        listRooms[position] = room
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationRoomViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiConversionRoomsIlistingItemBinding.inflate(inflater, parent, false)
        return ConversationRoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationRoomViewHolder, position: Int) {
        holder.bind(listRooms.get(position))
    }

    override fun getItemCount() =
        listRooms.size

    inner class ConversationRoomViewHolder(
        val binding: LiConversionRoomsIlistingItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        lateinit var roomItemAdapter: ConversationRoomItemAdapter

        fun bind(model: RoomListResponseItem) {

            with(binding) {
                roomItemAdapter = ConversationRoomItemAdapter()
                conversationTopicName.text = model.topic ?: ""

                speakersList.apply {
                    recycledViewPool.clear()
                    itemAnimator = null
                    adapter = roomItemAdapter
                    layoutManager =
                        LinearLayoutManager(binding.root.context, VERTICAL, false)
                    setHasFixedSize(false)
                }
                if (model.liveRoomUserList.isNullOrEmpty().not()) {
                    roomItemAdapter.addItems(model.liveRoomUserList!!)
                    if (model.liveRoomUserList?.size == 1) {
                        setProfilePhoto(
                            photo1,
                            model.liveRoomUserList!![0].photoUrl,
                            model.liveRoomUserList!![0].name
                        )
                        photo2.visibility = View.GONE
                    } else if (model.liveRoomUserList?.size!! > 1) {
                        setProfilePhoto(
                            photo1,
                            model.liveRoomUserList!![0].photoUrl,
                            model.liveRoomUserList!![0].name
                        )
                        setProfilePhoto(
                            photo2,
                            model.liveRoomUserList!![1].photoUrl,
                            model.liveRoomUserList!![1].name
                        )
                    }
                } else {
                    photo1.visibility = View.GONE
                    photo2.visibility = View.GONE
                }
                container.setOnSingleClickListener {
                    action.onRoomClick(model)
                }

                usersSize.text = "${model.audienceCount?.toInt()?.plus(model.speakerCount?.toInt()?:0) ?: 0}"
                speakerSize.text = "/ ${model.speakerCount?:0}"
            }
        }

        private fun setProfilePhoto(
            profile: ShapeableImageView,
            photoUrl: String?,
            userName: String?
        ) {
            profile.apply {
                clipToOutline = true
                visibility = View.VISIBLE
                setUserImageRectOrInitials(
                    url = photoUrl,
                    userName = userName ?: DEFAULT_NAME,
                    dpToPx = 24,
                    isRound = true,
                    radius = 16,
                    textColor = R.color.black,
                    bgColor = R.color.conversation_room_gray
                )
            }
        }

    }

}

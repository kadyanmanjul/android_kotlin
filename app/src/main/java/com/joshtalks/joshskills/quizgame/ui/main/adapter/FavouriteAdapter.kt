package com.joshtalks.joshskills.quizgame.ui.main.adapter

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieDrawable
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.CustomFavouriteBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.Favourite
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseTemp
import com.joshtalks.joshskills.quizgame.util.ACTIVE
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.IN_ACTIVE
import com.joshtalks.joshskills.quizgame.util.UtilsQuiz


class FavouriteAdapter(
    var context: Context, var arrayList: ArrayList<Favourite>?,
    private val openCourseListener: QuizBaseInterface,
    var firebaseDatabase: FirebaseTemp
) :
    RecyclerView.Adapter<FavouriteAdapter.FavViewHolder>() {

    var bindin: CustomFavouriteBinding? = null
    var pos: Int = 0
    var search: String? = null
    fun addItems(newList: ArrayList<Favourite>?) {
        if (newList!!.isEmpty()) {
            return
        }
        val diffResult: DiffUtil.DiffResult =
            DiffUtil.calculateDiff(FavouriteDiffCallback(newList, arrayList))
        diffResult.dispatchUpdatesTo(this)
        arrayList?.clear()
        arrayList?.addAll(newList)
    }

    fun updateListAfterSearch(list: ArrayList<Favourite>, searchString: String) {
        arrayList = list
        search = searchString
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavViewHolder {
        val binding =
            CustomFavouriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavViewHolder, position: Int) {
        arrayList?.get(position)?.let { holder.bind(it, position) }
        bindin = holder.binding
    }


    override fun getItemCount(): Int {
        return arrayList?.size!!
    }

    inner class FavViewHolder(val binding: CustomFavouriteBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(favouriteDemoData: Favourite?, position: Int) {
            binding.userImage.setUserImageOrInitials(
                favouriteDemoData?.image,
                favouriteDemoData?.name ?: "",
                30,
                isRound = true
            )

            val upperString = capitalizeString(favouriteDemoData?.name)
            binding.userName.text = UtilsQuiz.getSplitName(upperString)
            binding.status.text = favouriteDemoData?.status
            if (favouriteDemoData?.name?.lowercase()?.contains(search ?: "") == true) {
                val startPos: Int? = favouriteDemoData.name?.lowercase()?.indexOf(search ?: "")
                val endPos: Int? = startPos?.plus(search?.length ?: 0)
                val spanString: Spannable =
                    Spannable.Factory.getInstance().newSpannable(binding.userName.text)
                spanString.setSpan(
                    ForegroundColorSpan(Color.RED),
                    startPos ?: 0,
                    endPos ?: 0,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.userName.text = spanString
            }

            when (favouriteDemoData?.status) {
                ACTIVE -> {
                    binding.clickToken.setImageResource(R.drawable.ic_plus1)
                    binding.clickToken.setOnClickListener(View.OnClickListener {
                        AudioManagerQuiz.audioRecording.startPlaying(
                            context,
                            R.raw.tick_animation,
                            false
                        )
                        binding.clickToken.speed = 1.5F // How fast does the animation play
                        binding.clickToken.repeatCount = LottieDrawable.INFINITE
                        binding.clickToken.setAnimation("lottie/hourglass_anim.json")
                        binding.clickToken.playAnimation()
                        binding.clickToken.isEnabled = false
                        openCourseListener.onClickForGetToken(
                            arrayList?.get(position),
                            position.toString()
                        )
                    })
                }
                IN_ACTIVE -> {
                    binding.clickToken.visibility = View.INVISIBLE
                }
                else -> {
                    binding.clickToken.visibility = View.INVISIBLE
                }
            }
        }
    }

    fun getPositionById(mentorId: String): Int {
        for (v in 0 until arrayList?.size!!) {
            if (arrayList?.get(v)?.uuid == mentorId) {
                pos = v
            }
        }
        return pos
    }

    fun capitalizeString(str: String?): String {
        var retStr = str
        try {
            retStr = str?.substring(0, 1)?.toUpperCase() + str?.substring(1)
        } catch (e: Exception) {
        }
        return retStr ?: ""
    }

    interface QuizBaseInterface {
        fun onClickForGetToken(favourite: Favourite?, position: String)
    }

}
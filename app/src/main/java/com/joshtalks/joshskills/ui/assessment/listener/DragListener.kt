package com.joshtalks.joshskills.ui.assessment.listener

import android.view.DragEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.ui.assessment.adapter.MatchTheFollowingChoiceAdapter

class DragListener(private val listener: EmptyListListener) : View.OnDragListener {
    private var isDropped = false
    override fun onDrag(
        v: View,
        event: DragEvent
    ): Boolean {
        when (event.action) {
            DragEvent.ACTION_DROP -> {
                isDropped = true
                var positionTarget = -1
                val viewSource = event.localState as View
                val viewId = v.id
                val flItem = R.id.root_view
                val rvLeft = R.id.table_a_choices
                val rvRight = R.id.table_b_choices
                try {
                    when (viewId) {
                        flItem, rvLeft, rvRight -> {
                            val target: RecyclerView
                            when (viewId) {
                                rvLeft -> target = v.rootView.findViewById(rvLeft)
                                rvRight -> target = v.rootView.findViewById(rvRight)
                                else -> {
                                    target = v.parent as RecyclerView
                                    positionTarget = v.tag as Int
                                }
                            }
                            if (viewSource != null) {

                                val positionSource = viewSource.tag as Int
                                val source = viewSource.parent as RecyclerView

                                val adapterSource =
                                    source.adapter as MatchTheFollowingChoiceAdapter?
                                val sourceId = source.id
                                val listSource = ArrayList<Choice>(
                                    adapterSource!!.getList().sortedBy { it.sortOrder })

                                val adapterTarget =
                                    target.adapter as MatchTheFollowingChoiceAdapter?
                                val customListTarget = ArrayList<Choice>(
                                    adapterTarget!!.getList().sortedBy { it.sortOrder })
                                if (positionSource < 0 && positionTarget < 0) {
                                    return false
                                } else if (customListTarget == listSource) {
                                    return false
                                } else if (customListTarget[positionTarget].isSelectedByUser &&
                                    source == v.rootView.findViewById(rvRight)
                                    && target == v.rootView.findViewById(rvLeft)
                                    && listSource[positionSource].isSelectedByUser
                                    && customListTarget[positionTarget].userSelectedOrder == positionSource
                                ) {
                                    listSource[positionSource].isSelectedByUser = false
                                    listSource[positionSource].userSelectedOrder = 100
                                    customListTarget[positionTarget].isSelectedByUser = false
                                    customListTarget[positionTarget].userSelectedOrder = 100

                                    adapterSource.updateList(listSource)
                                    adapterSource.notifyDataSetChanged()

                                    adapterTarget.updateList(customListTarget)
                                    adapterTarget.notifyDataSetChanged()
                                    listener.setEmptyLeftList(false)
                                    return false
                                } else if (customListTarget[positionTarget].isSelectedByUser
                                    && target == v.rootView.findViewById(rvRight)
                                    && listSource[positionSource].isSelectedByUser.not()
                                ) {
                                    //listener.setEmptyLeftList(false)
                                    return false
                                } else if (source == v.rootView.findViewById(rvRight)) {
                                    //listener.setEmptyLeftList(false)
                                    return false
                                } else if (source == v.rootView.findViewById(rvLeft) &&
                                    listSource[positionSource].isSelectedByUser
                                ) {
                                    //listener.setEmptyLeftList(false)
                                    return false
                                }

                                listSource[positionSource].isSelectedByUser = true
                                listSource[positionSource].userSelectedOrder = positionTarget

                                adapterSource.updateList(listSource)
                                adapterSource.notifyDataSetChanged()


                                if (positionTarget >= 0) {
                                    customListTarget[positionTarget].isSelectedByUser = true
                                    customListTarget[positionTarget].userSelectedOrder =
                                        positionSource
                                } else {

                                }

                                adapterTarget.updateList(customListTarget)
                                adapterTarget.notifyDataSetChanged()

                                adapterSource.let {

                                    if (sourceId == rvLeft) {
                                        val item = adapterSource.getList()
                                            .filter { it.isSelectedByUser == false }
                                        if (item.size < 1)
                                            listener.setEmptyLeftList(true)
                                    }
                                }

                            }
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
        if (!isDropped && event.localState != null) {
            (event.localState as View).visibility = View.VISIBLE
        }
        return true
    }

}

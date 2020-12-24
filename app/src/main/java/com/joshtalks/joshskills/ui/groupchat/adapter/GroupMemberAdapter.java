package com.joshtalks.joshskills.ui.groupchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.pro.constants.CometChatConstants;
import com.cometchat.pro.core.CometChat;
import com.cometchat.pro.models.GroupMember;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.databinding.UserListRowBinding;
import com.joshtalks.joshskills.ui.groupchat.utils.FontUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Purpose - GroupMemberAdapter is a subclass of RecyclerView Adapter which is used to display
 * the list of group members. It helps to organize the list data in recyclerView.
 * It also help to perform search operation on list of groups members.
 * <p>
 * Created on - 20th December 2019
 * <p>
 * Modified on  - 24th January 2020
 */


public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.GroupMemberViewHolder> {

    private static final String TAG = GroupMemberAdapter.class.getSimpleName();
    private final Context context;
    private final FontUtils fontUtils;
    private String groupOwnerId;
    private List<GroupMember> groupMemberList = new ArrayList<>();

    /**
     * It is a constructor which is used to initialize wherever we needed.
     *
     * @param context is a object of Context.
     */
    public GroupMemberAdapter(Context context) {
        this.context = context;
        fontUtils = FontUtils.getInstance(context);
    }

    /**
     * It is constructor which takes groupMemberList as parameter and bind it with groupMemberList in adapter.
     *
     * @param context         is a object of Context.
     * @param groupMemberList is a list of group member used in this adapter.
     */
    public GroupMemberAdapter(Context context, List<GroupMember> groupMemberList, String groupOwnerId) {
        this.groupMemberList = groupMemberList;
        this.groupOwnerId = groupOwnerId;
        this.context = context;
        fontUtils = FontUtils.getInstance(context);
        sortMemberList();
    }

    @NonNull
    @Override
    public GroupMemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {

        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        UserListRowBinding userListRowBinding = DataBindingUtil.inflate(layoutInflater, R.layout.user_list_row, parent, false);

        return new GroupMemberViewHolder(userListRowBinding);
    }

    /**
     * This method is used to bind the GroupMemberViewHolder contents with groupMember at given
     * position. It set name icon, scope with respective GroupMemberViewHolder content.
     *
     * @param groupMemberViewHolder is a object of GroupMemberViewHolder.
     * @param i                     is a position of item in recyclerView.
     * @see GroupMember
     */
    @Override
    public void onBindViewHolder(@NonNull GroupMemberViewHolder groupMemberViewHolder, int i) {

        GroupMember groupMember = groupMemberList.get(i);

        groupMemberViewHolder.userListRowBinding.avUser.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
        groupMemberViewHolder.userListRowBinding.getRoot().setTag(R.string.user, groupMember);


        if (groupMember.getUid().equals(CometChat.getLoggedInUser().getUid())) {
            groupMemberViewHolder.userListRowBinding.txtUserName.setText(R.string.you);
        } else
            groupMemberViewHolder.userListRowBinding.txtUserName.setText(groupMember.getName());

        if (groupOwnerId != null && groupMember.getUid().equals(groupOwnerId) &&
                groupMember.getScope().equals(CometChatConstants.SCOPE_ADMIN)) {
            groupMemberViewHolder.userListRowBinding.txtUserScope.setText(R.string.owner);
        } else if (groupMember.getScope().equals(CometChatConstants.SCOPE_ADMIN)) {
            groupMemberViewHolder.userListRowBinding.txtUserScope.setText(R.string.admin);
        } else if (groupMember.getScope().equals(CometChatConstants.SCOPE_MODERATOR)) {
            groupMemberViewHolder.userListRowBinding.txtUserScope.setText(R.string.moderator);
        } else {
            groupMemberViewHolder.userListRowBinding.txtUserScope.setText("");
        }

        groupMemberViewHolder.userListRowBinding.txtUserName.setTypeface(fontUtils.getTypeFace(FontUtils.robotoRegular));
        String colorCode = null;
        try {
            if (groupMember.getMetadata() != null && groupMember.getMetadata().has("color_code")) {
                colorCode = groupMember.getMetadata().getString("color_code");
            }
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        if (groupMember.getAvatar() == null || groupMember.getAvatar().isEmpty())
            groupMemberViewHolder.userListRowBinding.avUser.setInitials(groupMember.getName(), colorCode);
        else
            groupMemberViewHolder.userListRowBinding.avUser.setAvatar(groupMember.getAvatar());

        if (i == getItemCount() - 1)
            groupMemberViewHolder.userListRowBinding.tvSeprator.setVisibility(View.GONE);


    }

    @Override
    public int getItemCount() {
        return groupMemberList.size();

    }

    /**
     * This method is used to add group members in groupMemberList of adapter
     *
     * @param groupMembers is a list of group members which will be added in adapter.
     * @see GroupMember
     */
    public void addAll(List<GroupMember> groupMembers) {

        for (GroupMember groupMember : groupMembers) {
            if (!groupMemberList.contains(groupMember)) {
                groupMemberList.add(groupMember);
                sortMemberList();
            }
        }
        notifyDataSetChanged();
    }

    /**
     * This method is used to set search list in a groupMemberList of adapter.
     *
     * @param filterlist is a list of searched group members.
     */
    public void searchGroupMembers(List<GroupMember> filterlist) {
        this.groupMemberList = filterlist;
        sortMemberList();
        notifyDataSetChanged();
    }

    /**
     * This method is used to add group member in a groupMemberList of adapter.
     *
     * @param joinedUser is object of GroupMember which will be added in a groupList.
     * @see GroupMember
     */
    public void addGroupMember(GroupMember joinedUser) {
        groupMemberList.add(joinedUser);
        sortMemberList();
        notifyDataSetChanged();
    }

    /**
     * This method is used to remove group member from groupMemberList of adapter.
     *
     * @param groupMember is a object of GroupMember which will be removed from groupList.
     * @see GroupMember
     */
    public void removeGroupMember(GroupMember groupMember) {
        if (groupMemberList.contains(groupMember)) {
            groupMemberList.remove(groupMember);
            notifyDataSetChanged();
        }
    }

    /**
     * This method is used to update group member from a groupMemberList of a adapter.
     *
     * @param groupMember is a object of GroupMember which will updated with old group member in
     *                    groupMemberList.
     * @see GroupMember
     */
    public void updateMember(GroupMember groupMember) {
        if (groupMemberList.contains(groupMember)) {
            int index = groupMemberList.indexOf(groupMember);
            groupMemberList.remove(groupMember);
            groupMemberList.add(index, groupMember);
            sortMemberList();
            notifyItemChanged(index);
        }
    }

    public void resetAdapter() {
        groupMemberList.clear();
        notifyDataSetChanged();
    }

    /**
     * This method is used to update group members in a groupMemberList of a adapter.
     *
     * @param groupMembers is a list of updated group members.
     */
    public void updateGroupMembers(List<GroupMember> groupMembers) {
        for (int i = 0; i < groupMembers.size(); i++) {
            if (groupMemberList.contains(groupMembers.get(i))) {
                int index = groupMemberList.indexOf(groupMembers.get(i));
                groupMemberList.remove(index);
                groupMemberList.add(index, groupMembers.get(i));
            } else {
                groupMemberList.add(groupMembers.get(i));
            }
        }
        sortMemberList();
        notifyDataSetChanged();
    }

    private void sortMemberList() {
        Collections.sort(this.groupMemberList, (member1, member2) -> member1.getName().toUpperCase().compareTo(member2.getName().toUpperCase()));
        GroupMember member = null;
        for (int i = 0; i < this.groupMemberList.size(); i++) {
            member = groupMemberList.get(i);
            if (member.getUid().equals(CometChat.getLoggedInUser().getUid())) {
                groupMemberList.remove(member);
                groupMemberList.add(0, member);
                break;
            }
        }
    }

    class GroupMemberViewHolder extends RecyclerView.ViewHolder {

        UserListRowBinding userListRowBinding;

        GroupMemberViewHolder(UserListRowBinding userListRowBinding) {
            super(userListRowBinding.getRoot());
            this.userListRowBinding = userListRowBinding;

        }
    }
}

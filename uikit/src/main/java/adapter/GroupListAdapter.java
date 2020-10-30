package adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.pro.constants.CometChatConstants;
import com.cometchat.pro.models.Group;
import com.cometchat.pro.uikit.R;
import com.cometchat.pro.uikit.databinding.GroupListRowBinding;

import java.util.ArrayList;
import java.util.List;

import utils.FontUtils;
import utils.Utils;

/**
 * Purpose - GroupListAdapter is a subclass of RecyclerView Adapter which is used to display
 * the list of groups. It helps to organize the list data in recyclerView.
 * <p>
 * Created on - 20th December 2019
 * <p>
 * Modified on  - 23rd March 2020
 */


public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupViewHolder> {

    private final Context context;
    private final FontUtils fontUtils;
    private List<Group> groupList = new ArrayList<>();

    /**
     * It is a constructor which is used to initialize wherever we needed.
     *
     * @param context is a object of Context.
     */
    public GroupListAdapter(Context context) {
        this.context = context;
        fontUtils = FontUtils.getInstance(context);
    }

    /**
     * It is constructor which takes groupsList as parameter and bind it with groupList in adapter.
     *
     * @param context   is a object of Context.
     * @param groupList is a list of groups used in this adapter.
     */
    public GroupListAdapter(Context context, List<Group> groupList) {

        this.groupList = groupList;
        this.context = context;
        fontUtils = FontUtils.getInstance(context);
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        GroupListRowBinding groupListRowBinding = DataBindingUtil.inflate(layoutInflater, R.layout.group_list_row, parent, false);
        return new GroupViewHolder(groupListRowBinding);
    }

    /**
     * This method is used to bind the GroupViewHolder contents with group at given
     * position. It set group icon, group name in a respective GroupViewHolder content.
     *
     * @param groupViewHolder is a object of GroupViewHolder.
     * @param position        is a position of item in recyclerView.
     * @see Group
     */
    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder groupViewHolder, int position) {
        Group group = groupList.get(position);
        groupViewHolder.groupListRowBinding.setGroup(group);
        groupViewHolder.groupListRowBinding.executePendingBindings();
        groupViewHolder.groupListRowBinding.txtUserMessage.setText("Members: " + group.getMembersCount());

        if (group.getGroupType().equals(CometChatConstants.GROUP_TYPE_PRIVATE))
            groupViewHolder.groupListRowBinding.txtUserName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_security_24dp, 0);
        else if (group.getGroupType().equals(CometChatConstants.GROUP_TYPE_PASSWORD))
            groupViewHolder.groupListRowBinding.txtUserName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_lock_24dp, 0);
        else
            groupViewHolder.groupListRowBinding.txtUserName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

        groupViewHolder.groupListRowBinding.executePendingBindings();
        groupViewHolder.groupListRowBinding.avGroup.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
        groupViewHolder.groupListRowBinding.getRoot().setTag(R.string.group, group);
        groupViewHolder.groupListRowBinding.txtUserMessage.setTypeface(fontUtils.getTypeFace(FontUtils.robotoRegular));
        groupViewHolder.groupListRowBinding.txtUserName.setTypeface(fontUtils.getTypeFace(FontUtils.robotoMedium));

        if (Utils.isDarkMode(context)) {
            TextViewCompat.setCompoundDrawableTintList(groupViewHolder.groupListRowBinding.txtUserName, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey)));
            groupViewHolder.groupListRowBinding.txtUserName.setTextColor(ContextCompat.getColor(context, R.color.textColorWhite));
            groupViewHolder.groupListRowBinding.tvSeprator.setBackgroundColor(ContextCompat.getColor(context, R.color.grey));
        } else {
            TextViewCompat.setCompoundDrawableTintList(groupViewHolder.groupListRowBinding.txtUserName, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.message_bubble_grey)));
            groupViewHolder.groupListRowBinding.txtUserName.setTextColor(ContextCompat.getColor(context, R.color.primaryTextColor));
            groupViewHolder.groupListRowBinding.tvSeprator.setBackgroundColor(ContextCompat.getColor(context, R.color.light_grey));
        }
    }

    /**
     * This method is used to update groupList in adapter.
     *
     * @param groupList is a list of groups which will be updated in adapter.
     */
    public void updateGroupList(List<Group> groupList) {

        for (int i = 0; i < groupList.size(); i++) {
            updateGroup(groupList.get(i));
        }
    }

    /**
     * This method is used to update a particular group in groupList of adapter.
     *
     * @param group is an object of Group. It will be updated with previous group in a list.
     */
    public void updateGroup(Group group) {
        if (group != null) {
            if (groupList.contains(group)) {
                int index = groupList.indexOf(group);
                groupList.remove(index);
                groupList.add(index, group);
                notifyItemChanged(index);
            } else {
                groupList.add(group);
                notifyItemInserted(getItemCount() - 1);
            }
        }
    }

    /**
     * This method is used to remove particular group from groupList in adapter.
     *
     * @param group is a object of Group which will be removed from groupList.
     * @see Group
     */
    public void removeGroup(Group group) {
        if (group != null) {
            if (groupList.contains(group)) {
                int index = groupList.indexOf(group);
                groupList.remove(group);
                notifyItemRemoved(index);
            }
        }
    }

    @Override
    public int getItemCount() {
        return groupList.size();

    }

    /**
     * This method is used to set searchGroupList with a groupList in adapter.
     *
     * @param groups is a list of group which will be set with a groupList in adapter.
     */
    public void searchGroup(List<Group> groups) {
        if (groups != null) {
            groupList = groups;
            notifyDataSetChanged();
        }

    }

    /**
     * This method is used to add particular group in groupList of adapter.
     *
     * @param group is a object of group which will be added in groupList.
     * @see Group
     */
    public void add(Group group) {
        if (group != null) {
            groupList.add(group);
            notifyItemInserted(getItemCount() - 1);
        }
    }

    public void clear() {
        groupList.clear();
        notifyDataSetChanged();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {

        GroupListRowBinding groupListRowBinding;

        GroupViewHolder(@NonNull GroupListRowBinding groupListRowBinding) {
            super(groupListRowBinding.getRoot());
            this.groupListRowBinding = groupListRowBinding;
        }
    }
}

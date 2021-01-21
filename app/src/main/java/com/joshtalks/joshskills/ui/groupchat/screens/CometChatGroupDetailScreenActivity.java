package com.joshtalks.joshskills.ui.groupchat.screens;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.pro.constants.CometChatConstants;
import com.cometchat.pro.core.BannedGroupMembersRequest;
import com.cometchat.pro.core.CometChat;
import com.cometchat.pro.core.GroupMembersRequest;
import com.cometchat.pro.exceptions.CometChatException;
import com.cometchat.pro.models.Action;
import com.cometchat.pro.models.Group;
import com.cometchat.pro.models.GroupMember;
import com.cometchat.pro.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.core.BaseActivity;
import com.joshtalks.joshskills.core.PrefManager;
import com.joshtalks.joshskills.ui.groupchat.adapter.GroupMemberAdapter;
import com.joshtalks.joshskills.ui.groupchat.constant.StringContract;
import com.joshtalks.joshskills.ui.groupchat.listeners.ClickListener;
import com.joshtalks.joshskills.ui.groupchat.listeners.RecyclerTouchListener;
import com.joshtalks.joshskills.ui.groupchat.utils.FontUtils;
import com.joshtalks.joshskills.ui.groupchat.utils.Utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.joshtalks.joshskills.core.PrefManagerKt.IS_GROUP_NOTIFICATION_MUTED;
import static com.joshtalks.joshskills.ui.groupchat.utils.Utils.UserToGroupMember;


public class CometChatGroupDetailScreenActivity extends BaseActivity {


    private static final int LIMIT = 30;
    private final String TAG = "CometChatGroupDetail";
    private final ArrayList<String> groupMemberUids = new ArrayList<>();
    //private final List<GroupMember> groupMembers = new ArrayList<>();
    private final User loggedInUser = CometChat.getLoggedInUser();
    String[] s = new String[0];
    // private Avatar groupIcon;
    private AppCompatImageView groupImage;
    private String groupType;
    private String ownerId;
    // private TextView tvGroupName;
    private TextView tvGroupDesc;
    //private TextView tvAdminCount;
    //private TextView tvModeratorCount;
    //private TextView tvBanMemberCount;
    private RecyclerView rvMemberList;
    private String guid;
    private String gName;
    private String gDesc;
    private String gPassword;
    private GroupMembersRequest groupMembersRequest;
    private GroupMemberAdapter groupMemberAdapter;
    private int adminCount;
    private int moderatorCount;
    private RelativeLayout rlAddMemberView;
    private RelativeLayout rlAdminListView;
    private RelativeLayout rlModeratorView;
    private RelativeLayout rlBanMembers;
    private String loggedInUserScope;
    private GroupMember groupMember;
    private TextView tvDelete;
    //private TextView tvLoadMore;
    //private AlertDialog.Builder dialog;
    private TextView tvMemberCount;
    private int groupMemberCount = 0;
    private FontUtils fontUtils;
    private boolean isNoMoreMembers;
    private boolean isInProgress;

    private ImageView videoCallBtn;

    private ImageView callBtn;

    private TextView dividerAdmin, dividerBan, dividerModerator, divider2;

    private BannedGroupMembersRequest banMemberRequest;

    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comet_chat_group_detail_screen);
        fontUtils = FontUtils.getInstance(this);
        initComponent();

    }

    private void initComponent() {

        dividerAdmin = findViewById(R.id.tv_seperator_admin);
        dividerModerator = findViewById(R.id.tv_seperator_moderator);
        dividerBan = findViewById(R.id.tv_seperator_ban);
        divider2 = findViewById(R.id.tv_seperator_1);
        // groupIcon = findViewById(R.id.iv_group);
        // tvGroupName = findViewById(R.id.tv_group_name);
        groupImage = findViewById(R.id.groupImage);
        tvGroupDesc = findViewById(R.id.group_description);
        // tvGroupName.setOnClickListener(v -> updateGroupDialog());
        tvMemberCount = findViewById(R.id.tv_members);
//        tvAdminCount = findViewById(R.id.tv_admin_count);
//        tvModeratorCount = findViewById(R.id.tv_moderator_count);
//        tvBanMemberCount = findViewById(R.id.tv_ban_count);
        rvMemberList = findViewById(R.id.member_list);
//        tvLoadMore = findViewById(R.id.tv_load_more);
//        tvLoadMore.setText(String.format(getResources().getString(R.string.load_more_members, ""), ""));
        TextView tvAddMember = findViewById(R.id.tv_add_member);
        callBtn = findViewById(R.id.callBtn_iv);
        videoCallBtn = findViewById(R.id.video_callBtn_iv);
        rlBanMembers = findViewById(R.id.rlBanView);
        rlBanMembers.setOnClickListener(v -> openBanMemberListScreen());
        rlAddMemberView = findViewById(R.id.rl_add_member);
        rlAddMemberView.setOnClickListener(v -> addMembers());
        rlAdminListView = findViewById(R.id.rlAdminView);
        rlAdminListView.setOnClickListener(v -> openAdminListScreen(false));
        rlModeratorView = findViewById(R.id.rlModeratorView);
        rlModeratorView.setOnClickListener(v -> openAdminListScreen(true));
        tvDelete = findViewById(R.id.tv_delete);
        TextView tvExit = findViewById(R.id.tv_exit);
        toolbar = findViewById(R.id.groupDetailToolbar);
        SwitchMaterial notificationSwitch = findViewById(R.id.notif_switch);
        TextView notificationStatusTv = findViewById(R.id.notification_status_tv);
        tvDelete.setTypeface(fontUtils.getTypeFace(FontUtils.robotoMedium));
        tvExit.setTypeface(fontUtils.getTypeFace(FontUtils.robotoMedium));
        tvAddMember.setTypeface(fontUtils.getTypeFace(FontUtils.robotoRegular));

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvMemberList.setLayoutManager(linearLayoutManager);
//        rvMemberList.setNestedScrollingEnabled(false);

        boolean isNotificationMuted = PrefManager.getBoolValue(IS_GROUP_NOTIFICATION_MUTED, false, false);
        notificationSwitch.setChecked(!isNotificationMuted);
        if (isNotificationMuted) {
            notificationStatusTv.setText(getString(R.string.off));
        } else {
            notificationStatusTv.setText(getString(R.string.on));

        }

        handleIntent();
        checkDarkMode();
        rvMemberList.addOnItemTouchListener(new RecyclerTouchListener(this, rvMemberList, new ClickListener() {
            @Override
            public void onClick(View var1, int var2) {
                GroupMember user = (GroupMember) var1.getTag(R.string.user);
                /*if (loggedInUserScope != null && (loggedInUserScope.equals(CometChatConstants.SCOPE_ADMIN) || loggedInUserScope.equals(CometChatConstants.SCOPE_MODERATOR))) {
                    groupMember = user;
                    boolean isAdmin = user.getScope().equals(CometChatConstants.SCOPE_ADMIN);
                    boolean isSelf = loggedInUser.getUid().equals(user.getUid());
                    boolean isOwner = loggedInUser.getUid().equals(ownerId);
                    if (!isSelf) {
                        if (!isAdmin || isOwner) {
                            registerForContextMenu(rvMemberList);
                            openContextMenu(var1);
                        }
                    }
                }*/
                Utils.moveToUserProfile(user.getUid(), CometChatGroupDetailScreenActivity.this);
                //finish();
            }

            @Override
            public void onLongClick(View var1, int var2) {

            }
        }));

        rvMemberList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!isNoMoreMembers && !isInProgress) {
                    if (linearLayoutManager.findFirstVisibleItemPosition() == 10 || !rvMemberList.canScrollVertically(1)) {
                        isInProgress = true;
                        getGroupMembers();
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

//        tvLoadMore.setOnClickListener(view -> getGroupMembers());
        tvExit.setOnClickListener(view -> createDialog(getResources().getString(R.string.exit_group_title), getResources().getString(R.string.exit_group_message),
                getResources().getString(R.string.exit), getResources().getString(R.string.cancel), R.drawable.ic_exit_to_app));

        tvDelete.setOnClickListener(view -> createDialog(getResources().getString(R.string.delete_group_title), getResources().getString(R.string.delete_group_message),
                getResources().getString(R.string.delete), getResources().getString(R.string.cancel), R.drawable.ic_delete));

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PrefManager.INSTANCE.put(IS_GROUP_NOTIFICATION_MUTED, !isChecked, false);
            if (isChecked) {
                notificationStatusTv.setText(getString(R.string.on));
            } else {
                notificationStatusTv.setText(getString(R.string.off));
            }
        });

    }

    private void checkDarkMode() {
//        if (Utils.isDarkMode(this)) {
//            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.textColorWhite));
//            tvGroupName.setTextColor(ContextCompat.getColor(this, R.color.textColorWhite));
//            dividerAdmin.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));
//            dividerModerator.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));
//            dividerBan.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));
//            divider2.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));
//        } else {
//            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.textColorWhite));
//            tvGroupName.setTextColor(ContextCompat.getColor(this, R.color.primaryTextColor));
//            dividerAdmin.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey));
//            dividerModerator.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey));
//            dividerBan.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey));
//            divider2.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey));
//        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.group_action_menu, menu);

        menu.findItem(R.id.item_make_admin).setVisible(false);

        menu.setHeaderTitle("Group Action");
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {


        if (item.getItemId() == R.id.item_remove) {
            kickMember();
        } else if (item.getItemId() == R.id.item_ban) {
            banMember();
        }

        return super.onContextItemSelected(item);
    }


    /**
     * This method is used to create dialog box on click of events like <b>Delete Group</b> and <b>Exit Group</b>
     *
     * @param title
     * @param message
     * @param positiveText
     * @param negativeText
     * @param drawableRes
     */
    private void createDialog(String title, String message, String positiveText, String negativeText, int drawableRes) {

        MaterialAlertDialogBuilder alert_dialog = new MaterialAlertDialogBuilder(CometChatGroupDetailScreenActivity.this,
                R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
        alert_dialog.setTitle(title);
        alert_dialog.setMessage(message);
        alert_dialog.setPositiveButton(positiveText, (dialogInterface, i) -> {

            if (positiveText.equalsIgnoreCase(getResources().getString(R.string.exit)))
                leaveGroup();

            else if (positiveText.equalsIgnoreCase(getResources().getString(R.string.delete))
                    && loggedInUserScope.equalsIgnoreCase(CometChatConstants.SCOPE_ADMIN))
                deleteGroup();

        });

        alert_dialog.setNegativeButton(negativeText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alert_dialog.create();
        alert_dialog.show();

    }


    /**
     * This method is used to handle the intent passed to this activity.
     */
    private void handleIntent() {
        if (getIntent().hasExtra(StringContract.IntentStrings.GUID)) {
            guid = getIntent().getStringExtra(StringContract.IntentStrings.GUID);
        }
        if (getIntent().hasExtra(StringContract.IntentStrings.MEMBER_SCOPE)) {
            loggedInUserScope = getIntent().getStringExtra(StringContract.IntentStrings.MEMBER_SCOPE);
            if (loggedInUserScope != null && loggedInUserScope.equals(CometChatConstants.SCOPE_ADMIN)) {
                // rlAddMemberView.setVisibility(View.VISIBLE);
                rlBanMembers.setVisibility(View.VISIBLE);
                rlModeratorView.setVisibility(View.VISIBLE);
                // tvDelete.setVisibility(View.VISIBLE);
            } else if (loggedInUserScope != null && loggedInUserScope.equals(CometChatConstants.SCOPE_MODERATOR)) {
                rlAddMemberView.setVisibility(View.GONE);
                rlBanMembers.setVisibility(View.VISIBLE);
                rlModeratorView.setVisibility(View.VISIBLE);
                rlAdminListView.setVisibility(View.VISIBLE);
            } else {
                dividerModerator.setVisibility(View.GONE);
                dividerAdmin.setVisibility(View.GONE);
                rlAdminListView.setVisibility(View.GONE);
                rlModeratorView.setVisibility(View.GONE);
                rlBanMembers.setVisibility(View.GONE);
                rlAddMemberView.setVisibility(View.GONE);
            }

        }
        if (getIntent().hasExtra(StringContract.IntentStrings.NAME)) {
            gName = getIntent().getStringExtra(StringContract.IntentStrings.NAME);
            // tvGroupName.setText(gName);
            toolbar.setTitle(gName);
        }
        if (getIntent().hasExtra(StringContract.IntentStrings.AVATAR)) {
            String avatar = getIntent().getStringExtra(StringContract.IntentStrings.AVATAR);
            if (avatar != null && !avatar.isEmpty()) {
                // groupIcon.setAvatar(avatar);
                // groupImage.setImageURI(Uri.parse(avatar));
                try {
                    URL url = new URL(avatar);
                    Bitmap bmImg = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    BitmapDrawable background = new BitmapDrawable(bmImg);
                    groupImage.setBackgroundDrawable(background);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // groupIcon.setInitials(gName);
            }
        }
        if (getIntent().hasExtra(StringContract.IntentStrings.GROUP_DESC)) {
            gDesc = getIntent().getStringExtra(StringContract.IntentStrings.GROUP_DESC);
            if (gDesc != null && !gDesc.isEmpty()) {
                tvGroupDesc.setText(gDesc);
            }
        }
        if (getIntent().hasExtra(StringContract.IntentStrings.GROUP_PASSWORD)) {
            gPassword = getIntent().getStringExtra(StringContract.IntentStrings.GROUP_PASSWORD);
        }
        if (getIntent().hasExtra(StringContract.IntentStrings.GROUP_OWNER)) {
            ownerId = getIntent().getStringExtra(StringContract.IntentStrings.GROUP_OWNER);
        }
        if (getIntent().hasExtra(StringContract.IntentStrings.MEMBER_COUNT)) {
            tvMemberCount.setVisibility(View.VISIBLE);
            groupMemberCount = getIntent().getIntExtra(StringContract.IntentStrings.MEMBER_COUNT, 0);
            tvMemberCount.setText((groupMemberCount) + " Members");
        }
        if (getIntent().hasExtra(StringContract.IntentStrings.GROUP_TYPE)) {
            groupType = getIntent().getStringExtra(StringContract.IntentStrings.GROUP_TYPE);
        }
    }


    /**
     * This method is used whenever user click <b>Banned Members</b>. It takes user to
     * <code>CometChatBanMemberScreenActivity.class</code>
     *
     * @see
     */
    private void openBanMemberListScreen() {

    }


    /**
     * This method is used whenever user click <b>Administrator</b>. It takes user to
     * <code>CometChatAdminListScreenActivity.class</code>
     *
     * @see
     */
    public void openAdminListScreen(boolean showModerators) {
    /*    Intent intent = new Intent(this, CometChatAdminModeratorListScreenActivity.class);
        intent.putExtra(StringContract.IntentStrings.GUID, guid);
        intent.putExtra(StringContract.IntentStrings.SHOW_MODERATORLIST, showModerators);
        intent.putExtra(StringContract.IntentStrings.GROUP_OWNER, ownerId);
        intent.putExtra(StringContract.IntentStrings.MEMBER_SCOPE, loggedInUserScope);
        startActivity(intent);*/
    }

    /**
     * This method is used whenever user click <b>Add Member</b>. It takes user to
     * <code>CometChatAddMemberScreenActivity.class</code>
     *
     * @see
     */
    public void addMembers() {
       /* Intent intent = new Intent(this, CometChatAddMemberScreenActivity.class);
        intent.putExtra(StringContract.IntentStrings.GUID, guid);
        intent.putExtra(StringContract.IntentStrings.GROUP_MEMBER, groupMemberUids);
        intent.putExtra(StringContract.IntentStrings.GROUP_NAME, gName);
        intent.putExtra(StringContract.IntentStrings.MEMBER_SCOPE, loggedInUserScope);
        intent.putExtra(StringContract.IntentStrings.IS_ADD_MEMBER, true);
        startActivity(intent);*/
    }

    /**
     * This method is used to delete Group. It is used only if loggedIn user is admin.
     */
    private void deleteGroup() {
        CometChat.deleteGroup(guid, new CometChat.CallbackListener<String>() {
            @Override
            public void onSuccess(String s) {
                launchUnified();
            }

            @Override
            public void onError(CometChatException e) {
                Snackbar.make(rvMemberList, getResources().getString(R.string.group_delete_error), Snackbar.LENGTH_SHORT).show();
                Log.e(TAG, "onError: " + e.getMessage());
            }
        });
    }

    private void launchUnified() {
      /*  Intent intent = new Intent(CometChatGroupDetailScreenActivity.this, CometChatUnified.class);
        startActivity(intent);
        finish();*/
    }


    /**
     * This method is used to kick group member from the group. It is used only if loggedIn user is admin.
     *
     * @see CometChat#kickGroupMember(String, String, CometChat.CallbackListener)
     */
    private void kickMember() {
        CometChat.kickGroupMember(groupMember.getUid(), guid, new CometChat.CallbackListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.e(TAG, "onSuccess: " + s);
                tvMemberCount.setText((--groupMemberCount) + " Members");
                groupMemberUids.remove(groupMember.getUid());
                groupMemberAdapter.removeGroupMember(groupMember);
            }

            @Override
            public void onError(CometChatException e) {
                Snackbar.make(rvMemberList, String.format(getResources().getString(R.string.cannot_remove_member), groupMember.getName()), Snackbar.LENGTH_SHORT).show();
                Log.e(TAG, "onError: " + e.getMessage());
            }
        });
    }

    /**
     * This method is used to ban group member from the group. It is used only if loggedIn user is admin.
     *
     * @see CometChat#banGroupMember(String, String, CometChat.CallbackListener)
     */
    private void banMember() {
        CometChat.banGroupMember(groupMember.getUid(), guid, new CometChat.CallbackListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.e(TAG, "onSuccess: " + s);
                tvMemberCount.setText((--groupMemberCount) + " Members");
//                int count = Integer.parseInt(tvBanMemberCount.getText().toString());
//                tvBanMemberCount.setText(String.valueOf(++count));
                groupMemberUids.remove(groupMember.getUid());
                groupMemberAdapter.removeGroupMember(groupMember);
            }

            @Override
            public void onError(CometChatException e) {
                Snackbar.make(rvMemberList, String.format(getResources().getString(R.string.cannot_remove_member), groupMember.getName()), Snackbar.LENGTH_SHORT).show();
                Log.e(TAG, "onError: " + e.getMessage());
            }
        });
    }

    /**
     * This method is used to get list of group members. It also helps to update other things like
     * Admin count.
     *
     * @see GroupMembersRequest#fetchNext(CometChat.CallbackListener)
     * @see GroupMember
     */
    private void getGroupMembers() {
        List<String> groupMemeberScopes = new ArrayList<>();
        groupMemeberScopes.add(CometChatConstants.SCOPE_ADMIN);
        groupMemeberScopes.add(CometChatConstants.SCOPE_PARTICIPANT);
        if (groupMembersRequest == null) {
            groupMembersRequest = new GroupMembersRequest.GroupMembersRequestBuilder(guid)
                    .setScopes(Arrays.asList(CometChatConstants.SCOPE_ADMIN, CometChatConstants.SCOPE_PARTICIPANT, CometChatConstants.SCOPE_MODERATOR))
                    .setLimit(LIMIT).build();
        }
        groupMembersRequest.fetchNext(new CometChat.CallbackListener<List<GroupMember>>() {
            @Override
            public void onSuccess(List<GroupMember> groupMembers) {
                isInProgress = false;
                if (groupMembers != null && !groupMembers.isEmpty()) {
                    adminCount = 0;
                    moderatorCount = 0;
                    groupMemberUids.clear();
                    s = new String[groupMembers.size()];
                    for (int j = 0; j < groupMembers.size(); j++) {
                        groupMemberUids.add(groupMembers.get(j).getUid());
                        if (groupMembers.get(j).getScope().equals(CometChatConstants.SCOPE_ADMIN)) {
                            adminCount++;
                        }
                        if (groupMembers.get(j).getScope().equals(CometChatConstants.SCOPE_MODERATOR)) {
                            moderatorCount++;
                        }
                        s[j] = groupMembers.get(j).getName();
                    }
//                    tvAdminCount.setText(adminCount+"");
//                    tvModeratorCount.setText(moderatorCount+"");
                    if (groupMemberAdapter == null) {
                        groupMemberAdapter = new GroupMemberAdapter(CometChatGroupDetailScreenActivity.this, groupMembers, ownerId);
                        rvMemberList.setAdapter(groupMemberAdapter);
                    } else {
                        groupMemberAdapter.addAll(groupMembers);
                    }
                    if (groupMembers.size() < LIMIT) {
//                        tvLoadMore.setVisibility(View.GONE);
                        isNoMoreMembers = true;
                    }
                } else {
//                    tvLoadMore.setVisibility(View.GONE);
                    isNoMoreMembers = true;
                }
            }

            @Override
            public void onError(CometChatException e) {
                Snackbar.make(rvMemberList, getResources().getString(R.string.group_member_list_error), Snackbar.LENGTH_SHORT).show();
                Log.e(TAG, "onError: " + e.getMessage());
            }
        });
    }

//    private void getBannedMemberCount() {
//        banMemberRequest = new BannedGroupMembersRequest.BannedGroupMembersRequestBuilder(guid).setLimit(100).build();
//        banMemberRequest.fetchNext(new CometChat.CallbackListener<List<GroupMember>>() {
//            @Override
//            public void onSuccess(List<GroupMember> groupMembers) {
//                if (groupMembers.size()>=99) {
//                    tvBanMemberCount.setText("99+");
//                } else {
//                    tvBanMemberCount.setText(groupMembers.size()+"");
//                }
//            }
//
//            @Override
//            public void onError(CometChatException e) {
//                Log.e(TAG, "onError: "+e.getMessage()+"\n"+e.getCode());
//            }
//        });
//    }

    /**
     * This method is used to leave the loggedIn User from respective group.
     *
     * @see CometChat#leaveGroup(String, CometChat.CallbackListener)
     */
    private void leaveGroup() {
        CometChat.leaveGroup(guid, new CometChat.CallbackListener<String>() {
            @Override
            public void onSuccess(String s) {
                launchUnified();
            }

            @Override
            public void onError(CometChatException e) {
                Snackbar.make(rlAddMemberView, getResources().getString(R.string.leave_group_error), Snackbar.LENGTH_SHORT).show();
                Log.e(TAG, "onError: " + e.getMessage());
            }
        });
    }

    /**
     * This method is used to add group listener in this screen to receive real-time events.
     *
     * @see CometChat#addGroupListener(String, CometChat.GroupListener)
     */
    public void addGroupListener() {
        CometChat.addGroupListener(TAG, new CometChat.GroupListener() {
            @Override
            public void onGroupMemberJoined(Action action, User joinedUser, Group joinedGroup) {
                Log.e(TAG, "onGroupMemberJoined: " + joinedUser.getUid());
                if (joinedGroup.getGuid().equals(guid))
                    updateGroupMember(joinedUser, false, false, action);
            }

            @Override
            public void onGroupMemberLeft(Action action, User leftUser, Group leftGroup) {
                Log.d(TAG, "onGroupMemberLeft: ");
                if (leftGroup.getGuid().equals(guid))
                    updateGroupMember(leftUser, true, false, action);
            }

            @Override
            public void onGroupMemberKicked(Action action, User kickedUser, User kickedBy, Group kickedFrom) {
                Log.d(TAG, "onGroupMemberKicked: ");
                if (kickedFrom.getGuid().equals(guid))
                    updateGroupMember(kickedUser, true, false, action);
            }

            @Override
            public void onGroupMemberScopeChanged(Action action, User updatedBy, User updatedUser, String scopeChangedTo, String scopeChangedFrom, Group group) {
                Log.d(TAG, "onGroupMemberScopeChanged: ");
                if (group.getGuid().equals(guid))
                    updateGroupMember(updatedUser, false, true, action);
            }

            @Override
            public void onMemberAddedToGroup(Action action, User addedby, User userAdded, Group addedTo) {
                if (addedTo.getGuid().equals(guid))
                    updateGroupMember(userAdded, false, false, action);
            }

            @Override
            public void onGroupMemberBanned(Action action, User bannedUser, User bannedBy, Group bannedFrom) {
                if (bannedFrom.getGuid().equals(guid)) {
//                    int count = Integer.parseInt(tvBanMemberCount.getText().toString());
//                    tvBanMemberCount.setText(String.valueOf(++count));
                    updateGroupMember(bannedUser, true, false, action);
                }
            }

            @Override
            public void onGroupMemberUnbanned(Action action, User unbannedUser, User unbannedBy, Group unbannedFrom) {
                if (unbannedFrom.getGuid().equals(guid)) {
//                    int count = Integer.parseInt(tvBanMemberCount.getText().toString());
//                    tvBanMemberCount.setText(String.valueOf(--count));
                }
            }
        });
    }

    /**
     * This method is used to update group members from events recieved in real time. It updates or removes
     * group member from list based on parameters passed.
     *
     * @param user          is a object of User.
     * @param isRemoved     is a boolean which helps to know whether group member needs to be removed from list or not.
     * @param isScopeUpdate is a boolean which helps to know whether group member scope is updated or not.
     * @param action        is object of Action.
     * @see Action
     * @see GroupMember
     * @see User
     * @see
     */
    private void updateGroupMember(User user, boolean isRemoved, boolean isScopeUpdate, Action action) {
        if (groupMemberAdapter != null) {
            if (!isRemoved && !isScopeUpdate) {
                groupMemberAdapter.addGroupMember(UserToGroupMember(user, false, action.getOldScope()));
                int count = ++groupMemberCount;
                tvMemberCount.setText(count + " Members");
            } else if (isRemoved && !isScopeUpdate) {
                groupMemberAdapter.removeGroupMember(UserToGroupMember(user, false, action.getOldScope()));
                int count = --groupMemberCount;
                tvMemberCount.setText(count + " Members");
                if (action.getNewScope() != null) {
                    if (action.getNewScope().equals(CometChatConstants.SCOPE_ADMIN)) {
                        adminCount = adminCount - 1;
//                        tvAdminCount.setText(String.valueOf(adminCount));
                    } else if (action.getNewScope().equals(CometChatConstants.SCOPE_MODERATOR)) {
                        moderatorCount = moderatorCount - 1;
//                        tvModeratorCount.setText(String.valueOf(moderatorCount));
                    }
                }
            } else if (!isRemoved) {
                groupMemberAdapter.updateMember(UserToGroupMember(user, true, action.getNewScope()));
                if (action.getNewScope().equals(CometChatConstants.SCOPE_ADMIN)) {
                    adminCount = adminCount + 1;
//                    tvAdminCount.setText(String.valueOf(adminCount));
                    if (user.getUid().equals(loggedInUser.getUid())) {
                        // rlAddMemberView.setVisibility(View.VISIBLE);
                        loggedInUserScope = CometChatConstants.SCOPE_ADMIN;
                        // tvDelete.setVisibility(View.VISIBLE);
                    }
                } else if (action.getNewScope().equals(CometChatConstants.SCOPE_MODERATOR)) {
                    moderatorCount = moderatorCount + 1;
//                    tvModeratorCount.setText(String.valueOf(moderatorCount));
                    if (user.getUid().equals(loggedInUser.getUid())) {
                        rlBanMembers.setVisibility(View.VISIBLE);
                        loggedInUserScope = CometChatConstants.SCOPE_MODERATOR;
                    }
                } else if (action.getOldScope().equals(CometChatConstants.SCOPE_ADMIN)) {
                    adminCount = adminCount - 1;
//                    tvAdminCount.setText(String.valueOf(adminCount));
                } else if (action.getOldScope().equals(CometChatConstants.SCOPE_MODERATOR)) {
                    moderatorCount = moderatorCount - 1;
//                    tvModeratorCount.setText(String.valueOf(moderatorCount));
                }
            }
        }
    }


    /**
     * This method is used to remove group listener.
     */
    public void removeGroupListener() {
        CometChat.removeGroupListener(TAG);
    }

    /**
     * This method is used to get Group Details.
     *
     * @see CometChat#getGroup(String, CometChat.CallbackListener)
     */
    private void getGroup() {

        CometChat.getGroup(guid, new CometChat.CallbackListener<Group>() {
            @Override
            public void onSuccess(Group group) {
                gName = group.getName();
                // tvGroupName.setText(gName);
                toolbar.setTitle(gName);
                if (group.getIcon() != null && !group.getIcon().isEmpty()) {
                    // groupIcon.setAvatar(group.getIcon());
                    // groupImage.setImageURI(Uri.parse(group.getIcon()));
                    try {
                        URL url = new URL(group.getIcon());
                        Bitmap bmImg = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        BitmapDrawable background = new BitmapDrawable(bmImg);
                        groupImage.setBackgroundDrawable(background);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // groupIcon.setInitials(group.getName());
                }
                loggedInUserScope = group.getScope();
                // groupMemberCount = group.getMembersCount();
                groupType = group.getGroupType();
                gDesc = group.getDescription();
                if (gDesc != null && !gDesc.isEmpty()) {
                    tvGroupDesc.setText(gDesc);
                }
                tvMemberCount.setText(groupMemberCount + " Members");
            }

            @Override
            public void onError(CometChatException e) {
                Toast.makeText(CometChatGroupDetailScreenActivity.this, "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getGroup();
        groupMembersRequest = null;
        isNoMoreMembers = false;
        isInProgress = false;
        if (groupMemberAdapter != null) {
            groupMemberAdapter.resetAdapter();
            groupMemberAdapter = null;

        }
//        getBannedMemberCount();
        getGroupMembers();
        addGroupListener();
        callBtn.setClickable(true);
        videoCallBtn.setClickable(true);
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        removeGroupListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeGroupListener();
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }
}

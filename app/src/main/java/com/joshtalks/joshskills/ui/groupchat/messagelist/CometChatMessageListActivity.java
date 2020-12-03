package com.joshtalks.joshskills.ui.groupchat.messagelist;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.pro.constants.CometChatConstants;
import com.cometchat.pro.core.CometChat;
import com.cometchat.pro.core.GroupMembersRequest;
import com.cometchat.pro.core.MessagesRequest;
import com.cometchat.pro.exceptions.CometChatException;
import com.cometchat.pro.models.Action;
import com.cometchat.pro.models.Attachment;
import com.cometchat.pro.models.BaseMessage;
import com.cometchat.pro.models.CustomMessage;
import com.cometchat.pro.models.Group;
import com.cometchat.pro.models.GroupMember;
import com.cometchat.pro.models.MediaMessage;
import com.cometchat.pro.models.MessageReceipt;
import com.cometchat.pro.models.TextMessage;
import com.cometchat.pro.models.TypingIndicator;
import com.cometchat.pro.models.User;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.snackbar.Snackbar;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.core.PermissionUtils;
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener;
import com.joshtalks.joshskills.ui.groupchat.adapter.MessageAdapter;
import com.joshtalks.joshskills.ui.groupchat.constant.StringContract;
import com.joshtalks.joshskills.ui.groupchat.listeners.ComposeActionListener;
import com.joshtalks.joshskills.ui.groupchat.listeners.MessageActionCloseListener;
import com.joshtalks.joshskills.ui.groupchat.listeners.OnMessageLongClick;
import com.joshtalks.joshskills.ui.groupchat.listeners.StickyHeaderDecoration;
import com.joshtalks.joshskills.ui.groupchat.screens.CometChatGroupDetailScreenActivity;
import com.joshtalks.joshskills.ui.groupchat.uikit.Avatar;
import com.joshtalks.joshskills.ui.groupchat.uikit.ComposeBox.ComposeBox;
import com.joshtalks.joshskills.ui.groupchat.utils.KeyBoardUtils;
import com.joshtalks.joshskills.ui.groupchat.utils.MediaUtils;
import com.joshtalks.joshskills.ui.groupchat.utils.Utils;
import com.joshtalks.joshskills.util.ExoAudioPlayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.View.GONE;


/**
 * Purpose - CometChatMessageScreen class is a fragment used to display list of messages and perform certain action on click of message.
 * It also provide search bar to perform search operation on the list of messages. User can send text,images,video and file as messages
 * to each other and in groups. User can also perform actions like edit message,delete message and forward messages to other user and groups.
 *
 * @see CometChat
 * @see User
 * @see Group
 * @see TextMessage
 * @see MediaMessage
 * <p>
 * Created on - 20th December 2019
 * <p>
 * Modified on  - 16th January 2020
 */


public class CometChatMessageListActivity extends AppCompatActivity implements View.OnClickListener,
        MessageActionCloseListener, OnMessageLongClick, ExoAudioPlayer.ProgressUpdateListener, AudioPlayerEventListener {

    private static final String TAG = "CometChatMessageScreen";

    private static final int LIMIT = 30;
    private final List<BaseMessage> messageList = new ArrayList<>();
    private final User loggedInUser = CometChat.getLoggedInUser();
    private final int currentAudioPosition = -1;
    public int count = 0;
    private String name = "";
    private String status = "";
    private MessagesRequest messagesRequest;    //Used to fetch messages.
    private ComposeBox composeBox;
    private RecyclerView rvChatListView;    //Used to display list of messages.
    private MessageAdapter messageAdapter;
    private LinearLayoutManager linearLayoutManager;
    private ShimmerFrameLayout messageShimmer;
    /**
     * <b>Avatar</b> is a UI Kit Component which is used to display user and group avatars.
     */
    private Avatar userAvatar;
    private TextView tvName;
    private TextView tvStatus;
    private String Id;
    private StickyHeaderDecoration stickyHeaderDecoration;
    private String avatarUrl;
    private Toolbar toolbar;
    private ImageView imgClose;
    private String type;
    private String groupType;
    private String loggedInUserScope;
    private RelativeLayout replyMessageLayout;
    private TextView replyTitle;
    private TextView replyMessage;
    private ImageView replyMedia;
    private ImageView replyClose;
    private BaseMessage baseMessage;
    private List<BaseMessage> baseMessages = new ArrayList<>();
    private boolean isEdit;
    private boolean isReply;
    private String groupOwnerId;
    private int memberCount;
    private String memberNames;
    private int totalMembers;
    private int onlineMembers;
    private String groupDesc;
    private String groupPassword;
    private Timer typingTimer = new Timer();
    private boolean isNoMoreMessages;
    private boolean isInProgress;
    private MessageActionFragment messageActionFragment;
    private ExoAudioPlayer audioPlayerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_chat_screen);
        handleArguments();
        initViewComponent();
    }

    /**
     * This method is used to handle arguments passed to this fragment.
     */
    private void handleArguments() {
        avatarUrl = getIntent().getStringExtra(StringContract.IntentStrings.AVATAR);
        name = getIntent().getStringExtra(StringContract.IntentStrings.NAME);
        type = getIntent().getStringExtra(StringContract.IntentStrings.TYPE);

        if (getIntent().hasExtra(StringContract.IntentStrings.TYPE) &&
                getIntent().getStringExtra(StringContract.IntentStrings.TYPE).equals(CometChatConstants.RECEIVER_TYPE_USER)) {
            Id = getIntent().getStringExtra(StringContract.IntentStrings.UID);
            status = getIntent().getStringExtra(StringContract.IntentStrings.STATUS);
        } else {
            Id = getIntent().getStringExtra(StringContract.IntentStrings.GUID);
            status = getIntent().getStringExtra(StringContract.IntentStrings.STATUS);
            totalMembers = getIntent().getIntExtra(StringContract.IntentStrings.MEMBER_COUNT, -1);

            groupDesc = getIntent().getStringExtra(StringContract.IntentStrings.GROUP_DESC);
            groupPassword = getIntent().getStringExtra(StringContract.IntentStrings.GROUP_PASSWORD);
            groupType = getIntent().getStringExtra(StringContract.IntentStrings.GROUP_TYPE);
        }
    }

    private void initViewComponent() {
        audioPlayerManager = ExoAudioPlayer.getInstance();
        if (audioPlayerManager != null) {
            audioPlayerManager.setProgressUpdateListener(this);
            audioPlayerManager.setPlayerListener(this);
        }
        messageShimmer = findViewById(R.id.shimmer_layout);
        composeBox = findViewById(R.id.message_box);
        composeBox.usedIn(CometChatMessageListActivity.class.getName());
        setComposeBoxListener();

        replyMessageLayout = findViewById(R.id.replyMessageLayout);
        replyTitle = findViewById(R.id.tv_reply_layout_title);
        replyMessage = findViewById(R.id.tv_reply_layout_subtitle);
        replyMedia = findViewById(R.id.iv_reply_media);
        replyClose = findViewById(R.id.iv_reply_close);
        replyClose.setOnClickListener(this);


        rvChatListView = findViewById(R.id.rv_message_list);
        tvName = findViewById(R.id.tv_name);
        tvStatus = findViewById(R.id.tv_status);
        userAvatar = findViewById(R.id.iv_chat_avatar);
        toolbar = findViewById(R.id.chatList_toolbar);
        imgClose = findViewById(R.id.iv_close_message_action);
        imgClose.setOnClickListener(this);
        toolbar.setOnClickListener(this);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        tvName.setText(name);
        setAvatar();

        rvChatListView.setLayoutManager(linearLayoutManager);

        setSupportActionBar(toolbar);


        KeyBoardUtils.setKeyboardVisibilityListener(this, (View) rvChatListView.getParent(), keyboardVisible -> {
            if (keyboardVisible) {
                scrollToBottom();
                composeBox.ivMic.setVisibility(GONE);
                composeBox.ivSend.setVisibility(View.VISIBLE);
            } else {
                if (isEdit) {
                    composeBox.ivMic.setVisibility(GONE);
                    composeBox.ivSend.setVisibility(View.VISIBLE);
                } else {
                    composeBox.ivMic.setVisibility(View.VISIBLE);
                    composeBox.ivSend.setVisibility(GONE);
                }
            }
        });


        // Uses to fetch next list of messages if rvChatListView (RecyclerView) is scrolled in downward direction.
        rvChatListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {

                //for toolbar elevation animation i.e stateListAnimator
                toolbar.setSelected(rvChatListView.canScrollVertically(-1));
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {

                if (!isNoMoreMessages && !isInProgress) {
                    if (linearLayoutManager.findFirstVisibleItemPosition() == 10 || !rvChatListView.canScrollVertically(-1)) {
                        isInProgress = true;
                        fetchMessage();
                    }
                }
            }

        });
    }

    private void setComposeBoxListener() {

        composeBox.setComposeBoxListener(new ComposeActionListener() {
            @Override
            public void onEditTextMediaSelected(InputContentInfoCompat inputContentInfo) {
                Log.e(TAG, "onEditTextMediaSelected: Path=" + inputContentInfo.getLinkUri().getPath()
                        + "\nHost=" + inputContentInfo.getLinkUri().getFragment());
                String messageType = inputContentInfo.getLinkUri().toString().substring(inputContentInfo.getLinkUri().toString().lastIndexOf('.'));
                MediaMessage mediaMessage = new MediaMessage(Id, null, CometChatConstants.MESSAGE_TYPE_IMAGE, type);
                Attachment attachment = new Attachment();
                attachment.setFileUrl(inputContentInfo.getLinkUri().toString());
                attachment.setFileMimeType(inputContentInfo.getDescription().getMimeType(0));
                attachment.setFileExtension(messageType);
                attachment.setFileName(inputContentInfo.getDescription().getLabel().toString());
                mediaMessage.setAttachment(attachment);
                Log.e(TAG, "onClick: " + attachment.toString());
                CometChat.sendMediaMessage(mediaMessage, new CometChat.CallbackListener<MediaMessage>() {
                    @Override
                    public void onSuccess(MediaMessage mediaMessage) {
                        if (messageAdapter != null) {
                            messageAdapter.addMessage(mediaMessage);
                            scrollToBottom();
                        }
                    }

                    @Override
                    public void onError(CometChatException e) {
                        Toast.makeText(CometChatMessageListActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                sendTypingIndicator(charSequence.length() <= 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (typingTimer == null) {
                    typingTimer = new Timer();
                }
                endTypingTimer();
            }

            @Override
            public void onAudioActionClicked() {
                if (Utils.hasPermissions(CometChatMessageListActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    startActivityForResult(MediaUtils.openAudio(CometChatMessageListActivity.this), StringContract.RequestCode.AUDIO);
                } else {
                    PermissionUtils.checkPermissionForAudioRecord(CometChatMessageListActivity.this);

                    //  requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, StringContract.RequestCode.AUDIO,1001);
                }
            }

            @Override
            public void onSendActionClicked(EditText editText) {
                String message = editText.getText().toString().trim();
                editText.setText("");
                editText.setHint(getString(R.string.message));
                if (isReply) {
                    replyMessage(baseMessage, message);
                    replyMessageLayout.setVisibility(GONE);
                } else if (!message.isEmpty()) {
                    sendMessage(message);
                }
            }

            @Override
            public void onVoiceNoteComplete(String string, JSONObject metadata) {
                if (string != null) {
                    File audioFile = new File(string);
                    if (isReply) {
                        sendMediaMessage(audioFile, CometChatConstants.MESSAGE_TYPE_AUDIO, metadata, baseMessage);
                    } else {
                        sendMediaMessage(audioFile, CometChatConstants.MESSAGE_TYPE_AUDIO, metadata, null);
                    }
                }
            }
        });
    }

    /**
     * This method sends custom message based on parameter received
     *
     * @param customType
     * @param customData
     */
    private void sendCustomMessage(String customType, JSONObject customData) {
        CustomMessage customMessage;

        if (type.equalsIgnoreCase(CometChatConstants.RECEIVER_TYPE_USER))
            customMessage = new CustomMessage(Id, CometChatConstants.RECEIVER_TYPE_USER, customType, customData);
        else
            customMessage = new CustomMessage(Id, CometChatConstants.RECEIVER_TYPE_GROUP, customType, customData);

        CometChat.sendCustomMessage(customMessage, new CometChat.CallbackListener<CustomMessage>() {
            @Override
            public void onSuccess(CustomMessage customMessage) {
                if (messageAdapter != null) {
                    messageAdapter.addMessage(customMessage);
                    scrollToBottom();
                }
            }

            @Override
            public void onError(CometChatException e) {
                Toast.makeText(CometChatMessageListActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case StringContract.RequestCode.FILE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    startActivityForResult(MediaUtils.getFileIntent(StringContract.IntentStrings.EXTRA_MIME_DOC), StringContract.RequestCode.FILE);
                else
                    showSnackBar(findViewById(R.id.message_box), getResources().getString(R.string.grant_storage_permission));
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showSnackBar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is used to get Group Members and display names of group member.
     *
     * @see GroupMember
     * @see GroupMembersRequest
     */
    private void getMember() {
        GroupMembersRequest groupMembersRequest = new GroupMembersRequest.GroupMembersRequestBuilder(Id).setLimit(100).build();

        groupMembersRequest.fetchNext(new CometChat.CallbackListener<List<GroupMember>>() {
            @Override
            public void onSuccess(List<GroupMember> list) {
                String[] s = new String[0];
                if (list != null && list.size() != 0) {
                    s = new String[list.size()];
                    for (int j = 0; j < list.size(); j++) {

                        s[j] = list.get(j).getName();
                    }

                }
                setSubTitle(s);

            }

            @Override
            public void onError(CometChatException e) {
                Log.d(TAG, "Group Member list fetching failed with exception: " + e.getMessage());
                Toast.makeText(CometChatMessageListActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        });
    }

    /**
     * This method is used to set GroupMember names as subtitle in toolbar.
     *
     * @param users
     */
    private void setSubTitle(String... users) {
        if (users != null && users.length != 0) {
            StringBuilder stringBuilder = new StringBuilder();

            for (String user : users) {
                stringBuilder.append(user).append(",");
            }

            memberNames = stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
            totalMembers = users.length;
            // Random Number between 10%-30% of totalMembers
            onlineMembers = (int) ((new Random().nextInt((30 - 10) + 1) + 10) / 100.0) * totalMembers;
            if (onlineMembers == 0) {
                onlineMembers++;
            }
            tvStatus.setText(String.format("%d Members, %d Online", totalMembers, onlineMembers));
        }

    }


    /**
     * This method is used to fetch message of users & groups. For user it fetches previous 100 messages at
     * a time and for groups it fetches previous 30 messages. You can change limit of messages by modifying
     * number in <code>setLimit()</code>
     * This method also mark last message as read using markMessageAsRead() present in this class.
     * So all the above messages get marked as read.
     *
     * @see MessagesRequest#fetchPrevious(CometChat.CallbackListener)
     */
    private void fetchMessage() {

        if (messagesRequest == null) {
            if (type != null) {
                if (type.equals(CometChatConstants.RECEIVER_TYPE_USER))
                    messagesRequest = new MessagesRequest.MessagesRequestBuilder().setLimit(LIMIT)
                            .setTypes(StringContract.MessageRequest.messageTypesForUser)
                            .setCategories(StringContract.MessageRequest.messageCategoriesForUser)
                            .hideReplies(true).setUID(Id).build();
                else
                    messagesRequest = new MessagesRequest.MessagesRequestBuilder().setLimit(LIMIT)
                            .setTypes(StringContract.MessageRequest.messageTypesForGroup)
                            .setCategories(StringContract.MessageRequest.messageCategoriesForGroup)
                            .hideReplies(true).setGUID(Id).hideMessagesFromBlockedUsers(true).build();
            }
        }
        messagesRequest.fetchPrevious(new CometChat.CallbackListener<List<BaseMessage>>() {

            @Override
            public void onSuccess(List<BaseMessage> baseMessages) {
                isInProgress = false;
//                List<BaseMessage> filteredMessageList = filterBaseMessages(baseMessages);
//                initMessageAdapter(filteredMessageList);

                initMessageAdapter(baseMessages);
                if (baseMessages.size() != 0) {
                    stopHideShimmer();
                    BaseMessage baseMessage = baseMessages.get(baseMessages.size() - 1);
                    markMessageAsRead(baseMessage);
                }

                if (baseMessages.size() == 0) {
                    stopHideShimmer();
                    isNoMoreMessages = true;
                }
            }

            @Override
            public void onError(CometChatException e) {
                Log.d(TAG, "onError: " + e.getMessage());
            }
        });
    }

    private void stopHideShimmer() {
        messageShimmer.stopShimmer();
        messageShimmer.setVisibility(GONE);
    }

    /**
     * This method is used to initialize the message adapter if it is empty else it helps
     * to update the messagelist in adapter.
     *
     * @param messageList is a list of messages which will be added.
     */
    private void initMessageAdapter(List<BaseMessage> messageList) {
        if (messageAdapter == null) {
            messageAdapter = new MessageAdapter(CometChatMessageListActivity.this, messageList);
            rvChatListView.setAdapter(messageAdapter);
            stickyHeaderDecoration = new StickyHeaderDecoration(messageAdapter);
            rvChatListView.addItemDecoration(stickyHeaderDecoration, 0);
            scrollToBottom();
            messageAdapter.notifyDataSetChanged();
        } else {
            messageAdapter.updateList(messageList);

        }
    }

    /**
     * This method is used to send typing indicator to other users and groups.
     *
     * @param isEnd is boolean which is used to differentiate between startTyping & endTyping Indicators.
     * @see CometChat#startTyping(TypingIndicator)
     * @see CometChat#endTyping(TypingIndicator)
     */
    private void sendTypingIndicator(boolean isEnd) {
        if (isEnd) {
            if (type.equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                CometChat.endTyping(new TypingIndicator(Id, CometChatConstants.RECEIVER_TYPE_USER));
            } else {
                CometChat.endTyping(new TypingIndicator(Id, CometChatConstants.RECEIVER_TYPE_GROUP));
            }
        } else {
            if (type.equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                CometChat.startTyping(new TypingIndicator(Id, CometChatConstants.RECEIVER_TYPE_USER));
            } else {
                CometChat.startTyping(new TypingIndicator(Id, CometChatConstants.RECEIVER_TYPE_GROUP));
            }
        }
    }

    private void endTypingTimer() {
        if (typingTimer != null) {
            typingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sendTypingIndicator(true);
                }
            }, 2000);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: ");

        switch (requestCode) {
            case StringContract.RequestCode.AUDIO:
                if (data != null) {
                    File file = MediaUtils.getRealPath(this, data.getData());
                    ContentResolver cr = this.getContentResolver();
                    if (isReply) {
                        sendMediaMessage(file, CometChatConstants.MESSAGE_TYPE_AUDIO, null, baseMessage);
                    } else {
                        sendMediaMessage(file, CometChatConstants.MESSAGE_TYPE_AUDIO, null, null);
                    }
                }
                break;

            case StringContract.RequestCode.FILE:
                if (data != null)
                    if (isReply) {
                        sendMediaMessage(MediaUtils.getRealPath(this, data.getData()), CometChatConstants.MESSAGE_TYPE_FILE, null, baseMessage);
                    } else {
                        sendMediaMessage(MediaUtils.getRealPath(this, data.getData()), CometChatConstants.MESSAGE_TYPE_FILE, null, null);
                    }
                break;
        }

    }


    /**
     * This method is used to send media messages to other users and group.
     *
     * @param file        is an object of File which is been sent within the message.
     * @param filetype    is a string which indicate a type of file been sent within the message.
     * @param baseMessage is a linked message for reply
     * @see CometChat#sendMediaMessage(MediaMessage, CometChat.CallbackListener)
     * @see MediaMessage
     */
    private void sendMediaMessage(File file, String filetype, JSONObject metadata, BaseMessage baseMessage) {
        try {
            ProgressDialog progressDialog;
            progressDialog = ProgressDialog.show(CometChatMessageListActivity.this, "", getResources().getString(R.string.sending_media_message));
            MediaMessage mediaMessage;

            if (type.equalsIgnoreCase(CometChatConstants.RECEIVER_TYPE_USER))
                mediaMessage = new MediaMessage(Id, file, filetype, CometChatConstants.RECEIVER_TYPE_USER);
            else
                mediaMessage = new MediaMessage(Id, file, filetype, CometChatConstants.RECEIVER_TYPE_GROUP);

            if (metadata == null) {
                metadata = new JSONObject();
            }
            try {
                metadata.put("path", file.getAbsolutePath());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (isReply) {
                JSONObject replyObject = new JSONObject();
                if (baseMessage != null) {
                    if (baseMessage.getCategory().equals(CometChatConstants.CATEGORY_MESSAGE)) {
                        switch (baseMessage.getType()) {
                            case CometChatConstants.MESSAGE_TYPE_TEXT:
                                replyObject.put("type", CometChatConstants.MESSAGE_TYPE_TEXT);
                                replyObject.put("message", ((TextMessage) baseMessage).getText());
                                break;
                            case CometChatConstants.MESSAGE_TYPE_IMAGE:
                                replyObject.put("type", CometChatConstants.MESSAGE_TYPE_IMAGE);
                                replyObject.put("message", "image");
                                break;
                            case CometChatConstants.MESSAGE_TYPE_VIDEO:
                                replyObject.put("type", CometChatConstants.MESSAGE_TYPE_VIDEO);
                                replyObject.put("message", "video");
                                break;
                            case CometChatConstants.MESSAGE_TYPE_FILE:
                                replyObject.put("type", CometChatConstants.MESSAGE_TYPE_FILE);
                                replyObject.put("message", "file");
                                break;
                            case CometChatConstants.MESSAGE_TYPE_AUDIO:
                                replyObject.put("type", CometChatConstants.MESSAGE_TYPE_AUDIO);
                                replyObject.put("message", "audio");
                                break;
                        }
                    } else if (baseMessage.getType().equals(StringContract.IntentStrings.LOCATION)) {
                        replyObject.put("type", StringContract.IntentStrings.LOCATION);
                        replyObject.put("message", "location");
                    } else if (baseMessage.getType().equals(StringContract.IntentStrings.POLLS)) {
                        replyObject.put("type", StringContract.IntentStrings.POLLS);
                        replyObject.put("message", ((CustomMessage) baseMessage).getCustomData().getString("question"));
                    }
                    replyObject.put("name", baseMessage.getSender().getName());
                    replyObject.put("avatar", baseMessage.getSender().getAvatar());
                }
                metadata.put("reply", replyObject);
                replyMessageLayout.setVisibility(GONE);
            }

            mediaMessage.setMetadata(metadata);

            CometChat.sendMediaMessage(mediaMessage, new CometChat.CallbackListener<MediaMessage>() {
                @Override
                public void onSuccess(MediaMessage mediaMessage) {
                    progressDialog.dismiss();
                    Log.d(TAG, "sendMediaMessage onSuccess: " + mediaMessage.toString());
                    if (messageAdapter != null) {
                        messageAdapter.addMessage(mediaMessage);
                        scrollToBottom();
                    }
                }

                @Override
                public void onError(CometChatException e) {
                    progressDialog.dismiss();
                    Toast.makeText(CometChatMessageListActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is used to get details of reciever.
     *
     * @see CometChat#getUser(String, CometChat.CallbackListener)
     */
    private void getUser() {

        CometChat.getUser(Id, new CometChat.CallbackListener<User>() {
            @Override
            public void onSuccess(User user) {
                if (user.isBlockedByMe()) {
                    toolbar.setSelected(false);
                } else {
                    avatarUrl = user.getAvatar();
                    if (user.getStatus().equals(CometChatConstants.USER_STATUS_ONLINE)) {
                        tvStatus.setTextColor(getApplicationContext().getResources().getColor(R.color.colorPrimary));
                    }
                    setAvatar();
                    tvStatus.setText(status);

                }
                name = user.getName();
                tvName.setText(name);
                Log.d(TAG, "onSuccess: " + user.toString());

            }

            @Override
            public void onError(CometChatException e) {
                Toast.makeText(CometChatMessageListActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setAvatar() {
        if (avatarUrl != null && !avatarUrl.isEmpty())
            userAvatar.setAvatar(avatarUrl);
        else {
            userAvatar.setInitials(name);
        }
    }

    /**
     * This method is used to get Group Details.
     *
     * @see CometChat#getGroup(String, CometChat.CallbackListener)
     */
    private void getGroup() {

        CometChat.getGroup(Id, new CometChat.CallbackListener<Group>() {
            @Override
            public void onSuccess(Group group) {
                name = group.getName();
                avatarUrl = group.getIcon();
                loggedInUserScope = group.getScope();
                groupOwnerId = group.getOwner();

                tvName.setText(name);
                userAvatar.setAvatar(getApplicationContext().getResources().getDrawable(R.drawable.ic_account), avatarUrl);
                setAvatar();
            }

            @Override
            public void onError(CometChatException e) {

            }
        });
    }

    /**
     * This method is used to send Text Message to other users and groups.
     *
     * @param message is a String which is been sent as message.
     * @see TextMessage
     * @see CometChat#sendMessage(TextMessage, CometChat.CallbackListener)
     */
    private void sendMessage(String message) {
        TextMessage textMessage;
        if (type.equalsIgnoreCase(CometChatConstants.RECEIVER_TYPE_USER))
            textMessage = new TextMessage(Id, message, CometChatConstants.RECEIVER_TYPE_USER);
        else
            textMessage = new TextMessage(Id, message, CometChatConstants.RECEIVER_TYPE_GROUP);


        sendTypingIndicator(true);

        CometChat.sendMessage(textMessage, new CometChat.CallbackListener<TextMessage>() {
            @Override
            public void onSuccess(TextMessage textMessage) {
                if (messageAdapter != null) {
                    if (StringContract.Sounds.enableMessageSounds)
                        // MediaUtils.playSendSound(context, R.raw.outgoing_message);
                        messageAdapter.addMessage(textMessage);
                    scrollToBottom();
                }
            }

            @Override
            public void onError(CometChatException e) {
                Log.d(TAG, "onError: " + e.getMessage());
            }
        });

    }

    /**
     * This method is used to send reply message by link previous message with new message.
     *
     * @param baseMessage is a linked message
     * @param message     is a String. It will be new message sent as reply.
     */
    private void replyMessage(BaseMessage baseMessage, String message) {
        isReply = false;
        try {
            TextMessage textMessage;
            if (type.equalsIgnoreCase(CometChatConstants.RECEIVER_TYPE_USER))
                textMessage = new TextMessage(Id, message, CometChatConstants.RECEIVER_TYPE_USER);
            else
                textMessage = new TextMessage(Id, message, CometChatConstants.RECEIVER_TYPE_GROUP);
            JSONObject jsonObject = new JSONObject();
            JSONObject replyObject = new JSONObject();
            if (baseMessage.getCategory().equals(CometChatConstants.CATEGORY_MESSAGE)) {
                if (baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_TEXT)) {
                    replyObject.put("type", CometChatConstants.MESSAGE_TYPE_TEXT);
                    replyObject.put("message", ((TextMessage) baseMessage).getText());
                } else if (baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_IMAGE)) {
                    replyObject.put("type", CometChatConstants.MESSAGE_TYPE_IMAGE);
                    replyObject.put("message", "image");
                } else if (baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_VIDEO)) {
                    replyObject.put("type", CometChatConstants.MESSAGE_TYPE_VIDEO);
                    replyObject.put("message", "video");
                } else if (baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_FILE)) {
                    replyObject.put("type", CometChatConstants.MESSAGE_TYPE_FILE);
                    replyObject.put("message", "file");
                } else if (baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_AUDIO)) {
                    replyObject.put("type", CometChatConstants.MESSAGE_TYPE_AUDIO);
                    replyObject.put("message", "audio");
                }
            } else if (baseMessage.getType().equals(StringContract.IntentStrings.LOCATION)) {
                replyObject.put("type", StringContract.IntentStrings.LOCATION);
                replyObject.put("message", "location");
            } else if (baseMessage.getType().equals(StringContract.IntentStrings.POLLS)) {
                replyObject.put("type", StringContract.IntentStrings.POLLS);
                replyObject.put("message", ((CustomMessage) baseMessage).getCustomData().getString("question"));
            }
            replyObject.put("name", baseMessage.getSender().getName());
            replyObject.put("avatar", baseMessage.getSender().getAvatar());
            jsonObject.put("reply", replyObject);
            textMessage.setMetadata(jsonObject);
            sendTypingIndicator(true);
            CometChat.sendMessage(textMessage, new CometChat.CallbackListener<TextMessage>() {
                @Override
                public void onSuccess(TextMessage textMessage) {
                    if (messageAdapter != null) {
                        if (StringContract.Sounds.enableMessageSounds)
                            // MediaUtils.playSendSound(context, R.raw.outgoing_message);
                            messageAdapter.addMessage(textMessage);
                        scrollToBottom();
                    }
                }

                @Override
                public void onError(CometChatException e) {
                    Log.e(TAG, "onError: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "replyMessage: " + e.getMessage());
        }
    }

    private void scrollToBottom() {
        if (messageAdapter != null && messageAdapter.getItemCount() > 0) {
            rvChatListView.scrollToPosition(messageAdapter.getItemCount() - 1);

        }
    }

    /**
     * This method is used to recieve real time group events like onMemberAddedToGroup, onGroupMemberJoined,
     * onGroupMemberKicked, onGroupMemberLeft, onGroupMemberBanned, onGroupMemberUnbanned,
     * onGroupMemberScopeChanged.
     *
     * @see CometChat#addGroupListener(String, CometChat.GroupListener)
     */
    private void addGroupListener() {
        CometChat.addGroupListener(TAG, new CometChat.GroupListener() {
            @Override
            public void onGroupMemberJoined(Action action, User joinedUser, Group joinedGroup) {
                super.onGroupMemberJoined(action, joinedUser, joinedGroup);
                if (joinedGroup.getGuid().equals(Id)) {
                    totalMembers++;
                    onlineMembers++;
                    memberNames += "," + joinedUser.getName();
                    tvStatus.setText(String.format("%d Members, %d Online", totalMembers, onlineMembers));
                }
                onMessageReceived(action);
            }

            @Override
            public void onGroupMemberLeft(Action action, User leftUser, Group leftGroup) {
                super.onGroupMemberLeft(action, leftUser, leftGroup);
                Log.d(TAG, "onGroupMemberLeft: " + leftUser.getName());
                if (leftGroup.getGuid().equals(Id)) {
                    if (totalMembers > 1) {
                        totalMembers--;
                    }
                    if (onlineMembers > 1) {
                        onlineMembers--;
                    }
                    if (memberNames != null) {
                        memberNames = memberNames.replace("," + leftUser.getName(), "");
                    }
                    tvStatus.setText(String.format("%d Members, %d Online", totalMembers, onlineMembers));
                }
                onMessageReceived(action);
            }

            @Override
            public void onGroupMemberKicked(Action action, User kickedUser, User kickedBy, Group kickedFrom) {
                super.onGroupMemberKicked(action, kickedUser, kickedBy, kickedFrom);
                Log.d(TAG, "onGroupMemberKicked: " + kickedUser.getName());
                if (kickedUser.getUid().equals(CometChat.getLoggedInUser().getUid())) {
                    CometChatMessageListActivity.this.finish();
                }
                if (kickedFrom.getGuid().equals(Id)) {
                    if (totalMembers > 1) {
                        totalMembers--;
                    }
                    if (onlineMembers > 1) {
                        onlineMembers--;
                    }
                    if (memberNames != null) {
                        memberNames = memberNames.replace("," + kickedUser.getName(), "");
                    }
                    tvStatus.setText(String.format("%d Members, %d Online", totalMembers, onlineMembers));
                }
                onMessageReceived(action);
            }

            @Override
            public void onGroupMemberBanned(Action action, User bannedUser, User bannedBy, Group bannedFrom) {
                if (bannedUser.getUid().equals(CometChat.getLoggedInUser().getUid())) {
                    onBackPressed();
                    Toast.makeText(CometChatMessageListActivity.this, "You have been banned", Toast.LENGTH_SHORT).show();
                }
                onMessageReceived(action);

            }

            @Override
            public void onGroupMemberUnbanned(Action action, User unbannedUser, User unbannedBy, Group unbannedFrom) {
                onMessageReceived(action);
            }

            @Override
            public void onGroupMemberScopeChanged(Action action, User updatedBy, User updatedUser, String scopeChangedTo, String scopeChangedFrom, Group group) {
                onMessageReceived(action);
            }

            @Override
            public void onMemberAddedToGroup(Action action, User addedby, User userAdded, Group addedTo) {
                if (addedTo.getGuid().equals(Id)) {
                    totalMembers++;
                    onlineMembers++;
                    tvStatus.setText(String.format("%d Members, %d Online", totalMembers, onlineMembers));
                }
                onMessageReceived(action);
            }
        });
    }

    /**
     * This method is used to get real time user status i.e user is online or offline.
     *
     * @see CometChat#addUserListener(String, CometChat.UserListener)
     */
    private void addUserListener() {
        if (type.equals(CometChatConstants.RECEIVER_TYPE_USER)) {
            CometChat.addUserListener(TAG, new CometChat.UserListener() {
                @Override
                public void onUserOnline(User user) {
                    Log.d(TAG, "onUserOnline: " + user.toString());
                    if (user.getUid().equals(Id)) {
                        status = CometChatConstants.USER_STATUS_ONLINE;
                        tvStatus.setText(user.getStatus());
                        tvStatus.setTextColor(getResources().getColor(R.color.colorPrimary));
                    }
                }

                @Override
                public void onUserOffline(User user) {
                    Log.d(TAG, "onUserOffline: " + user.toString());
                    if (user.getUid().equals(Id)) {
                        if (Utils.isDarkMode(getApplicationContext()))
                            tvStatus.setTextColor(getResources().getColor(R.color.white));
                        else
                            tvStatus.setTextColor(getResources().getColor(android.R.color.black));
                        tvStatus.setText(user.getStatus());
                        status = CometChatConstants.USER_STATUS_OFFLINE;
                    }
                }
            });
        }
    }


    /**
     * This method is used to mark users & group message as read.
     *
     * @param baseMessage is object of BaseMessage.class. It is message which is been marked as read.
     */
    private void markMessageAsRead(BaseMessage baseMessage) {
        if (type.equals(CometChatConstants.RECEIVER_TYPE_USER))
            CometChat.markAsRead(baseMessage.getId(), baseMessage.getSender().getUid(), baseMessage.getReceiverType());
        else
            CometChat.markAsRead(baseMessage.getId(), baseMessage.getReceiverUid(), baseMessage.getReceiverType());
    }


    /**
     * This method is used to add message listener to recieve real time messages between users &
     * groups. It also give real time events for typing indicators, edit message, delete message,
     * message being read & delivered.
     *
     * @see CometChat#addMessageListener(String, CometChat.MessageListener)
     */
    private void addMessageListener() {

        CometChat.addMessageListener(TAG, new CometChat.MessageListener() {
            @Override
            public void onTextMessageReceived(TextMessage message) {
                Log.d(TAG, "onTextMessageReceived: " + message.toString());
                onMessageReceived(message);
            }

            @Override
            public void onMediaMessageReceived(MediaMessage message) {
                Log.d(TAG, "onMediaMessageReceived: " + message.toString());
                onMessageReceived(message);
            }

            @Override
            public void onCustomMessageReceived(CustomMessage message) {
                Log.d(TAG, "onCustomMessageReceived: " + message.toString());
                onMessageReceived(message);
            }

            @Override
            public void onTypingStarted(TypingIndicator typingIndicator) {
                Log.e(TAG, "onTypingStarted: " + typingIndicator);
                setTypingIndicator(typingIndicator, true);
            }

            @Override
            public void onTypingEnded(TypingIndicator typingIndicator) {
                Log.d(TAG, "onTypingEnded: " + typingIndicator.toString());
                setTypingIndicator(typingIndicator, false);
            }

            @Override
            public void onMessagesDelivered(MessageReceipt messageReceipt) {
                Log.d(TAG, "onMessagesDelivered: " + messageReceipt.toString());
                setMessageReciept(messageReceipt);
            }

            @Override
            public void onMessagesRead(MessageReceipt messageReceipt) {
                Log.e(TAG, "onMessagesRead: " + messageReceipt.toString());
                setMessageReciept(messageReceipt);
            }

            @Override
            public void onMessageEdited(BaseMessage message) {
                Log.d(TAG, "onMessageEdited: " + message.toString());
                updateMessage(message);
            }

            @Override
            public void onMessageDeleted(BaseMessage message) {
                Log.d(TAG, "onMessageDeleted: ");
                updateMessage(message);
            }
        });
    }

    private void setMessageReciept(MessageReceipt messageReceipt) {
        if (messageAdapter != null) {
            if (messageReceipt != null && messageReceipt.getReceiptType() != null &&
                    messageReceipt.getReceivertype().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                if (Id != null && messageReceipt.getSender() != null && messageReceipt.getSender().getUid().equals(Id)) {
                    if (messageReceipt.getReceiptType().equals(MessageReceipt.RECEIPT_TYPE_DELIVERED))
                        messageAdapter.setDeliveryReceipts(messageReceipt);
                    else
                        messageAdapter.setReadReceipts(messageReceipt);
                }
            }
        }
    }

    private void setTypingIndicator(TypingIndicator typingIndicator, boolean isShow) {
        if (typingIndicator.getReceiverType().equalsIgnoreCase(CometChatConstants.RECEIVER_TYPE_USER)) {
            Log.e(TAG, "onTypingStarted: " + typingIndicator);
            if (Id != null && Id.equalsIgnoreCase(typingIndicator.getSender().getUid())) {
                if (typingIndicator.getMetadata() == null)
                    typingIndicator(typingIndicator, isShow);
            }
        } else {
            if (Id != null && Id.equalsIgnoreCase(typingIndicator.getReceiverId()))
                typingIndicator(typingIndicator, isShow);
        }
    }

    private void onMessageReceived(BaseMessage message) {
        if (StringContract.Sounds.enableMessageSounds)
            // MediaUtils.playSendSound(context, R.raw.incoming_message);
            if (message.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                if (Id != null && Id.equalsIgnoreCase(message.getSender().getUid())) {
                    setMessage(message);
                } else if (Id != null && Id.equalsIgnoreCase(message.getReceiverUid()) && message.getSender().getUid().equalsIgnoreCase(loggedInUser.getUid())) {
                    setMessage(message);
                }
            } else {
                if (Id != null && Id.equalsIgnoreCase(message.getReceiverUid())) {
                    setMessage(message);
                }
            }
    }

    /**
     * This method is used to update edited message by calling <code>setEditMessage()</code> of adapter
     *
     * @param message is an object of BaseMessage and it will replace with old message.
     * @see BaseMessage
     */
    private void updateMessage(BaseMessage message) {
        if (messageAdapter != null)
            messageAdapter.setUpdatedMessage(message);
    }


    /**
     * This method is used to mark message as read before adding them to list. This method helps to
     * add real time message in list.
     *
     * @param message is an object of BaseMessage, It is recieved from message listener.
     * @see BaseMessage
     */
    private void setMessage(BaseMessage message) {
        if (message.getParentMessageId() == 0) {
            if (messageAdapter != null) {
                messageAdapter.addMessage(message);
                markMessageAsRead(message);
                if ((messageAdapter.getItemCount() - 1) - ((LinearLayoutManager) rvChatListView.getLayoutManager()).findLastVisibleItemPosition() < 5)
                    scrollToBottom();
            } else {
                messageList.add(message);
                initMessageAdapter(messageList);
            }
        }
    }

    /**
     * This method is used to display typing status to user.
     *
     * @param show is boolean, If it is true then <b>is Typing</b> will be shown to user
     *             If it is false then it will show user status i.e online or offline.
     */
    private void typingIndicator(TypingIndicator typingIndicator, boolean show) {
        if (messageAdapter != null) {
            if (show) {
                if (typingIndicator.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER))
                    tvStatus.setText("is Typing...");
                else
                    tvStatus.setText(typingIndicator.getSender().getName() + " is Typing...");
            } else {
                if (typingIndicator.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                    tvStatus.setText(status);
                } else {
                    tvStatus.setText(String.format("%d Members, %d Online", totalMembers, onlineMembers));
                }
            }

        }
    }

    /**
     * This method is used to remove message listener
     *
     * @see CometChat#removeMessageListener(String)
     */
    private void removeMessageListener() {
        CometChat.removeMessageListener(TAG);
    }

    /**
     * This method is used to remove user presence listener
     *
     * @see CometChat#removeUserListener(String)
     */
    private void removeUserListener() {
        CometChat.removeUserListener(TAG);
    }


    @Override
    public void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        removeMessageListener();
        removeUserListener();
        removeGroupListener();
        sendTypingIndicator(true);
    }

    private void removeGroupListener() {
        CometChat.removeGroupListener(TAG);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        rvChatListView.removeItemDecoration(stickyHeaderDecoration);
        messageAdapter = null;
        messagesRequest = null;
        // checkOnGoingCall();
        fetchMessage();
        addMessageListener();

        if (messageActionFragment != null)
            messageActionFragment.dismiss();

        if (type != null) {
            if (type.equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                addUserListener();
                tvStatus.setText(status);
                new Thread(this::getUser).start();
            } else {
                addGroupListener();
                new Thread(this::getGroup).start();
                new Thread(this::getMember).start();
            }
        }
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.iv_reply_close) {
            if (messageAdapter != null) {
                messageAdapter.clearLongClickSelectedItem();
                messageAdapter.notifyDataSetChanged();
            }
            isReply = false;
            baseMessage = null;
            replyMessageLayout.setVisibility(GONE);
        } else if (id == R.id.chatList_toolbar) {
            Intent intent = new Intent(this, CometChatGroupDetailScreenActivity.class);
            intent.putExtra(StringContract.IntentStrings.GUID, Id);
            intent.putExtra(StringContract.IntentStrings.NAME, name);
            intent.putExtra(StringContract.IntentStrings.AVATAR, avatarUrl);
            intent.putExtra(StringContract.IntentStrings.TYPE, type);
            intent.putExtra(StringContract.IntentStrings.GROUP_TYPE, groupType);
            intent.putExtra(StringContract.IntentStrings.MEMBER_SCOPE, loggedInUserScope);
            intent.putExtra(StringContract.IntentStrings.GROUP_OWNER, groupOwnerId);
            intent.putExtra(StringContract.IntentStrings.MEMBER_COUNT, totalMembers);
            intent.putExtra(StringContract.IntentStrings.GROUP_DESC, groupDesc);
            intent.putExtra(StringContract.IntentStrings.GROUP_PASSWORD, groupPassword);
            startActivity(intent);
        } else if (id == R.id.iv_close_message_action) {
            onBackPressed();
        }
    }

    @Override
    public void setLongMessageClick(List<BaseMessage> baseMessagesList) {
        Log.e(TAG, "setLongMessageClick: " + baseMessagesList);
        isReply = false;
        isEdit = false;
        messageActionFragment = new MessageActionFragment();
        replyMessageLayout.setVisibility(GONE);
        boolean shareVisible = true;
        boolean copyVisible = true;
        boolean threadVisible = true;
        boolean replyVisible = true;
        boolean editVisible = true;
        boolean deleteVisible = true;
        boolean forwardVisible = true;
        boolean mapVisible = false;
        List<BaseMessage> textMessageList = new ArrayList<>();
        List<BaseMessage> mediaMessageList = new ArrayList<>();
        List<BaseMessage> locationMessageList = new ArrayList<>();
        List<BaseMessage> pollsMessageList = new ArrayList<>();
        for (BaseMessage baseMessage : baseMessagesList) {
            if (baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_TEXT)) {
                textMessageList.add(baseMessage);
            } else if (baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_IMAGE) ||
                    baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_VIDEO) ||
                    baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_FILE) ||
                    baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_AUDIO)) {
                mediaMessageList.add(baseMessage);
            } else if (baseMessage.getType().equals(StringContract.IntentStrings.LOCATION)) {
                locationMessageList.add(baseMessage);
            } else {
                pollsMessageList.add(baseMessage);
            }
        }
        if (textMessageList.size() == 1) {
            BaseMessage basemessage = textMessageList.get(0);
            if (basemessage != null && basemessage.getSender() != null) {
                if (!(basemessage instanceof Action) && basemessage.getDeletedAt() == 0) {
                    baseMessage = basemessage;
                    threadVisible = basemessage.getReplyCount() <= 0;
                    if (basemessage.getSender().getUid().equals(CometChat.getLoggedInUser().getUid())) {
                        deleteVisible = true;
                        editVisible = true;
                        forwardVisible = true;
                    } else {
                        editVisible = false;
                        forwardVisible = true;
                        deleteVisible = loggedInUserScope != null && (loggedInUserScope.equals(CometChatConstants.SCOPE_ADMIN) || loggedInUserScope.equals(CometChatConstants.SCOPE_MODERATOR));
                    }
                }
            }
        }

        if (mediaMessageList.size() == 1) {
            BaseMessage basemessage = mediaMessageList.get(0);
            if (basemessage != null && basemessage.getSender() != null) {
                if (!(basemessage instanceof Action) && basemessage.getDeletedAt() == 0) {
                    baseMessage = basemessage;
                    threadVisible = basemessage.getReplyCount() <= 0;
                    copyVisible = false;
                    if (basemessage.getSender().getUid().equals(CometChat.getLoggedInUser().getUid())) {
                        deleteVisible = true;
                        editVisible = false;
                        forwardVisible = true;
                    } else {
                        deleteVisible = loggedInUserScope != null && (loggedInUserScope.equals(CometChatConstants.SCOPE_ADMIN) || loggedInUserScope.equals(CometChatConstants.SCOPE_MODERATOR));
                        forwardVisible = true;
                        editVisible = false;
                    }
                }
            }
        }
        baseMessages = baseMessagesList;
        Bundle bundle = new Bundle();
        bundle.putBoolean("copyVisible", copyVisible);
        bundle.putBoolean("threadVisible", threadVisible);
        bundle.putBoolean("shareVisible", shareVisible);
        bundle.putBoolean("editVisible", editVisible);
        bundle.putBoolean("deleteVisible", deleteVisible);
        bundle.putBoolean("replyVisible", replyVisible);
        bundle.putBoolean("forwardVisible", forwardVisible);
        bundle.putBoolean("mapVisible", mapVisible);
        if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_GROUP) &&
                baseMessage.getSender().getUid().equals(loggedInUser.getUid()))
            bundle.putBoolean("messageInfoVisible", true);
        bundle.putString("type", CometChatMessageListActivity.class.getName());
        messageActionFragment.setArguments(bundle);
        messageActionFragment.show(getSupportFragmentManager(), messageActionFragment.getTag());
        messageActionFragment.setMessageActionListener(new MessageActionFragment.MessageActionListener() {

            @Override
            public void onThreadMessageClick() {

            }

            @Override
            public void onEditMessageClick() {

            }

            @Override
            public void onReplyMessageClick() {
                replyMessage();
            }

            @Override
            public void onForwardMessageClick() {

            }

            @Override
            public void onDeleteMessageClick() {

            }

            @Override
            public void onCopyMessageClick() {
                String message = "";
                for (BaseMessage bMessage : baseMessages) {
                    if (bMessage.getDeletedAt() == 0 && bMessage instanceof TextMessage) {
                        message = message + "[" + Utils.getLastMessageDate(bMessage.getSentAt()) + "] " + bMessage.getSender().getName() + ": " + ((TextMessage) bMessage).getText();
                    }
                }
                Log.e(TAG, "onCopy: " + message);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("MessageAdapter", message);
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(CometChatMessageListActivity.this, getResources().getString(R.string.text_copied_clipboard), Toast.LENGTH_LONG).show();
                if (messageAdapter != null) {
                    messageAdapter.clearLongClickSelectedItem();
                    messageAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onShareMessageClick() {

            }

            @Override
            public void onMessageInfoClick() {

            }
        });
    }


    private void replyMessage() {
        if (baseMessage != null) {
            isReply = true;
            replyTitle.setText(baseMessage.getSender().getName());
            replyMedia.setVisibility(View.VISIBLE);
            if (baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_TEXT)) {
                replyMessage.setText(((TextMessage) baseMessage).getText());
                replyMedia.setVisibility(GONE);
            } else if (baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_AUDIO)) {
                String messageStr = String.format(getResources().getString(R.string.shared_a_audio),
                        Utils.getFileSize(((MediaMessage) baseMessage).getAttachment().getFileSize()));
                replyMessage.setText(messageStr);
                replyMessage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_library_music_24dp, 0, 0, 0);
            } else if (baseMessage.getType().equals(CometChatConstants.MESSAGE_TYPE_FILE)) {
                String messageStr = String.format(getResources().getString(R.string.shared_a_file),
                        Utils.getFileSize(((MediaMessage) baseMessage).getAttachment().getFileSize()));
                replyMessage.setText(messageStr);
                replyMessage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_insert_drive_file_black_24dp, 0, 0, 0);
            }
            composeBox.ivMic.setVisibility(View.VISIBLE);
            composeBox.ivSend.setVisibility(GONE);
            replyMessageLayout.setVisibility(View.VISIBLE);
            if (messageAdapter != null) {
                messageAdapter.setSelectedMessage(baseMessage.getId());
                messageAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void handleDialogClose(DialogInterface dialog) {
        if (messageAdapter != null)
            messageAdapter.clearLongClickSelectedItem();
        dialog.dismiss();
    }

    @Override
    public void onProgressUpdate(long progress) {

    }

    @Override
    public void onDurationUpdate(@org.jetbrains.annotations.Nullable Long duration) {

    }

    @Override
    public void onPlayerPause() {

    }

    @Override
    public void onPlayerResume() {

    }

    @Override
    public void onCurrentTimeUpdated(long lastPosition) {

    }

    @Override
    public void onTrackChange(@org.jetbrains.annotations.Nullable String tag) {

    }

    @Override
    public void onPositionDiscontinuity(long lastPos, int reason) {

    }

    @Override
    public void onPlayerReleased() {

    }

    @Override
    public void onPlayerEmptyTrack() {

    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void complete() {
        audioPlayerManager.seekTo(0);
        audioPlayerManager.onPause();
        setPlayProgress(0);
    }

    private void setPlayProgress(int progress) {
        if (currentAudioPosition != -1) {
            messageAdapter.notifyItemChanged(currentAudioPosition);
        }
    }
}

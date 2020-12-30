package com.joshtalks.joshskills.ui.groupchat.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cometchat.pro.constants.CometChatConstants;
import com.cometchat.pro.core.Call;
import com.cometchat.pro.core.CometChat;
import com.cometchat.pro.models.Action;
import com.cometchat.pro.models.BaseMessage;
import com.cometchat.pro.models.MediaMessage;
import com.cometchat.pro.models.MessageReceipt;
import com.cometchat.pro.models.TextMessage;
import com.cometchat.pro.models.User;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.core.datetimeutils.DateTimeUtils;
import com.joshtalks.joshskills.ui.groupchat.constant.StringContract;
import com.joshtalks.joshskills.ui.groupchat.listeners.OnRepliedMessageClick;
import com.joshtalks.joshskills.ui.groupchat.listeners.StickyHeaderAdapter;
import com.joshtalks.joshskills.ui.groupchat.messagelist.CometChatMessageListActivity;
import com.joshtalks.joshskills.ui.groupchat.uikit.AudioV2PlayerView;
import com.joshtalks.joshskills.ui.groupchat.uikit.Avatar;
import com.joshtalks.joshskills.ui.groupchat.utils.Extensions;
import com.joshtalks.joshskills.ui.groupchat.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Purpose - MessageAdapter is a subclass of RecyclerView Adapter which is used to display
 * the list of messages. It helps to organize the messages based on its type i.e TextMessage,
 * MediaMessage, Actions. It also helps to manage whether message is sent or recieved and organizes
 * view based on it. It is single adapter used to manage group messages and user messages.
 * <p>
 * Created on - 20th December 2019
 * <p>
 * Modified on  - 23rd March 2020
 */


public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements StickyHeaderAdapter<MessageAdapter.DateItemHolder> {

    private static final int RIGHT_IMAGE_MESSAGE = 56;

    private static final int LEFT_IMAGE_MESSAGE = 89;

    private static final int RIGHT_VIDEO_MESSAGE = 78;

    private static final int LEFT_VIDEO_MESSAGE = 87;

    private static final int RIGHT_AUDIO_MESSAGE = 39;

    private static final int LEFT_AUDIO_MESSAGE = 93;

    private static final int CALL_MESSAGE = 234;
    private static final int LEFT_TEXT_MESSAGE = 1;
    private static final int RIGHT_TEXT_MESSAGE = 2;
    private static final int RIGHT_REPLY_TEXT_MESSAGE = 987;
    private static final int LEFT_REPLY_TEXT_MESSAGE = 789;
    private static final int RIGHT_FILE_MESSAGE = 23;
    private static final int LEFT_FILE_MESSAGE = 25;
    private static final int ACTION_MESSAGE = 99;
    private static final int RIGHT_LINK_MESSAGE = 12;
    private static final int LEFT_LINK_MESSAGE = 13;
    private static final int LEFT_DELETE_MESSAGE = 551;
    private static final int RIGHT_DELETE_MESSAGE = 552;
    private static final int RIGHT_CUSTOM_MESSAGE = 432;
    private static final int LEFT_CUSTOM_MESSAGE = 431;
    private static final int LEFT_LOCATION_CUSTOM_MESSAGE = 31;
    private static final int RIGHT_LOCATION_CUSTOM_MESSAGE = 32;
    private static final int RIGHT_POLLS_CUSTOM_MESSAGE = 41;
    private static final int LEFT_POLLS_CUSTOM_MESSAGE = 42;
    private final List<BaseMessage> messageList = new ArrayList<>();
    private final User loggedInUser = CometChat.getLoggedInUser();
    private final List<Integer> selectedItemList = new ArrayList<>();
    private final String TAG = "MessageAdapter";
    public Context context;
    public List<BaseMessage> longselectedItemList = new ArrayList<>();
    private boolean isLongClickEnabled;
    //    private OnMessageLongClick messageLongClick;
    private OnRepliedMessageClick repliedMessageClickListener;
    private boolean isUserDetailVisible;
    private boolean isTextMessageClick;
    private boolean isImageMessageClick;

    /**
     * It is used to initialize the adapter wherever we needed. It has parameter like messageList
     * which contains list of messages and it will be used in adapter and paramter type is a String
     * whose values will be either CometChatConstants.RECEIVER_TYPE_USER
     * CometChatConstants.RECEIVER_TYPE_GROUP.
     *
     * @param context     is a object of Context.
     * @param messageList is a list of messages used in this adapter.
     */
    public MessageAdapter(CometChatMessageListActivity context, List<BaseMessage> messageList) {
        setMessageList(messageList);
        this.context = context;
//        try {
//            messageLongClick = context;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        try {
            repliedMessageClickListener = context;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is used to return the different view types to adapter based on item position.
     * It uses getItemViewTypes() method to identify the view type of item.
     *
     * @param position is a position of item in recyclerView.
     * @return It returns int which is value of view type of item.
     * @see MessageAdapter#getItemViewTypes(int)
     * *
     * @see MessageAdapter#onCreateViewHolder(ViewGroup, int)
     */
    @Override
    public int getItemViewType(int position) {
        return getItemViewTypes(position);
    }

    public List<BaseMessage> getMessageList() {
        return messageList;
    }

    private void setMessageList(List<BaseMessage> messageList) {
        for (int i = 0; i < messageList.size(); i++) {
            boolean isVisibleToLoggedInUser = isMessageVisible(messageList.get(i));
            if (!isVisibleToLoggedInUser) {
                messageList.remove(messageList.get(i));
            }
        }
        this.messageList.addAll(0, messageList);
        notifyItemRangeInserted(0, messageList.size());
    }

    private boolean isMessageVisible(BaseMessage message) {
        boolean isVisibleToLoggedInUser = true;
        if (message.getMetadata() != null && message.getMetadata().has("onlyVisibleTo")) {
            String userId = null;
            try {
                userId = message.getMetadata().getString("onlyVisibleTo");
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
            if (userId != null && !(CometChat.getLoggedInUser().getUid().equals(message.getSender().getUid()) || CometChat.getLoggedInUser().getUid().equals(userId))) {
                isVisibleToLoggedInUser = false;
            }
        }
        return isVisibleToLoggedInUser;
    }

    /**
     * This method is used to inflate the view for item based on its viewtype.
     * It helps to differentiate view for different type of messages.
     * Based on view type it returns various ViewHolder
     * Ex :- For MediaMessage it will return ImageMessageViewHolder,
     * For TextMessage it will return TextMessageViewHolder,etc.
     *
     * @param parent is a object of ViewGroup.
     * @param i      is viewType based on it various view will be inflated by adapter for various type
     *               of messages.
     * @return It return different ViewHolder for different viewType.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {

        View view;
        switch (i) {
            case LEFT_DELETE_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.left_message_item, parent, false);
                view.setTag(LEFT_DELETE_MESSAGE);
                return new DeleteMessageViewHolder(view);
            case RIGHT_DELETE_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.right_message_item, parent, false);
                view.setTag(RIGHT_DELETE_MESSAGE);
                return new DeleteMessageViewHolder(view);
            case LEFT_TEXT_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.left_message_item, parent, false);
                view.setTag(LEFT_TEXT_MESSAGE);
                return new TextMessageViewHolder(view);

            case RIGHT_TEXT_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.right_message_item, parent, false);
                view.setTag(RIGHT_TEXT_MESSAGE);
                return new TextMessageViewHolder(view);

            case LEFT_REPLY_TEXT_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.left_message_item, parent, false);
                view.setTag(LEFT_REPLY_TEXT_MESSAGE);
                return new TextMessageViewHolder(view);

            case RIGHT_REPLY_TEXT_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.right_message_item, parent, false);
                view.setTag(RIGHT_REPLY_TEXT_MESSAGE);
                return new TextMessageViewHolder(view);

            case RIGHT_LINK_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_right_link_item, parent, false);
                view.setTag(RIGHT_LINK_MESSAGE);
                return new LinkMessageViewHolder(view);

            case LEFT_LINK_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_left_link_item, parent, false);
                view.setTag(LEFT_LINK_MESSAGE);
                return new LinkMessageViewHolder(view);

            case RIGHT_AUDIO_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cometchat_audio_layout_right, parent, false);
                view.setTag(RIGHT_AUDIO_MESSAGE);
                return new AudioMessageViewHolder(view);

            case LEFT_AUDIO_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cometchat_audio_layout_left, parent, false);
                view.setTag(LEFT_AUDIO_MESSAGE);
                return new AudioMessageViewHolder(view);

            case ACTION_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cometchat_action_message, parent, false);
                view.setTag(ACTION_MESSAGE);
                return new ActionMessageViewHolder(view);

            case RIGHT_CUSTOM_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.right_message_item, parent, false);
                view.setTag(RIGHT_TEXT_MESSAGE);
                return new CustomMessageViewHolder(view);

            case LEFT_CUSTOM_MESSAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.left_message_item, parent, false);
                view.setTag(RIGHT_TEXT_MESSAGE);
                return new CustomMessageViewHolder(view);

            default:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.right_message_item, parent, false);
                view.setTag(-1);
                return new TextMessageViewHolder(view);
        }
    }


    /**
     * This method is used to bind the various ViewHolder content with their respective view types.
     * Here different methods are being called for different view type and in each method different
     * ViewHolder are been passed as parameter along with position of item.
     * <p>
     * Ex :- For TextMessage <code>setTextData((TextMessageViewHolder)viewHolder,i)</code> is been called.
     * where <b>viewHolder</b> is casted as <b>TextMessageViewHolder</b> and <b>i</b> is position of item.
     *
     * @param viewHolder is a object of RecyclerViewHolder.
     * @param i          is position of item in recyclerView.
     * @see MessageAdapter#setTextData(TextMessageViewHolder, int)
     * @see MessageAdapter#setActionData(ActionMessageViewHolder, int)
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        BaseMessage baseMessage = messageList.get(i);
        BaseMessage nextMessage = null, prevMessage = null;
        boolean isNextMessage = false, isPreviousMessage = false, isNextActionMessage = false;
        if ((i + 1) < messageList.size()) {
            if (messageList.get(i + 1).getSender() != null)
                nextMessage = messageList.get(i + 1);
        }

        if ((i - 1) >= 0) {
            if (messageList.get(i - 1).getSender() != null)
                prevMessage = messageList.get(i - 1);
        }

        isNextActionMessage = (nextMessage != null && (nextMessage.getCategory().equals(CometChatConstants.CATEGORY_ACTION) || nextMessage.getCategory().equals(CometChatConstants.CATEGORY_CALL)));
        isNextMessage = (nextMessage != null && baseMessage.getSender().getUid().equals(nextMessage.getSender().getUid()));
        isPreviousMessage = (prevMessage != null && baseMessage.getSender().getUid().equals(prevMessage.getSender().getUid()));

        if (isPreviousMessage && !isNextMessage) {
            isUserDetailVisible = true;
        }

        if (isPreviousMessage && isNextMessage) {
            isUserDetailVisible = false;
        } else if (!isNextMessage && !isPreviousMessage) {
            isUserDetailVisible = true;
        } else if (!isPreviousMessage) {
            isUserDetailVisible = false;
        }
        if (isNextActionMessage) {
            isUserDetailVisible = true;
        }


        switch (viewHolder.getItemViewType()) {

            case LEFT_DELETE_MESSAGE:
                ((DeleteMessageViewHolder) viewHolder).ivUser.setVisibility(View.GONE);
            case RIGHT_DELETE_MESSAGE:
                setDeleteData((DeleteMessageViewHolder) viewHolder, i);
                break;
            case LEFT_TEXT_MESSAGE:
            case LEFT_REPLY_TEXT_MESSAGE:
                ((TextMessageViewHolder) viewHolder).ivUser.setVisibility(View.GONE);
            case RIGHT_TEXT_MESSAGE:
            case RIGHT_REPLY_TEXT_MESSAGE:
                setTextData((TextMessageViewHolder) viewHolder, i);
                break;
            case LEFT_LINK_MESSAGE:
            case RIGHT_LINK_MESSAGE:
                setLinkData((LinkMessageViewHolder) viewHolder, i);
                break;
            case LEFT_AUDIO_MESSAGE:
            case RIGHT_AUDIO_MESSAGE:
                setAudioData((AudioMessageViewHolder) viewHolder, i);
                break;
            case ACTION_MESSAGE:
            case CALL_MESSAGE:
                setActionData((ActionMessageViewHolder) viewHolder, i);
                break;
            case LEFT_CUSTOM_MESSAGE:
                ((CustomMessageViewHolder) viewHolder).ivUser.setVisibility(View.GONE);
            case RIGHT_CUSTOM_MESSAGE:
                setCustomData((CustomMessageViewHolder) viewHolder, i);
                break;


        }
    }

    /**
     * This method is called whenever viewType of item is file. It is used to bind AudioMessageViewHolder
     * contents with MediaMessage at a given position.
     *
     * @param viewHolder is a object of AudioMessageViewHolder.
     * @param i          is a position of item in recyclerView.
     * @see MediaMessage
     * @see BaseMessage
     */
    private void setAudioData(AudioMessageViewHolder viewHolder, int i) {
        BaseMessage baseMessage = messageList.get(i);
        if (baseMessage != null && baseMessage.getDeletedAt() == 0) {
            viewHolder.view.setTag(baseMessage.getId());
            viewHolder.dummyView.setVisibility(View.VISIBLE);
            if (viewHolder.imgDeliveryTick != null) {
                if (baseMessage.getSentAt() == 0) {
                    viewHolder.imgDeliveryTick.setImageResource(R.drawable.ic_sent_message_s_tick);
                } else {
                    viewHolder.imgDeliveryTick.setImageResource(R.drawable.ic_sent_message_d_tick);
                }
            }
            if (!baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                    viewHolder.tvUser.setVisibility(View.GONE);
                    viewHolder.ivUser.setVisibility(View.GONE);
                    viewHolder.dummyView.setVisibility(View.VISIBLE);
                } else if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_GROUP)) {
                    if (isUserDetailVisible) {
                        viewHolder.tvUser.setVisibility(View.VISIBLE);
                        viewHolder.ivUser.setVisibility(View.VISIBLE);
                        viewHolder.dummyView.setVisibility(View.GONE);
                        viewHolder.rlMessageBubble.setBackground(ContextCompat.getDrawable(context, R.drawable.incoming_message_normal_bg_groupchat));
                    } else {
                        viewHolder.tvUser.setVisibility(View.GONE);
                        viewHolder.ivUser.setVisibility(View.INVISIBLE);
                        viewHolder.dummyView.setVisibility(View.VISIBLE);
                        viewHolder.rlMessageBubble.setBackground(ContextCompat.getDrawable(context, R.drawable.incoming_message_same_bg_groupchat));
                    }
                    String colorCode = null;
                    try {
                        if (baseMessage.getSender().getMetadata() != null && baseMessage.getSender().getMetadata().has("color_code")) {
                            colorCode = baseMessage.getSender().getMetadata().getString("color_code");
                        }
                    } catch (JSONException exception) {
                        exception.printStackTrace();
                    }
                    if (colorCode != null) {
                        viewHolder.tvUser.setTextColor(Color.parseColor(colorCode));
                    } else {
                        viewHolder.tvUser.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    }
                    setAvatar(viewHolder.ivUser, baseMessage.getSender().getAvatar(), baseMessage.getSender().getName(), colorCode);
                    viewHolder.tvUser.setText(baseMessage.getSender().getName());
                }
            } else {
                if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_GROUP)) {
                    if (isUserDetailVisible) {
                        viewHolder.rlMessageBubble.setBackground(ContextCompat.getDrawable(context, R.drawable.outgoing_message_normal_bg_groupchat));
                    } else {
                        viewHolder.rlMessageBubble.setBackground(ContextCompat.getDrawable(context, R.drawable.outgoing_message_same_bg_groupchat));
                    }
                }
            }

            if (baseMessage.getMetadata() != null && baseMessage.getMetadata().has("reply")) {
                try {
                    JSONObject metaData = baseMessage.getMetadata().getJSONObject("reply");
                    String messageType = metaData.getString("type");
                    String message = metaData.getString("message");
                    viewHolder.replyLayout.setVisibility(View.VISIBLE);
                    viewHolder.dummyView.setVisibility(View.VISIBLE);
                    String replyUserName = metaData.getString("name");
                    String colorCode = null;
                    if (metaData.has("color_code")) {
                        colorCode = metaData.getString("color_code");
                    }
                    if (colorCode != null) {
                        viewHolder.replyUser.setTextColor(Color.parseColor(colorCode));
                        viewHolder.indicatorView.setBackgroundColor(Color.parseColor(colorCode));
                    } else {
                        viewHolder.replyUser.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                        viewHolder.indicatorView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    }
                    if (replyUserName.equals(loggedInUser.getName())) {
                        viewHolder.replyUser.setText(context.getString(R.string.you));
                    } else {
                        viewHolder.replyUser.setText(replyUserName);
                    }
                    if (messageType.equals(CometChatConstants.MESSAGE_TYPE_TEXT)) {
                        viewHolder.replyMessage.setText(message);
                        viewHolder.replyMessage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    } else if (messageType.equals(CometChatConstants.MESSAGE_TYPE_AUDIO)) {
                        viewHolder.replyMessage.setText(String.format(context.getResources().getString(R.string.shared_a_audio), ""));
                        if (metaData.has("audioDurationInMs")) {
                            long audioDurationInMs = metaData.getLong("audioDurationInMs");
                            viewHolder.replyMessage.setText(String.format(context.getResources().getString(R.string.shared_a_audio), "(" + audioDurationInMs + ")"));
                        }
                        viewHolder.replyMessage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mic_grey, 0, 0, 0);
                    }
                    viewHolder.replyLayout.setOnClickListener(view -> {
                        if (metaData.has("id")) {
                            try {
                                repliedMessageClickListener.onRepliedMessageClick(metaData.getInt("id"));
                            } catch (JSONException exception) {
                                exception.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "setTextData: " + e.getMessage());
                }
            }

            if (baseMessage.getReplyCount() != 0) {
                viewHolder.tvThreadReplyCount.setVisibility(View.VISIBLE);
                viewHolder.tvThreadReplyCount.setText(baseMessage.getReplyCount() + " Replies");
            } else {
                viewHolder.lvReplyAvatar.setVisibility(View.GONE);
                viewHolder.tvThreadReplyCount.setVisibility(View.GONE);
            }

            showMessageTime(viewHolder, baseMessage);

            viewHolder.audioV2PlayerView.bindView(baseMessage.getId(), ((MediaMessage) baseMessage).getAttachment().getFileUrl(), baseMessage.getMetadata());
            viewHolder.audioV2PlayerView.setThemeColor(R.color.grey_68);
            viewHolder.txtTime.setVisibility(View.VISIBLE);
            viewHolder.rlMessageBubble.setOnLongClickListener(v -> {
                if (!isLongClickEnabled && !isTextMessageClick) {
                    isImageMessageClick = true;
                    setLongClickSelectedItem(baseMessage);
//                    messageLongClick.setLongMessageClick(longselectedItemList);
                    notifyDataSetChanged();
                }
                return true;
            });

            if (viewHolder.ivUser != null)
                viewHolder.ivUser.setOnClickListener(view -> Utils.moveToUserProfile(baseMessage.getSender().getUid(), context));

            if (viewHolder.tvUser != null)
                viewHolder.tvUser.setOnClickListener(view -> Utils.moveToUserProfile(baseMessage.getSender().getUid(), context));
        }
    }

    private void setDeleteData(DeleteMessageViewHolder viewHolder, int i) {
        BaseMessage baseMessage = messageList.get(i);
        if (baseMessage != null) {
            viewHolder.view.setTag(baseMessage.getId());
            if (!baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                    viewHolder.tvUser.setVisibility(View.GONE);
                    viewHolder.ivUser.setVisibility(View.GONE);
                } else if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_GROUP)) {
                    if (isUserDetailVisible) {
                        viewHolder.tvUser.setVisibility(View.VISIBLE);
                        viewHolder.ivUser.setVisibility(View.VISIBLE);
                    } else {
                        viewHolder.tvUser.setVisibility(View.GONE);
                        viewHolder.ivUser.setVisibility(View.INVISIBLE);
                    }
                    String colorCode = null;
                    try {
                        if (baseMessage.getSender().getMetadata() != null && baseMessage.getSender().getMetadata().has("color_code")) {
                            colorCode = baseMessage.getSender().getMetadata().getString("color_code");
                        }
                    } catch (JSONException exception) {
                        exception.printStackTrace();
                    }
                    if (colorCode != null) {
                        viewHolder.tvUser.setTextColor(Color.parseColor(colorCode));
                    } else {
                        viewHolder.tvUser.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    }
                    setAvatar(viewHolder.ivUser, baseMessage.getSender().getAvatar(), baseMessage.getSender().getName(), colorCode);
                    viewHolder.tvUser.setText(baseMessage.getSender().getName());
                }
            }
            if (baseMessage.getDeletedAt() != 0) {
                viewHolder.tvThreadReplyCount.setVisibility(View.GONE);
                viewHolder.lvReplyAvatar.setVisibility(View.GONE);
                viewHolder.txtMessage.setText(R.string.message_deleted);
                viewHolder.txtMessage.setTextColor(context.getResources().getColor(R.color.dark_grey));
                viewHolder.txtMessage.setTypeface(null, Typeface.ITALIC);
            }
            showMessageTime(viewHolder, baseMessage);

//        if (selectedItemList.contains(baseMessage.getId()))
            viewHolder.txtTime.setVisibility(View.VISIBLE);
//        else
//            viewHolder.txtTime.setVisibility(View.GONE);
//
        }
    }


    /**
     * This method is called whenever viewType of item is Action. It is used to bind
     * ActionMessageViewHolder contents with Action at a given position. It shows action message
     * or call status based on message category
     *
     * @param viewHolder is a object of ActionMessageViewHolder.
     * @param i          is a position of item in recyclerView.
     * @see Action
     * @see Call
     * @see BaseMessage
     */
    private void setActionData(ActionMessageViewHolder viewHolder, int i) {
        BaseMessage baseMessage = messageList.get(i);
//        if (Utils.isDarkMode(context))
//            viewHolder.textView.setTextColor(context.getResources().getColor(R.color.textColorWhite));
//        else
//            viewHolder.textView.setTextColor(context.getResources().getColor(R.color.primaryTextColor));
        if (baseMessage instanceof Action)
            viewHolder.textView.setText(((Action) baseMessage).getMessage());
        else if (baseMessage instanceof Call) {
            Call call = ((Call) baseMessage);
            String callMessageText = "";
            boolean isMissed = false, isIncoming = false, isVideo = false;
            if (call.getCallStatus().equals(CometChatConstants.CALL_STATUS_INITIATED)) {
                callMessageText = call.getSender().getName() + " " + call.getCallStatus();
            } else if (call.getCallStatus().equals(CometChatConstants.CALL_STATUS_UNANSWERED) ||
                    call.getCallStatus().equals(CometChatConstants.CALL_STATUS_CANCELLED)) {
                callMessageText = context.getResources().getString(R.string.missed_call);
                isMissed = true;
            } else if (call.getCallStatus().equals(CometChatConstants.CALL_STATUS_REJECTED)) {
                callMessageText = context.getResources().getString(R.string.rejected_call);
            } else if (call.getCallStatus().equals(CometChatConstants.CALL_STATUS_ONGOING)) {
                callMessageText = context.getString(R.string.ongoing);
            } else if (call.getCallStatus().equals(CometChatConstants.CALL_STATUS_ENDED)) {
                callMessageText = context.getString(R.string.ended);
            } else {
                callMessageText = call.getCallStatus();
            }
            isIncoming = !((User) call.getCallInitiator()).getUid().equals(loggedInUser);

            if (call.getType().equals(CometChatConstants.CALL_TYPE_VIDEO)) {
                callMessageText = callMessageText + " " + context.getResources().getString(R.string.video_call);
                isVideo = true;
            } else {
                callMessageText = callMessageText + " " + context.getResources().getString(R.string.audio_call);
                isVideo = false;
            }
            viewHolder.textView.setText(callMessageText);
        }
    }

    /**
     * This method is used to show message time below message whenever we click on message.
     * Since we have different ViewHolder, we have to pass <b>txtTime</b> of each viewHolder to
     * <code>setStatusIcon(RecyclerView.ViewHolder viewHolder,BaseMessage baseMessage)</code>
     * along with baseMessage.
     *
     * @param viewHolder  is object of ViewHolder.
     * @param baseMessage is a object of BaseMessage.
     * @see MessageAdapter#setStatusIcon(TextView, BaseMessage)
     * *
     * @see BaseMessage
     */
    private void showMessageTime(RecyclerView.ViewHolder viewHolder, BaseMessage baseMessage) {

        if (viewHolder instanceof TextMessageViewHolder) {
            setStatusIcon(((TextMessageViewHolder) viewHolder).txtTime, baseMessage);
        } else if (viewHolder instanceof LinkMessageViewHolder) {
            setStatusIcon(((LinkMessageViewHolder) viewHolder).txtTime, baseMessage);
        } else if (viewHolder instanceof AudioMessageViewHolder) {
            setStatusIcon(((AudioMessageViewHolder) viewHolder).txtTime, baseMessage);
        }

    }

    /**
     * This method is used set message time i.e sentAt, deliveredAt & readAt in <b>txtTime</b>.
     * If sender of baseMessage is user then for user side messages it will show readAt, deliveredAt
     * time along with respective icon. For reciever side message it will show only deliveredAt time
     *
     * @param txtTime     is a object of TextView which will show time.
     * @param baseMessage is a object of BaseMessage used to identify baseMessage sender.
     * @see BaseMessage
     */
    private void setStatusIcon(TextView txtTime, BaseMessage baseMessage) {
        if (baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
            if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                if (baseMessage.getReadAt() != 0) {
                    txtTime.setText(Utils.getHeaderDate(baseMessage.getReadAt() * 1000));
                    txtTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_done_all_black_24dp, 0);
                    txtTime.setCompoundDrawablePadding(10);
                } else if (baseMessage.getDeliveredAt() != 0) {
                    txtTime.setText(Utils.getHeaderDate(baseMessage.getDeliveredAt() * 1000));
                    txtTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_done_all_black_24dp, 0);
                    txtTime.setCompoundDrawablePadding(10);
                } else {
                    txtTime.setText(Utils.getHeaderDate(baseMessage.getSentAt() * 1000));
                    txtTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_black_24dp, 0);
                    txtTime.setCompoundDrawablePadding(10);
                }
            } else {
                txtTime.setText(Utils.getHeaderDate(baseMessage.getSentAt() * 1000));
            }
        } else {
            txtTime.setText(Utils.getHeaderDate(baseMessage.getSentAt() * 1000));
        }
    }

    /**
     * This method is called whenever viewType of item is TextMessage. It is used to bind
     * TextMessageViewHolder content with TextMessage at given position.
     * It shows text of a message if deletedAt = 0 else it shows "message deleted"
     *
     * @param viewHolder is a object of TextMessageViewHolder.
     * @param i          is postion of item in recyclerView.
     * @see TextMessage
     * @see BaseMessage
     */
    private void setTextData(TextMessageViewHolder viewHolder, int i) {

        BaseMessage baseMessage = messageList.get(i);
        if (baseMessage != null && baseMessage.getDeletedAt() == 0) {
            viewHolder.view.setTag(baseMessage.getId());
            if (viewHolder.imgDeliveryTick != null) {
                if (baseMessage.getSentAt() == 0) {
                    viewHolder.imgDeliveryTick.setImageResource(R.drawable.ic_sent_message_s_tick);
                } else {
                    viewHolder.imgDeliveryTick.setImageResource(R.drawable.ic_sent_message_d_tick);
                }
            }
            if (!baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                    viewHolder.tvUser.setVisibility(View.GONE);
                    viewHolder.ivUser.setVisibility(View.GONE);
                } else if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_GROUP)) {
                    if (isUserDetailVisible) {
                        viewHolder.tvUser.setVisibility(View.VISIBLE);
                        viewHolder.ivUser.setVisibility(View.VISIBLE);
                        viewHolder.cardView.setBackground(ContextCompat.getDrawable(context, R.drawable.incoming_message_normal_bg_groupchat));
                    } else {
                        viewHolder.tvUser.setVisibility(View.GONE);
                        viewHolder.ivUser.setVisibility(View.INVISIBLE);
                        viewHolder.cardView.setBackground(ContextCompat.getDrawable(context, R.drawable.incoming_message_same_bg_groupchat));
                    }
                    String colorCode = null;
                    try {
                        if (baseMessage.getSender().getMetadata() != null && baseMessage.getSender().getMetadata().has("color_code")) {
                            colorCode = baseMessage.getSender().getMetadata().getString("color_code");
                        }
                    } catch (JSONException exception) {
                        exception.printStackTrace();
                    }
                    if (colorCode != null) {
                        viewHolder.tvUser.setTextColor(Color.parseColor(colorCode));
                    } else {
                        viewHolder.tvUser.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    }
                    setAvatar(viewHolder.ivUser, baseMessage.getSender().getAvatar(), baseMessage.getSender().getName(), colorCode);
                    viewHolder.tvUser.setText(baseMessage.getSender().getName());
                }
                boolean isSentimentNegative = Extensions.checkSentiment(baseMessage);
                if (isSentimentNegative) {
                    viewHolder.txtMessage.setVisibility(View.GONE);
                    viewHolder.sentimentVw.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.txtMessage.setVisibility(View.VISIBLE);
                    viewHolder.sentimentVw.setVisibility(View.GONE);
                }
                viewHolder.viewSentimentMessage.setOnClickListener(v -> {
                    AlertDialog.Builder sentimentAlert = new AlertDialog.Builder(context)
                            .setTitle(context.getResources().getString(R.string.sentiment_alert))
                            .setMessage(context.getResources().getString(R.string.sentiment_alert_message))
                            .setPositiveButton(context.getResources().getString(R.string.yes), (dialog, which) -> {
                                viewHolder.txtMessage.setVisibility(View.VISIBLE);
                                viewHolder.sentimentVw.setVisibility(View.GONE);
                            })
                            .setNegativeButton(context.getResources().getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
                    sentimentAlert.create().show();
                });
            } else {
                if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_GROUP)) {
                    if (isUserDetailVisible) {
                        viewHolder.cardView.setBackground(ContextCompat.getDrawable(context, R.drawable.outgoing_message_normal_bg_groupchat));
                    } else {
                        viewHolder.cardView.setBackground(ContextCompat.getDrawable(context, R.drawable.outgoing_message_same_bg_groupchat));
                    }
                }
            }
            if (baseMessage.getMetadata() != null && baseMessage.getMetadata().has("reply")) {
                try {
                    JSONObject metaData = baseMessage.getMetadata().getJSONObject("reply");
                    String messageType = metaData.getString("type");
                    String message = metaData.getString("message");
                    viewHolder.replyLayout.setVisibility(View.VISIBLE);
                    String replyUserName = metaData.getString("name");
                    String colorCode = null;
                    if (metaData.has("color_code")) {
                        colorCode = metaData.getString("color_code");
                    }
                    if (colorCode != null) {
                        viewHolder.replyUser.setTextColor(Color.parseColor(colorCode));
                        viewHolder.indicatorView.setBackgroundColor(Color.parseColor(colorCode));
                    } else {
                        viewHolder.replyUser.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                        viewHolder.indicatorView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    }
                    if (replyUserName.equals(loggedInUser.getName())) {
                        viewHolder.replyUser.setText(context.getString(R.string.you));
                    } else {
                        viewHolder.replyUser.setText(replyUserName);
                    }
                    if (messageType.equals(CometChatConstants.MESSAGE_TYPE_TEXT)) {
                        viewHolder.replyMessage.setText(message);
                        viewHolder.replyMessage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    } else if (messageType.equals(CometChatConstants.MESSAGE_TYPE_AUDIO)) {
                        String strMsg = String.format(context.getResources().getString(R.string.shared_a_audio), "");
                        if (metaData.has("audioDurationInMs")) {
                            long audioDurationInMs = metaData.getLong("audioDurationInMs");
                            strMsg = String.format(
                                    context.getResources().getString(R.string.shared_a_audio),
                                    "(" + com.joshtalks.joshskills.core.Utils.INSTANCE.formatDuration((int) audioDurationInMs) + ")"
                            );
                        }
                        viewHolder.replyMessage.setText(strMsg);
                        viewHolder.replyMessage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mic_grey, 0, 0, 0);
                    }
                    viewHolder.replyLayout.setOnClickListener(view -> {
                        if (metaData.has("id")) {
                            try {
                                repliedMessageClickListener.onRepliedMessageClick(metaData.getInt("id"));
                            } catch (JSONException exception) {
                                exception.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "setTextData: " + e.getMessage());
                }
            }

            if (baseMessage.getReplyCount() != 0) {
                viewHolder.tvThreadReplyCount.setVisibility(View.VISIBLE);
                viewHolder.tvThreadReplyCount.setText(baseMessage.getReplyCount() + " Replies");
            } else {
                viewHolder.lvReplyAvatar.setVisibility(View.GONE);
                viewHolder.tvThreadReplyCount.setVisibility(View.GONE);
            }

            String txtMessage = ((TextMessage) baseMessage).getText().trim();
            viewHolder.txtMessage.setText(txtMessage);
            String profanityFilter = Extensions.checkProfanityMessage(baseMessage);
            viewHolder.txtMessage.setText(profanityFilter);
            Utils.setHyperLinkSupport(context, viewHolder.txtMessage);

            viewHolder.txtMessage.setOnLongClickListener(view -> {
                if (!isImageMessageClick) {
                    isLongClickEnabled = true;
                    isTextMessageClick = true;
                    setLongClickSelectedItem(baseMessage);
//                    messageLongClick.setLongMessageClick(longselectedItemList);
                    notifyDataSetChanged();
                }
                return true;
            });

            viewHolder.rlMessageBubble.setOnLongClickListener(view -> {
                if (!isImageMessageClick) {
                    isLongClickEnabled = true;
                    isTextMessageClick = true;
                    setLongClickSelectedItem(baseMessage);
//                    messageLongClick.setLongMessageClick(longselectedItemList);
                    notifyDataSetChanged();
                }
                return true;
            });

            if (viewHolder.ivUser != null)
                viewHolder.ivUser.setOnClickListener(view -> Utils.moveToUserProfile(baseMessage.getSender().getUid(), context));

            if (viewHolder.tvUser != null)
                viewHolder.tvUser.setOnClickListener(view -> Utils.moveToUserProfile(baseMessage.getSender().getUid(), context));

//            if (baseMessage.getSender().getUid().equals(loggedInUser.getUid()))
//                viewHolder.txtMessage.setTextColor(context.getResources().getColor(R.color.textColorWhite));
//            else
//                viewHolder.txtMessage.setTextColor(context.getResources().getColor(R.color.primaryTextColor));

            showMessageTime(viewHolder, baseMessage);
//             if (messageList.get(messageList.size()-1).equals(baseMessage))
//             {
//                 selectedItemList.add(baseMessage.getId());
//             }
//             if (selectedItemList.contains(baseMessage.getId()))
            viewHolder.txtTime.setVisibility(View.VISIBLE);
//             else
//                 viewHolder.txtTime.setVisibility(View.GONE);

            // setColorFilter(baseMessage, viewHolder.cardView);

//             viewHolder.rlMessageBubble.setOnClickListener(view -> {
//                 if (isLongClickEnabled && !isImageMessageClick) {
//                     setLongClickSelectedItem(baseMessage);
//                     messageLongClick.setLongMessageClick(longselectedItemList);
//                 }
//                 else {
//                     setSelectedMessage(baseMessage.getId());
//                 }
//                 notifyDataSetChanged();
//
//             });

//             viewHolder.rlMessageBubble.setOnLongClickListener(new View.OnLongClickListener() {
//                 @Override
//                 public boolean onLongClick(View view) {
//                     if (!isImageMessageClick) {
//                         isLongClickEnabled = true;
//                         isTextMessageClick = true;
//                         setLongClickSelectedItem(baseMessage);
//                         messageLongClick.setLongMessageClick(longselectedItemList);
//                         notifyDataSetChanged();
//                     }
//                         return true;
//                 }
//             });
            viewHolder.itemView.setTag(R.string.message, baseMessage);
        }
    }


    private void setCustomData(CustomMessageViewHolder viewHolder, int i) {

        BaseMessage baseMessage = messageList.get(i);
        if (baseMessage != null && baseMessage.getDeletedAt() == 0) {
            viewHolder.view.setTag(baseMessage.getId());
            if (!baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                    viewHolder.tvUser.setVisibility(View.GONE);
                    viewHolder.ivUser.setVisibility(View.GONE);
                } else if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_GROUP)) {
                    if (isUserDetailVisible) {
                        viewHolder.tvUser.setVisibility(View.VISIBLE);
                        viewHolder.ivUser.setVisibility(View.VISIBLE);
                    } else {
                        viewHolder.tvUser.setVisibility(View.GONE);
                        viewHolder.ivUser.setVisibility(View.INVISIBLE);
                    }
                    String colorCode = null;
                    try {
                        if (baseMessage.getSender().getMetadata() != null && baseMessage.getSender().getMetadata().has("color_code")) {
                            colorCode = baseMessage.getSender().getMetadata().getString("color_code");
                        }
                    } catch (JSONException exception) {
                        exception.printStackTrace();
                    }
                    if (colorCode != null) {
                        viewHolder.tvUser.setTextColor(Color.parseColor(colorCode));
                    } else {
                        viewHolder.tvUser.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    }
                    setAvatar(viewHolder.ivUser, baseMessage.getSender().getAvatar(), baseMessage.getSender().getName(), colorCode);
                    viewHolder.tvUser.setText(baseMessage.getSender().getName());
                }
            }

            viewHolder.txtMessage.setText(context.getResources().getString(R.string.custom_message));
            if (baseMessage.getSender().getUid().equals(loggedInUser.getUid()))
                viewHolder.txtMessage.setTextColor(context.getResources().getColor(R.color.white));
            else
                viewHolder.txtMessage.setTextColor(context.getResources().getColor(R.color.black));

            showMessageTime(viewHolder, baseMessage);
            if (messageList.get(messageList.size() - 1).equals(baseMessage)) {
                selectedItemList.add(baseMessage.getId());
            }
//            if (selectedItemList.contains(baseMessage.getId()))
            viewHolder.txtTime.setVisibility(View.VISIBLE);
//            else
//                viewHolder.txtTime.setVisibility(View.GONE);

            viewHolder.rlMessageBubble.setOnClickListener(view -> {
                setSelectedMessage(baseMessage.getId());
                notifyDataSetChanged();

            });
            viewHolder.itemView.setTag(R.string.message, baseMessage);
        }
    }

    private void setLinkData(LinkMessageViewHolder viewHolder, int i) {

        BaseMessage baseMessage = messageList.get(i);

        String url = null;

        if (baseMessage != null && baseMessage.getDeletedAt() == 0) {
            viewHolder.view.setTag(baseMessage.getId());
            if (!baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                    viewHolder.tvUser.setVisibility(View.GONE);
                    viewHolder.ivUser.setVisibility(View.GONE);
                } else if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_GROUP)) {
                    if (isUserDetailVisible) {
                        viewHolder.tvUser.setVisibility(View.VISIBLE);
                        viewHolder.ivUser.setVisibility(View.VISIBLE);
                    } else {
                        viewHolder.tvUser.setVisibility(View.GONE);
                        viewHolder.ivUser.setVisibility(View.INVISIBLE);
                    }
                    String colorCode = null;
                    try {
                        if (baseMessage.getSender().getMetadata() != null && baseMessage.getSender().getMetadata().has("color_code")) {
                            colorCode = baseMessage.getSender().getMetadata().getString("color_code");
                        }
                    } catch (JSONException exception) {
                        exception.printStackTrace();
                    }
                    if (colorCode != null) {
                        viewHolder.tvUser.setTextColor(Color.parseColor(colorCode));
                    } else {
                        viewHolder.tvUser.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    }
                    setAvatar(viewHolder.ivUser, baseMessage.getSender().getAvatar(), baseMessage.getSender().getName(), colorCode);
                    viewHolder.tvUser.setText(baseMessage.getSender().getName());
                }
            }
            if (baseMessage.getDeletedAt() == 0) {
                HashMap<String, JSONObject> extensionList = Extensions.extensionCheck(baseMessage);
                if (extensionList != null) {
                    if (extensionList.containsKey("linkPreview")) {
                        JSONObject linkPreviewJsonObject = extensionList.get("linkPreview");
                        try {
                            String description = linkPreviewJsonObject.getString("description");
                            String image = linkPreviewJsonObject.getString("image");
                            String title = linkPreviewJsonObject.getString("title");
                            url = linkPreviewJsonObject.getString("url");
                            Log.e("setLinkData: ", baseMessage.toString() + "\n\n" + url + "\n" + description + "\n" + image);
                            viewHolder.linkTitle.setText(title);
                            viewHolder.linkSubtitle.setText(description);
                            Glide.with(context).load(Uri.parse(image)).timeout(1000).into(viewHolder.linkImg);
                            if (url.contains("youtu.be") || url.contains("youtube")) {
                                viewHolder.videoLink.setVisibility(View.VISIBLE);
                                viewHolder.linkVisit.setText(context.getResources().getString(R.string.view_on_youtube));
                            } else {
                                viewHolder.videoLink.setVisibility(View.GONE);
                                viewHolder.linkVisit.setText(context.getResources().getString(R.string.visit));
                            }
                            String messageStr = ((TextMessage) baseMessage).getText();
                            if (((TextMessage) baseMessage).getText().equals(url) || ((TextMessage) baseMessage).getText().equals(url + "/")) {
                                viewHolder.message.setVisibility(View.GONE);
                            } else {
                                viewHolder.message.setVisibility(View.VISIBLE);
                            }
                            viewHolder.message.setText(messageStr);
                        } catch (Exception e) {
                            Log.e("setLinkData: ", e.getMessage());
                        }
                    }
                }
            }

            if (baseMessage.getReplyCount() != 0) {
                viewHolder.tvThreadReplyCount.setVisibility(View.VISIBLE);
                viewHolder.tvThreadReplyCount.setText(baseMessage.getReplyCount() + " Replies");
            } else {
                viewHolder.lvReplyAvatar.setVisibility(View.GONE);
                viewHolder.tvThreadReplyCount.setVisibility(View.GONE);
            }


            Utils.setHyperLinkSupport(context, viewHolder.message);

            showMessageTime(viewHolder, baseMessage);
            String finalUrl = url;
            viewHolder.linkVisit.setOnClickListener(v -> {

                if (finalUrl != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(finalUrl));
                    context.startActivity(intent);
                }
            });
            viewHolder.txtTime.setVisibility(View.VISIBLE);
            viewHolder.rlMessageBubble.setOnClickListener(view -> {
                if (isLongClickEnabled && !isImageMessageClick) {
                    setLongClickSelectedItem(baseMessage);
//                    messageLongClick.setLongMessageClick(longselectedItemList);
                } else {
                    setSelectedMessage(baseMessage.getId());
                }
                notifyDataSetChanged();

            });
            viewHolder.rlMessageBubble.setOnLongClickListener(view -> {
                if (!isImageMessageClick) {
                    isLongClickEnabled = true;
                    isTextMessageClick = true;
                    setLongClickSelectedItem(baseMessage);
//                    messageLongClick.setLongMessageClick(longselectedItemList);
                    notifyDataSetChanged();
                }
                return true;
            });
            viewHolder.itemView.setTag(R.string.message, baseMessage);
        }
    }

    public void setSelectedMessage(Integer id) {
        if (selectedItemList.contains(id))
            selectedItemList.remove(id);
        else
            selectedItemList.add(id);
    }

    public void setLongClickSelectedItem(BaseMessage baseMessage) {


        if (longselectedItemList.contains(baseMessage))
            longselectedItemList.remove(baseMessage);
        else
            longselectedItemList.add(baseMessage);
    }

    /**
     * This method is used to set avatar of groupMembers to show in groupMessages. If avatar of
     * group member is not available then it calls <code>setInitials(String name)</code> to show
     * first two letter of group member name.
     *
     * @param avatar    is a object of Avatar
     * @param avatarUrl is a String. It is url of avatar.
     * @param name      is a String. It is a name of groupMember.
     * @see Avatar
     */
    private void setAvatar(Avatar avatar, String avatarUrl, String name, String bgColorCode) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(context).load(avatarUrl).into(avatar);
        } else {
            avatar.setInitials(name, bgColorCode);
        }
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public long getHeaderId(int var1) {

        BaseMessage baseMessage = messageList.get(var1);
        return Long.parseLong(Utils.getDateId(baseMessage.getSentAt() * 1000));
    }

    @Override
    public DateItemHolder onCreateHeaderViewHolder(ViewGroup var1) {
        View view = LayoutInflater.from(var1.getContext()).inflate(R.layout.cc_message_list_header,
                var1, false);

        return new DateItemHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(DateItemHolder var1, int var2, long var3) {
        BaseMessage baseMessage = messageList.get(var2);
        Date date = new Date(baseMessage.getSentAt() * 1000L);
        if (DateTimeUtils.isToday(date)) {
            var1.txtMessageDate.setText("Today");
        } else if (DateTimeUtils.isYesterday(date)) {
            var1.txtMessageDate.setText("Yesterday");
        } else {
            String formattedDate = Utils.getDate(date.getTime());
            var1.txtMessageDate.setText(formattedDate);
        }
    }

    /**
     * This method is used to maintain different viewType based on message category and type and
     * returns the different view types to adapter based on it.
     * <p>
     * Ex :- For message with category <b>CometChatConstants.CATEGORY_MESSAGE</b> and type
     * <b>CometChatConstants.MESSAGE_TYPE_TEXT</b> and message is sent by a <b>Logged-in user</b>,
     * It will return <b>RIGHT_TEXT_MESSAGE</b>
     *
     * @param position is a position of item in recyclerView.
     * @return It returns int which is value of view type of item.
     * @see MessageAdapter#onCreateViewHolder(ViewGroup, int)
     * @see BaseMessage
     */
    private int getItemViewTypes(int position) {
        BaseMessage baseMessage = messageList.get(position);
        HashMap<String, JSONObject> extensionList = Extensions.extensionCheck(baseMessage);
        if (baseMessage.getDeletedAt() == 0) {
            if (baseMessage.getCategory() != null && baseMessage.getCategory().equals(CometChatConstants.CATEGORY_MESSAGE)) {
                switch (baseMessage.getType()) {

                    case CometChatConstants.MESSAGE_TYPE_TEXT:
                        if (baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                            if (extensionList != null && extensionList.containsKey("linkPreview") && extensionList.get("linkPreview") != null)
                                return RIGHT_LINK_MESSAGE;
                            else if (baseMessage.getMetadata() != null && baseMessage.getMetadata().has("reply"))
                                return RIGHT_REPLY_TEXT_MESSAGE;
                            else
                                return RIGHT_TEXT_MESSAGE;
                        } else {
                            if (extensionList != null && extensionList.containsKey("linkPreview") && extensionList.get("linkPreview") != null)
                                return LEFT_LINK_MESSAGE;
                            else if (baseMessage.getMetadata() != null && baseMessage.getMetadata().has("reply"))
                                return LEFT_REPLY_TEXT_MESSAGE;
                            else
                                return LEFT_TEXT_MESSAGE;
                        }
                    case CometChatConstants.MESSAGE_TYPE_AUDIO:
                        if (baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                            return RIGHT_AUDIO_MESSAGE;
                        } else {
                            return LEFT_AUDIO_MESSAGE;
                        }
                    case CometChatConstants.MESSAGE_TYPE_IMAGE:
                        if (baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                            return RIGHT_IMAGE_MESSAGE;
                        } else {
                            return LEFT_IMAGE_MESSAGE;
                        }
                    case CometChatConstants.MESSAGE_TYPE_VIDEO:
                        if (baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                            return RIGHT_VIDEO_MESSAGE;
                        } else {
                            return LEFT_VIDEO_MESSAGE;
                        }
                    case CometChatConstants.MESSAGE_TYPE_FILE:
                        if (baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                            return RIGHT_FILE_MESSAGE;
                        } else {
                            return LEFT_FILE_MESSAGE;
                        }
                    default:
                        return -1;
                }
            } else {
                if (baseMessage.getCategory() != null && baseMessage.getCategory().equals(CometChatConstants.CATEGORY_ACTION)) {
                    return ACTION_MESSAGE;
                } else if (baseMessage.getCategory() != null && baseMessage.getCategory().equals(CometChatConstants.CATEGORY_CALL)) {
                    return CALL_MESSAGE;
                } else if (baseMessage.getCategory() != null && baseMessage.getCategory().equals(CometChatConstants.CATEGORY_CUSTOM)) {
                    if (baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                        if (baseMessage.getType().equalsIgnoreCase(StringContract.IntentStrings.LOCATION))
                            return RIGHT_LOCATION_CUSTOM_MESSAGE;
                        else if (baseMessage.getType().equalsIgnoreCase(StringContract.IntentStrings.POLLS))
                            return RIGHT_POLLS_CUSTOM_MESSAGE;
                        else
                            return RIGHT_CUSTOM_MESSAGE;
                    } else if (baseMessage.getType().equalsIgnoreCase(StringContract.IntentStrings.LOCATION))
                        return LEFT_LOCATION_CUSTOM_MESSAGE;
                    else if (baseMessage.getType().equalsIgnoreCase(StringContract.IntentStrings.POLLS))
                        return LEFT_POLLS_CUSTOM_MESSAGE;
                    else
                        return LEFT_CUSTOM_MESSAGE;
                }
            }
        } else {
            if (baseMessage.getSender().getUid().equals(loggedInUser.getUid())) {
                return RIGHT_DELETE_MESSAGE;
            } else {
                return LEFT_DELETE_MESSAGE;
            }
        }
        return -1;

    }

    /**
     * This method is used to update message list of adapter.
     *
     * @param baseMessageList is list of baseMessages.
     */
    public void updateList(List<BaseMessage> baseMessageList) {
        setMessageList(baseMessageList);
    }

    /**
     * This method is used to set real time delivery receipt of particular message in messageList
     * of adapter by updating message.
     *
     * @param messageReceipt is a object of MessageReceipt.
     * @see MessageReceipt
     */
    public void setDeliveryReceipts(MessageReceipt messageReceipt) {

        for (int i = messageList.size() - 1; i >= 0; i--) {
            BaseMessage baseMessage = messageList.get(i);
            if (baseMessage.getDeliveredAt() == 0) {
                int index = messageList.indexOf(baseMessage);
                messageList.get(index).setDeliveredAt(messageReceipt.getDeliveredAt());
            }
        }
        notifyDataSetChanged();
    }

    /**
     * This method is used to set real time read receipt of particular message in messageList
     * of adapter by updating message.
     *
     * @param messageReceipt is a object of MessageReceipt.
     * @see MessageReceipt
     */
    public void setReadReceipts(MessageReceipt messageReceipt) {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            BaseMessage baseMessage = messageList.get(i);
            if (baseMessage.getReadAt() == 0) {
                int index = messageList.indexOf(baseMessage);
                messageList.get(index).setReadAt(messageReceipt.getReadAt());
            }
        }

        notifyDataSetChanged();
    }

    /**
     * This method is used to add message in messageList when send by a user or when received in
     * real time.
     *
     * @param baseMessage is a object of BaseMessage. It is new message which will added.
     * @see BaseMessage
     */
    public void addMessage(BaseMessage baseMessage) {
        if (isMessageVisible(baseMessage) && !messageList.contains(baseMessage)) {
            messageList.add(baseMessage);
            selectedItemList.clear();
            notifyDataSetChanged();
        }
    }

    /**
     * This method is used to update previous message with new message in messageList of adapter.
     *
     * @param baseMessage is a object of BaseMessage. It is new message which will be updated.
     */
    public void setUpdatedMessage(BaseMessage baseMessage) {

        if (isMessageVisible(baseMessage) && messageList.contains(baseMessage)) {
            int index = messageList.indexOf(baseMessage);
            messageList.remove(baseMessage);
            messageList.add(index, baseMessage);
            notifyItemChanged(index);
        }
    }

    public void resetList() {
        messageList.clear();
        notifyDataSetChanged();
    }

    public void clearLongClickSelectedItem() {
        isLongClickEnabled = false;
        isTextMessageClick = false;
        isImageMessageClick = false;
        longselectedItemList.clear();
        notifyDataSetChanged();
    }

    public BaseMessage getLastMessage() {
        if (messageList.size() > 0) {
            Log.e(TAG, "getLastMessage: " + messageList.get(messageList.size() - 1));
            return messageList.get(messageList.size() - 1);
        } else
            return null;
    }

    public int getPosition(BaseMessage baseMessage) {
        return messageList.indexOf(baseMessage);
    }

    public class ActionMessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;

        public ActionMessageViewHolder(@NonNull View view) {
            super(view);
            int type = (int) view.getTag();
            textView = view.findViewById(R.id.go_txt_message);
        }
    }

    public class DeleteMessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtMessage;
        private final RelativeLayout cardView;
        private final View view;
        private final ImageView imgStatus;
        private final Avatar ivUser;
        private final RelativeLayout rlMessageBubble;
        private final TextView tvThreadReplyCount;
        private final LinearLayout lvReplyAvatar;
        private final TextView txtTime;
        private final TextView tvUser;

        DeleteMessageViewHolder(@NonNull View view) {
            super(view);

            tvUser = view.findViewById(R.id.tv_user);
            txtMessage = view.findViewById(R.id.go_txt_message);
            cardView = view.findViewById(R.id.cv_message_container);
            txtTime = view.findViewById(R.id.txt_time);
            imgStatus = view.findViewById(R.id.img_pending);
            ivUser = view.findViewById(R.id.iv_user);
            rlMessageBubble = view.findViewById(R.id.rl_message);
            tvThreadReplyCount = view.findViewById(R.id.thread_reply_count);
            lvReplyAvatar = view.findViewById(R.id.reply_avatar_layout);
            this.view = view;
        }
    }

    /**
     * This is TextMessageViewHolder which is used to handle TextMessage.
     */
    public class TextMessageViewHolder extends RecyclerView.ViewHolder {

        private final RelativeLayout rlMessageBubble;
        private final RelativeLayout cardView;
        private final View view;
        private final TextView txtMessage;            //Text Message
        private final TextView tvThreadReplyCount;    //Thread Reply Count
        private final ImageView imgStatus;
        private final Avatar ivUser;                  //sender avatar
        private final RelativeLayout sentimentVw;     //sentiment extension layout
        private final TextView viewSentimentMessage;  //sentiment extension text
        private final CardView replyLayout;     //reply message layout
        private final TextView replyUser;             //reply message sender name
        private final TextView replyMessage;          //reply message text
        private final View indicatorView;             //indicatorView
        private final LinearLayout lvReplyAvatar;
        private final TextView txtTime;                //Message Sent time.
        private final TextView tvUser;                 //sender name
        private final AppCompatImageView imgDeliveryTick;   //Delivery Tick

        TextMessageViewHolder(@NonNull View view) {
            super(view);

            tvUser = view.findViewById(R.id.tv_user);
            txtMessage = view.findViewById(R.id.go_txt_message);
            cardView = view.findViewById(R.id.cv_message_container);
            txtTime = view.findViewById(R.id.txt_time);
            imgStatus = view.findViewById(R.id.img_pending);
            ivUser = view.findViewById(R.id.iv_user);
            rlMessageBubble = view.findViewById(R.id.rl_message);
            replyLayout = view.findViewById(R.id.replyLayout);
            replyUser = view.findViewById(R.id.reply_user);
            replyMessage = view.findViewById(R.id.reply_message);
            indicatorView = view.findViewById(R.id.indicatorView);
            tvThreadReplyCount = view.findViewById(R.id.thread_reply_count);
            lvReplyAvatar = view.findViewById(R.id.reply_avatar_layout);
            sentimentVw = view.findViewById(R.id.sentiment_layout);
            viewSentimentMessage = view.findViewById(R.id.view_sentiment);
            imgDeliveryTick = view.findViewById(R.id.delivery_tick);
            this.view = view;

        }
    }

    public class CustomMessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtMessage;
        private final RelativeLayout cardView;
        private final View view;
        private final ImageView imgStatus;
        private final Avatar ivUser;
        private final RelativeLayout rlMessageBubble;
        private final TextView txtTime;
        private final TextView tvUser;


        CustomMessageViewHolder(@NonNull View view) {
            super(view);

            tvUser = view.findViewById(R.id.tv_user);
            txtMessage = view.findViewById(R.id.go_txt_message);
            cardView = view.findViewById(R.id.cv_message_container);
            txtTime = view.findViewById(R.id.txt_time);
            imgStatus = view.findViewById(R.id.img_pending);
            ivUser = view.findViewById(R.id.iv_user);
            rlMessageBubble = view.findViewById(R.id.rl_message);
            this.view = view;
        }
    }

    public class AudioMessageViewHolder extends RecyclerView.ViewHolder {

        private final View view;
        private final TextView tvUser;
        private final Avatar ivUser;
        private final RelativeLayout rlMessageBubble;
        private final TextView txtTime;
        private final TextView tvThreadReplyCount;
        private final LinearLayout lvReplyAvatar;
        private final AudioV2PlayerView audioV2PlayerView;
        private final CardView replyLayout;           //reply message layout
        private final TextView replyUser;             //reply message sender name
        private final TextView replyMessage;          //reply message text
        private final View indicatorView;             //indicatorView
        private final AppCompatImageView imgDeliveryTick;   //Delivery Tick
        private final View dummyView;                 //dummyView for Spacing


        public AudioMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            rlMessageBubble = itemView.findViewById(R.id.cv_message_container);
            tvUser = itemView.findViewById(R.id.tv_user);
            ivUser = itemView.findViewById(R.id.iv_user);
            txtTime = itemView.findViewById(R.id.txt_time);
            tvThreadReplyCount = itemView.findViewById(R.id.thread_reply_count);
            lvReplyAvatar = itemView.findViewById(R.id.reply_avatar_layout);
            audioV2PlayerView = itemView.findViewById(R.id.audio_player);
            replyLayout = itemView.findViewById(R.id.replyLayout);
            replyUser = itemView.findViewById(R.id.reply_user);
            replyMessage = itemView.findViewById(R.id.reply_message);
            indicatorView = itemView.findViewById(R.id.indicatorView);
            imgDeliveryTick = itemView.findViewById(R.id.delivery_tick);
            dummyView = itemView.findViewById(R.id.dummy_view);
            this.view = itemView;
        }
    }

    public class LinkMessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView linkTitle;
        private final TextView linkVisit;
        private final TextView linkSubtitle;
        private final TextView message;
        private final ImageView videoLink;
        private final RelativeLayout cardView;
        private final View view;
        private final ImageView imgStatus;
        private final ImageView linkImg;
        private final TextView tvUser;
        private final Avatar ivUser;
        private final RelativeLayout rlMessageBubble;
        private final TextView tvThreadReplyCount;
        private final LinearLayout lvReplyAvatar;
        private final TextView txtTime;

        LinkMessageViewHolder(@NonNull View view) {
            super(view);

            tvUser = view.findViewById(R.id.tv_user);
            linkTitle = view.findViewById(R.id.link_title);
            linkSubtitle = view.findViewById(R.id.link_subtitle);
            linkVisit = view.findViewById(R.id.visitLink);
            linkImg = view.findViewById(R.id.link_img);
            message = view.findViewById(R.id.message);
            videoLink = view.findViewById(R.id.videoLink);
            cardView = view.findViewById(R.id.cv_message_container);
            txtTime = view.findViewById(R.id.txt_time);
            imgStatus = view.findViewById(R.id.img_pending);
            ivUser = view.findViewById(R.id.iv_user);
            rlMessageBubble = view.findViewById(R.id.rl_message);
            tvThreadReplyCount = view.findViewById(R.id.thread_reply_count);
            lvReplyAvatar = view.findViewById(R.id.reply_avatar_layout);
            this.view = view;
        }
    }

    public class DateItemHolder extends RecyclerView.ViewHolder {

        TextView txtMessageDate;

        DateItemHolder(@NonNull View itemView) {
            super(itemView);
            txtMessageDate = itemView.findViewById(R.id.txt_message_date);
        }
    }

}

package com.tyagiabhinav.dialogflowchatlibrary;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.ProgressIndicator;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.EventInput;
import com.google.cloud.dialogflow.v2.Intent.Message;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.tyagiabhinav.dialogflowchatlibrary.networkutil.ChatbotCallback;
import com.tyagiabhinav.dialogflowchatlibrary.networkutil.TaskRunner;
import com.tyagiabhinav.dialogflowchatlibrary.pref.SharedPrefsManager;
import com.tyagiabhinav.dialogflowchatlibrary.templates.ButtonMessageTemplate;
import com.tyagiabhinav.dialogflowchatlibrary.templates.CardMessageTemplate;
import com.tyagiabhinav.dialogflowchatlibrary.templates.CarouselTemplate;
import com.tyagiabhinav.dialogflowchatlibrary.templates.CheckBoxMessageTemplate;
import com.tyagiabhinav.dialogflowchatlibrary.templates.HyperLinkTemplate;
import com.tyagiabhinav.dialogflowchatlibrary.templates.TextMessageTemplate;
import com.tyagiabhinav.dialogflowchatlibrary.templateutil.Constants;
import com.tyagiabhinav.dialogflowchatlibrary.templateutil.OnClickCallback;
import com.tyagiabhinav.dialogflowchatlibrary.templateutil.ReturnMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tyagiabhinav.dialogflowchatlibrary.pref.SharedPrefsManager.COURSE_TEST_IDS;

public class ChatbotActivity extends FragmentActivity implements ChatbotCallback, OnClickCallback {

    public static final String SESSION_ID = "sessionID";
    private static final String TAG = ChatbotActivity.class.getSimpleName();
    private static final int SPEECH_INPUT = 10070;
    //UI
    private LinearLayout chatLayout;
    private MaterialButton btnAction1;
    private MaterialButton btnAction2;
    private ProgressIndicator progressIndicator;

    //Variables
    private SessionsClient sessionsClient;
    private SessionName session;
    private TaskRunner dialogflowTaskRunner;
    private boolean isProgressRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chatbot);
        Log.d(TAG, "onCreate: ");

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.statusBarColor));

        final ScrollView scrollview = findViewById(R.id.chatScrollView);
        scrollview.post(new Runnable() {
            @Override
            public void run() {
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        chatLayout = findViewById(R.id.chatLayout);
        btnAction1 = findViewById(R.id.btnAction1);
        btnAction2 = findViewById(R.id.btnAction2);
        progressIndicator = findViewById(R.id.progressIndicator);

        findViewById(R.id.chat_back_iv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        btnAction1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Send click");
                if (btnAction1.getTag() == "EndOfFlow") {
                    returnToActivity();
                } else {
                    String msg = btnAction1.getText().toString();
                    sendMessage(msg);
                }
            }
        });

        btnAction2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Send click");
                String msg = btnAction2.getText().toString();
                sendMessage(msg);
            }
        });

        Bundle bundle = getIntent().getExtras();
        String sessionID = null;
        if (bundle != null) {
            sessionID = bundle.getString(SESSION_ID);
            if (sessionID == null || sessionID.trim().isEmpty()) {
                sessionID = UUID.randomUUID().toString();
            }
        }

        try {
            init(sessionID);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(ChatbotActivity.this, "Error creating a session!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        closeDialog();
    }

    @Override
    public void OnChatbotResponse(DetectIntentResponse response) {
        removeProcessWaitBubble();
        processResponse(response);

    }

    @Override
    public void OnUserClickAction(ReturnMessage msg) {
        String eventName = msg.getEventName();
        Struct param = msg.getParam();
        if (eventName != null && !eventName.trim().isEmpty()) {
            if (param != null && param.getFieldsCount() > 0) {
                Log.e("param123", param.toString());
                if (param.getFieldsMap().containsKey("selectedItems")
                        && param.getFieldsMap().get("selectedItems").getListValue().getValuesList().size() > 0) {
                    EventInput eventInput = EventInput.newBuilder().setName(eventName).setLanguageCode("en-US").setParameters(param).build();
                    send(eventInput, msg.getActionText());
                } else {
                    Toast.makeText(this, "Please select a value", Toast.LENGTH_SHORT).show();
                }
            } else {
                EventInput eventInput = EventInput.newBuilder().setName(eventName).setLanguageCode("en-US").build();
                send(eventInput, msg.getActionText());
            }
        } else {
            send(msg.getActionText(), true);
        }
    }

    private void init(String UUID) throws IOException {
        InputStream credentialStream = DialogflowCredentials.getInstance().getInputStream();
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialStream);
        String projectId = ((ServiceAccountCredentials) credentials).getProjectId();

        SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
        SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
        sessionsClient = SessionsClient.create(sessionsSettings);
        session = SessionName.of(projectId, UUID);

        if (ChatbotSettings.getInstance().isAutoWelcome()) {
            showProcessWaitBubble();
            send("hi", true);
        }
    }

    private void sendMessage(String msg) {
        if (msg.trim().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please enter your query!", Toast.LENGTH_LONG).show();
        } else {
            send(msg, true);
        }
    }

    private void send(String message, boolean showWaitBubble) {
        Log.d(TAG, "send: 1");
        TextMessageTemplate tmt = new TextMessageTemplate(getApplicationContext(), ChatbotActivity.this, Constants.USER);
        if (!ChatbotSettings.getInstance().isAutoWelcome()) {
            chatLayout.addView(tmt.showMessage(message));
            btnAction1.setVisibility(View.GONE);
            btnAction2.setVisibility(View.GONE);
            if (showWaitBubble) {
                showProcessWaitBubble();
            }
        } else {
            ChatbotSettings.getInstance().setAutoWelcome(false);
        }
        QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build();
        dialogflowTaskRunner = new TaskRunner(this, session, sessionsClient, queryInput);
        dialogflowTaskRunner.executeChat();
    }

    private void send(EventInput event, String message) {
        Log.d(TAG, "send: 2");
        TextMessageTemplate tmt = new TextMessageTemplate(getApplicationContext(), ChatbotActivity.this, Constants.USER);
        if (!ChatbotSettings.getInstance().isAutoWelcome()) {
            chatLayout.addView(tmt.showMessage(message));
            btnAction1.setVisibility(View.GONE);
            btnAction2.setVisibility(View.GONE);
            showProcessWaitBubble();
        } else {
            ChatbotSettings.getInstance().setAutoWelcome(false);
        }

        QueryInput queryInput = QueryInput.newBuilder().setEvent(event).build();
        dialogflowTaskRunner = new TaskRunner(this, session, sessionsClient, queryInput);
        dialogflowTaskRunner.executeChat();
    }

    private void showProcessWaitBubble() {
        TextMessageTemplate tmt = new TextMessageTemplate(getApplicationContext(), ChatbotActivity.this, Constants.BOT);
        chatLayout.addView(tmt.showMessage("....."));
        isProgressRunning = true;

    }

    private void removeProcessWaitBubble() {
        if (isProgressRunning && chatLayout != null && chatLayout.getChildCount() > 0) {
            chatLayout.removeViewAt(chatLayout.getChildCount() - 1);
            isProgressRunning = false;
        }
    }

    private void processResponse(DetectIntentResponse response) {
        Log.d(TAG, "processResponse");
        if (response != null) {
            Log.d("dialogflowres", response.toString());
            List<Context> contextList = response.getQueryResult().getOutputContextsList();
            if (!contextList.isEmpty()) {
                for (Context context : contextList) {
                    if (context.getName().contains("param_context")) {
                        Map<String, Value> paramMap = context.getParameters().getFieldsMap();
                        List<Value> msgTemplateList = paramMap.containsKey("messageTemplate") ? paramMap.get("messageTemplate").getListValue().getValuesList() : null;
                        if (msgTemplateList != null) {
                            for (Value msgItems : msgTemplateList) {
                                String template = msgItems.getStructValue().getFieldsMap().get("template").getStringValue();
                                switch (template) {
                                    case "text":
                                        Log.d(TAG, "processResponse: Text Template");
                                        TextMessageTemplate tmt = new TextMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
                                        chatLayout.addView(tmt.showMessage(response, msgItems.getStructValue().getFieldsMap())); // move focus to text view to automatically make it scroll up if softfocus
                                        break;
                                    case "button":
                                        Log.d(TAG, "processResponse: Button Template");
                                        ButtonMessageTemplate bmt = new ButtonMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
                                        chatLayout.addView(bmt.showMessage(response, msgItems.getStructValue().getFieldsMap())); // move focus to text view to automatically make it scroll up if softfocus
                                        btnAction1.setVisibility(View.GONE);
                                        btnAction2.setVisibility(View.GONE);
                                        break;
                                    case "hyperlink":
                                        Log.d(TAG, "processResponse: Hyperlink Template");
                                        HyperLinkTemplate blt = new HyperLinkTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
                                        chatLayout.addView(blt.showMessage(response, msgItems.getStructValue().getFieldsMap())); // move focus to text view to automatically make it scroll up if softfocus
                                        btnAction1.setVisibility(View.GONE);
                                        btnAction2.setVisibility(View.GONE);
                                        break;
                                    case "checkbox":
                                        Log.d(TAG, "processResponse: CheckBox Template");
                                        CheckBoxMessageTemplate cbmt = new CheckBoxMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
                                        chatLayout.addView(cbmt.showMessage(response, msgItems.getStructValue().getFieldsMap())); // move focus to text view to automatically make it scroll up if softfocus
                                        btnAction1.setVisibility(View.GONE);
                                        btnAction2.setVisibility(View.GONE);
                                        break;
                                    case "card":
                                        Log.d(TAG, "processResponse: Card Template");
                                        CardMessageTemplate cmt = new CardMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
                                        chatLayout.addView(cmt.showMessage(response, msgItems.getStructValue().getFieldsMap())); // move focus to text view to automatically make it scroll up if softfocus
                                        break;
                                    case "carousel":
                                        Log.d(TAG, "processResponse: Carousel Template");
                                        CarouselTemplate crt = new CarouselTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
                                        chatLayout.addView(crt.showMessage(response, msgItems.getStructValue().getFieldsMap())); // move focus to text view to automatically make it scroll up if softfocus
                                        break;
                                }
                            }
                        }
                        break;
                    } else {
                        // when no param context if found... go to default
                        TextMessageTemplate tmt = new TextMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
                        //if(response.hasQueryResult() && !response.getQueryResult().getFulfillmentText().isEmpty()) {
                        chatLayout.addView(tmt.showMessage(response));
                        //}
                    }
                }
            } else {
                // when no param context if found... go to default
                TextMessageTemplate tmt = new TextMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
                //if(response.hasQueryResult() && !response.getQueryResult().getFulfillmentText().isEmpty()) {
                chatLayout.addView(tmt.showMessage(response));
                //}
            }
            List<Message> fulfilmentMessages = response.getQueryResult().getFulfillmentMessagesList();
            for (Message message : fulfilmentMessages) {
                if (message.hasPayload() && message.getPayload().containsFields("suggestions")) {
                    List<Value> suggestionList = message.getPayload()
                            .getFieldsMap()
                            .get("suggestions")
                            .getListValue()
                            .getValuesList();


                    if (suggestionList.size() > 0) {
                        btnAction1.setText(suggestionList.get(0).getStringValue());
                        btnAction1.setVisibility(View.VISIBLE);
                        if (message.getPayload().getFieldsMap().get("isEndOfFlow").getBoolValue()) {
                            btnAction1.setTag("EndOfFlow");
                        }
                    }
                    if (suggestionList.size() > 1) {
                        btnAction2.setText(suggestionList.get(1).getStringValue());
                        btnAction2.setVisibility(View.VISIBLE);
                    }
                }
                if (message.hasPayload() && message.getPayload().containsFields("progressPercentage")) {
                    Double progressPercentage = message.getPayload()
                            .getFieldsMap()
                            .get("progressPercentage")
                            .getNumberValue();

                    progressIndicator.setProgress(progressPercentage.intValue());
                }
            }
        } else {
            Log.e(TAG, "processResponse: Null Response");
        }
    }

    private void closeDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ChatbotActivity.this);

        alertDialogBuilder.setTitle("Exit Chat?");
        alertDialogBuilder.setMessage("Do you want to exit the chat? You will loose this chat session.");

        alertDialogBuilder
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "clicked: YES");
                                dialog.cancel();
//                                destroyRequestDialogflowTask();
                                ChatbotSettings.getInstance().setAppToolbar(null);
                                ChatbotSettings.getInstance().setAutoWelcome(true);
//                                super.onBackPressed();
                                ChatbotActivity.this.finish();
                            }
                        })
                .setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "clicked: NO");
                                dialog.cancel();
                            }
                        })
                .create()
                .show();

    }

    private void returnToActivity() {
        ArrayList<Integer> courseIds = getCourseIds();
        Intent returnIntent = new Intent();
        returnIntent.putIntegerArrayListExtra("result", courseIds);
        setResult(1343, returnIntent);
        removeCourseIds();
        finish();
    }

    private ArrayList<Integer> getCourseIds() {
        return SharedPrefsManager.Companion.newInstance(getApplicationContext()).getArrayList(COURSE_TEST_IDS, "");
    }

    private void removeCourseIds() {
        SharedPrefsManager.Companion.newInstance(getApplicationContext()).putArrayList(COURSE_TEST_IDS, new ArrayList<>());
    }

}

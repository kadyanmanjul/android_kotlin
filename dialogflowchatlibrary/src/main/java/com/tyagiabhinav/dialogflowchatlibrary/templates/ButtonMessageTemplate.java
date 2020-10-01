package com.tyagiabhinav.dialogflowchatlibrary.templates;

import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.protobuf.Value;
import com.tyagiabhinav.dialogflowchatlibrary.templateutil.OnClickCallback;

import java.util.List;
import java.util.Map;

public class ButtonMessageTemplate extends MessageLayoutTemplate {

    private static final String TAG = ButtonMessageTemplate.class.getSimpleName();

    public ButtonMessageTemplate(Context context, OnClickCallback callback, int type) {
        super(context, callback, type);
        setOnlyTextResponse(false);
    }

    @Override
    FrameLayout populateRichMessageContainer() {
        FrameLayout richMessageContainer = getRichMessageContainer();
        DetectIntentResponse response = getResponse();
        List<com.google.cloud.dialogflow.v2.Context> contextList = response.getQueryResult().getOutputContextsList();

        LinearLayout btnLayout = getVerticalContainer();
//        btnLayout.setGravity(Gravity.CENTER);
        btnLayout.setFocusableInTouchMode(true);
        for (com.google.cloud.dialogflow.v2.Context context : contextList) {
            if (context.getName().contains("param_context")) {
                Map<String, Value> msgTemplate = getMsgTemplate();
                List<Value> buttonList = (msgTemplate.get("buttonItems") != null) ? msgTemplate.get("buttonItems").getListValue().getValuesList() : null;
                String align = (msgTemplate.get("align") != null) ? msgTemplate.get("align").getStringValue() : null;
                String sizeValue = (msgTemplate.get("size") != null) ? msgTemplate.get("size").getStringValue() : null;
                String eventName = (msgTemplate.get("eventToCall") != null) ? msgTemplate.get("eventToCall").getStringValue() : null;

                if (align.equalsIgnoreCase("horizontal") || align.equalsIgnoreCase("h")) {
                    btnLayout = getHorizontalContainer();
                }

                if (buttonList != null) {
                    for (Value item : buttonList) {
                        btnLayout.addView(getBtn("button", item.getStructValue().getFieldsMap(), sizeValue, eventName));
                    }
                }
            }
        }
        Log.d(TAG, "populateRichMessageContainer: btn layout count: " + btnLayout.getChildCount());
        richMessageContainer.addView(btnLayout);

        return richMessageContainer;
    }


}

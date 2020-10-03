package com.tyagiabhinav.dialogflowchatlibrary.templates;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager.widget.PagerAdapter;

import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.tyagiabhinav.dialogflowchatlibrary.R;
import com.tyagiabhinav.dialogflowchatlibrary.networkutil.TaskRunner;
import com.tyagiabhinav.dialogflowchatlibrary.templateutil.CarouselPager;
import com.tyagiabhinav.dialogflowchatlibrary.templateutil.OnClickCallback;
import com.tyagiabhinav.dialogflowchatlibrary.templateutil.ReturnMessage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarouselTemplate extends MessageLayoutTemplate {

    private static final String TAG = CarouselTemplate.class.getSimpleName();

    private Context activityContext;
    private Map<Integer, Value> selectedItems = new HashMap<>();

    public CarouselTemplate(Context context, OnClickCallback callback, int type) {
        super(context, callback, type);
        this.activityContext = context;
        setOnlyTextResponse(false);
    }

    @Override
    FrameLayout populateRichMessageContainer() {
        FrameLayout richMessageContainer = getRichMessageContainer();
        DetectIntentResponse response = getResponse();

        final ReturnMessage.Parameters parameters = ReturnMessage.Parameters.getInstance();
        Struct params = parameters.getParams();
        final Map<String, Value> selectedItems = new HashMap<>();
        params = params.toBuilder().putFields("template", Value.newBuilder().setStringValue("carousel").build()).build();
        parameters.setParams(params);


        List<com.google.cloud.dialogflow.v2.Context> contextList = response.getQueryResult().getOutputContextsList();
        LinearLayout carouselContainer = getVerticalContainer();
        LinearLayout btnLayout = getVerticalContainer();
        btnLayout.setFocusableInTouchMode(true);

        List<Value> cardList = null;
        for (com.google.cloud.dialogflow.v2.Context context : contextList) {
            if (context.getName().contains("param_context")) {
                Map<String, Value> msgTemplate = getMsgTemplate();
                cardList = (msgTemplate.get("carouselItems").getListValue() != null) ? msgTemplate.get("carouselItems").getListValue().getValuesList() : null;
                List<Value> buttonList = (msgTemplate.get("buttonItems") != null) ? msgTemplate.get("buttonItems").getListValue().getValuesList() : null;
                String align = (msgTemplate.get("align") != null) ? msgTemplate.get("align").getStringValue() : null;
                String sizeValue = (msgTemplate.get("size") != null) ? msgTemplate.get("size").getStringValue() : null;
                String eventName = (msgTemplate.get("eventToCall") != null) ? msgTemplate.get("eventToCall").getStringValue() : null;

//            ViewPager carouselPager = carouselLayout.findViewById(R.id.carouselPager);

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

        if (cardList != null) {
            CarouselPager carouselPager = getCarouselPager();//carouselBody.findViewById(R.id.carouselPager);
            CarouselAdapter carouselAdapter = new CarouselAdapter(activityContext, cardList, parameters);
            carouselPager.setAdapter(carouselAdapter);
//            FrameLayout carouselBtn = carouselBody.findViewById(R.id.carouselFooterLayout);
//            carouselBtn.addView(btnLayout);
            carouselContainer.addView(carouselPager);
            Log.d(TAG, "populateRichMessageContainer: carousel added");
        } else {
            Log.d(TAG, "populateRichMessageContainer: No carousel added!");
        }
        carouselContainer.addView(btnLayout);
        richMessageContainer.addView(carouselContainer);

        return richMessageContainer;
    }

    private class CarouselAdapter extends PagerAdapter {
        private Context context;
        private List<Value> cardList;
        private ReturnMessage.Parameters parameters;


        public CarouselAdapter(Context context, List<Value> cardList, ReturnMessage.Parameters parameters) {
            this.context = context;
            this.cardList = cardList;
            this.parameters = parameters;
        }

        @Override
        public int getCount() {
            return cardList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            final View cardLayout = getCardLayout(container);
            final Value carouselItem = cardList.get(position);
            final Map<String, Value> cardItems = carouselItem.getStructValue().getFieldsMap();
            populateCarouselList(cardLayout, cardItems);

            container.addView(cardLayout);
            Log.d(TAG, "instantiateItem: added view");
            //listening to image click
            cardLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (cardItems.get("toast") != null) {
                        Toast.makeText(context, cardItems.get("toast").getStringValue(), Toast.LENGTH_LONG).show();
                    }
                    if (selectedItems != null && !selectedItems.isEmpty()) {
                        if (selectedItems.get(position) != null) {
                            selectedItems.remove(position);
                            cardLayout.setBackgroundResource(android.R.color.transparent);
                        } else {
                            selectedItems.put(position, carouselItem);
                            cardLayout.setBackgroundResource(R.drawable.select_background);
                        }
                    } else {
                        selectedItems.put(position, carouselItem);
                        cardLayout.setBackgroundResource(R.drawable.select_background);
                    }
                    List<Value> allItems = new ArrayList<>(selectedItems.values());
                    Struct tempParam = parameters.getParams().toBuilder().putFields("selectedItems", Value.newBuilder().setListValue(ListValue.newBuilder().addAllValues(allItems).build()).build()).build();
                    parameters.setParams(tempParam);
                    Log.d(TAG, "onClick: " + selectedItems.size() + " -- ");
                }
            });

            return cardLayout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }

        private void populateCarouselList(final View cardLayout, Map<String, Value> cardItems) {
            if (cardItems != null) {
                final String imgUrl = (cardItems.get("imgUrl") != null) ? cardItems.get("imgUrl").getStringValue() : null;
                String title = (cardItems.get("title") != null) ? cardItems.get("title").getStringValue() : null;
                String description = (cardItems.get("description") != null) ? cardItems.get("description").getStringValue() : null;
                TextView titleView = cardLayout.findViewById(R.id.title);
                TextView descriptionView = cardLayout.findViewById(R.id.description);
                titleView.setMovementMethod(LinkMovementMethod.getInstance());
                if (title != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        titleView.setText(Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        titleView.setText(Html.fromHtml(title));
                    }
                } else {
                    titleView.setVisibility(GONE);
                }

                if (description != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        descriptionView.setText(Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        descriptionView.setText(Html.fromHtml(description));
                    }
                } else {
                    descriptionView.setVisibility(GONE);
                }

                Runnable downloadImage = new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bmp = null;
                        try {
                            InputStream in = new java.net.URL(imgUrl).openStream();
                            bmp = BitmapFactory.decodeStream(in);
                        } catch (Exception e) {
                            Log.e("Image download error", e.getMessage());
                            e.printStackTrace();
                        }
                        final Bitmap finalBmp = bmp;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (finalBmp != null) {
                                    ((ImageView) cardLayout.findViewById(R.id.img)).setImageBitmap(finalBmp);
                                } else {
                                    ((ImageView) cardLayout.findViewById(R.id.img)).setImageResource(R.drawable.error_image);
                                }
                            }
                        });
                    }
                };
                new TaskRunner().executeTask(downloadImage);
                Log.d(TAG, "populateCarouselList: List present");
            } else {
                Log.d(TAG, "populateCarouselList: Null list");
            }
        }
    }

}

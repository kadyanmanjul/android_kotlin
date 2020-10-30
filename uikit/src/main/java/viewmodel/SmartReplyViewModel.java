package viewmodel;

import android.content.Context;

import com.cometchat.pro.uikit.SmartReplyList;

import java.util.List;

import adapter.SmartReplyListAdapter;

public class SmartReplyViewModel {

    private static final String TAG = "SmartReplyViewModel";

    private final Context context;
    private final SmartReplyList smartReplyList;
    private SmartReplyListAdapter smartReplyListAdapter;

    public SmartReplyViewModel(Context context, SmartReplyList smartReplyList) {
        this.context = context;
        this.smartReplyList = smartReplyList;
        setSmartReplyAdapter(smartReplyList);
    }

    private void setSmartReplyAdapter(SmartReplyList smartReplyList) {
        smartReplyListAdapter = new SmartReplyListAdapter(context);
        smartReplyList.setAdapter(smartReplyListAdapter);
    }

    private SmartReplyListAdapter getAdapter() {
        if (smartReplyListAdapter == null) {
            smartReplyListAdapter = new SmartReplyListAdapter(context);
        }
        return smartReplyListAdapter;
    }

    public void setSmartReplyList(List<String> replyList) {
        getAdapter().updateList(replyList);
    }


}

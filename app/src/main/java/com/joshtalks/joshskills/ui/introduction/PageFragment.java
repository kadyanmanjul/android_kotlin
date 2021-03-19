package com.joshtalks.joshskills.ui.introduction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import com.joshtalks.joshskills.R;

public class PageFragment extends Fragment {

    private static final String ARG_POSITION = "com.example.PageFragment.ARG_POSITION";

    public PageFragment() {
    }

    public static PageFragment newInstance(final int position) {
        final Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        PageFragment f = new PageFragment();
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle
            savedInstanceState) {
        return inflater.inflate(R.layout.intro_fragment_layout_1, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        final int position = getArguments().getInt(ARG_POSITION);
        final String text = IntroductionActivity.Content.values()[position].getText();
        view.setTag(position);
        final TextView contentTextView = (TextView) view.findViewById(R.id.text);
        contentTextView.setText(text);
        contentTextView.setTag(position);
        final AppCompatImageView image = (AppCompatImageView) view.findViewById(R.id.image);
        image.setImageResource(IntroductionActivity.Content.values()[position].getDrawable());
    }
}
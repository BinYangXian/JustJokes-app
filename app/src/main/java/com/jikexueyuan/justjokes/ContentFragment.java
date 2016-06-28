package com.jikexueyuan.justjokes;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ContentFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_blank, container, false);
        TextView postContentTV= (TextView) rootView.findViewById(R.id.postContentTV);
        String content=getArguments().getString("post_content");
        assert content!=null;
        postContentTV.setText(content);
        return rootView;
    }

}

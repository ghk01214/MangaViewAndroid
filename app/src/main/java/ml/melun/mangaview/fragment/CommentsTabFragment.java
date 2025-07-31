package ml.melun.mangaview.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.CommentsAdapter;

// 댓글 목록을 표시하는 탭 프래그먼트
public class CommentsTabFragment extends Fragment {
    CommentsAdapter madapter; // 댓글을 표시할 리스트뷰의 어댑터

    public CommentsTabFragment() {
    }

    // 프래그먼트에서 사용할 어댑터를 설정합니다.
    public void setAdapter(CommentsAdapter adapter){
        madapter = adapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_test, container, false);
        ListView list = rootView.findViewById(R.id.section_list);
        // 리스트뷰에 어댑터를 설정합니다.
        list.setAdapter(madapter);
        return rootView;
    }
}


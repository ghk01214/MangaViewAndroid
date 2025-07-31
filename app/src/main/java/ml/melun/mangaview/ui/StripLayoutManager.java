package ml.melun.mangaview.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ml.melun.mangaview.adapter.StripAdapter;
import ml.melun.mangaview.model.PageItem;

// 뷰어의 스트립(웹툰처럼 세로로 긴) 모드를 위한 RecyclerView LayoutManager
public class StripLayoutManager extends NpaLinearLayoutManager {
    StripAdapter adapter; // 연결된 스트립 어댑터

    public StripLayoutManager(Context context) {
        super(context);
    }

    public StripLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public StripLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        // 뷰에 연결될 때 어댑터를 가져옴
        adapter = (StripAdapter) view.getAdapter();
    }

    @Override
    public void onAdapterChanged(@Nullable RecyclerView.Adapter oldAdapter, @Nullable RecyclerView.Adapter newAdapter) {
        super.onAdapterChanged(oldAdapter, newAdapter);
        // 어댑터가 변경되면 새로 가져옴
        adapter = (StripAdapter) newAdapter;
    }

    // 특정 페이지 아이템으로 스크롤하는 메서드
    public void scrollToPage(PageItem page){
        List<Object> items = adapter.getItems();
        for(int i=0; i<items.size(); i++){
            Object item = items.get(i);
            if(item instanceof PageItem){
                // 아이템이 PageItem이고, 찾는 페이지와 같으면 해당 위치로 스크롤
                if(((PageItem)item).equals(page))
                    scrollToPosition(i);
            }
        }
    }



}

package ml.melun.mangaview.ui;

import android.content.Context;
import android.util.AttributeSet;

import ml.melun.mangaview.adapter.CustomSpinnerAdapter;
import ml.melun.mangaview.mangaview.Manga;

// Manga 객체를 기반으로 선택 항목을 설정할 수 있는 커스텀 스피너
public class CustomSpinner extends androidx.appcompat.widget.AppCompatSpinner {

    public CustomSpinner(Context context) {
        super(context);
    }

    public CustomSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
    }

    // Manga 객체를 받아 해당 객체와 일치하는 항목을 스피너에서 선택합니다.
    public void setSelection(Manga m) {
        CustomSpinnerAdapter adapter = (CustomSpinnerAdapter) getAdapter();
        for(int i=0; i<adapter.getCount(); i++){
            if(m.equals((Manga)adapter.getItem(i))) {
                // 어댑터의 아이템과 Manga 객체가 일치하면 해당 위치를 선택
                setSelection(i, true);
            }
        }
    }


}

package ml.melun.mangaview.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;

// No Predictive Animation LinearLayoutManager
// 아이템 변경 시 애니메이션을 비활성화하여 IndexOutOfBoundsException을 방지하는 LinearLayoutManager
public class NpaLinearLayoutManager extends LinearLayoutManager {

    public NpaLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public NpaLinearLayoutManager(Context context) {
        super(context);
    }

    public NpaLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * 아이템 변경 애니메이션을 비활성화합니다.
     * @return false
     */
    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }
}

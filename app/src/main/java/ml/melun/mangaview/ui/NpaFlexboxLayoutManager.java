package ml.melun.mangaview.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.flexbox.FlexboxLayoutManager;

// No Predictive Animation FlexboxLayoutManager
// 아이템 변경 시 애니메이션을 비활성화하여 IndexOutOfBoundsException을 방지하는 FlexboxLayoutManager
public class NpaFlexboxLayoutManager extends FlexboxLayoutManager {
    public NpaFlexboxLayoutManager(Context context) {
        super(context);
    }

    public NpaFlexboxLayoutManager(Context context, int flexDirection) {
        super(context, flexDirection);
    }

    public NpaFlexboxLayoutManager(Context context, int flexDirection, int flexWrap) {
        super(context, flexDirection, flexWrap);
    }

    public NpaFlexboxLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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

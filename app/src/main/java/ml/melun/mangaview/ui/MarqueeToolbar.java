// https://gist.github.com/InsanityOnABun/95c0757f2f527cc50e39
package ml.melun.mangaview.ui;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import java.lang.reflect.Field;

// 툴바의 제목이 길 경우 흐르는 효과(marquee)를 적용하는 커스텀 툴바
public class MarqueeToolbar extends Toolbar {

    TextView title; // 툴바의 제목을 표시하는 TextView

    public MarqueeToolbar(Context context) {
        super(context);
    }

    public MarqueeToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MarqueeToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTitle(CharSequence title) {
        // 리플렉션이 아직 수행되지 않았다면 수행
        if (!reflected) {
            reflected = reflectTitle();
        }
        super.setTitle(title);
        // 제목 TextView를 선택하여 marquee 효과를 시작
        selectTitle();
    }

    @Override
    public void setTitle(int resId) {
        // 리플렉션이 아직 수행되지 않았다면 수행
        if (!reflected) {
            reflected = reflectTitle();
        }
        super.setTitle(resId);
        // 제목 TextView를 선택하여 marquee 효과를 시작
        selectTitle();
    }

    boolean reflected = false; // 리플렉션을 통해 mTitleTextView를 성공적으로 가져왔는지 여부

    // 리플렉션을 사용하여 Toolbar의 mTitleTextView 필드에 접근하고, marquee 효과를 설정
    private boolean reflectTitle() {
        try {
            Field field = Toolbar.class.getDeclaredField("mTitleTextView");
            field.setAccessible(true);
            title = (TextView) field.get(this);
            title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            title.setMarqueeRepeatLimit(-1); // 무한 반복
            return true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 제목 TextView를 선택하여 marquee 효과를 시작
    public void selectTitle() {
        if (title != null)
            title.setSelected(true);
    }
}

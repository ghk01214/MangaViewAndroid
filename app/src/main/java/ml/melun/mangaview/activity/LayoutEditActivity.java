package ml.melun.mangaview.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import ml.melun.mangaview.R;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.getScreenWidth;

// 뷰어의 페이지 넘김 버튼 레이아웃을 편집하는 액티비티
public class LayoutEditActivity extends AppCompatActivity {
    Button left; // 왼쪽 버튼
    Button right; // 오른쪽 버튼
    boolean leftRight; // 좌우 버튼의 기능 (true: 왼쪽이 다음, 오른쪽이 이전; false: 반대)
    SeekBar seekBar; // 버튼 크기 조절을 위한 시크바
    ViewGroup.LayoutParams params; // 왼쪽 버튼의 레이아웃 파라미터

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(p.getDarkTheme()) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_edit);

        Context context = this;

        left = this.findViewById(R.id.layoutLeftButton);
        right = this.findViewById(R.id.layoutRightButton);
        leftRight = p.getLeftRight();
        setButtonText();

        seekBar = this.findViewById(R.id.seekBar);
        params = left.getLayoutParams();
        refreshSeekbar();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                params.width = i;
                left.setLayoutParams(params);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        this.findViewById(R.id.layout_save).setOnClickListener(view -> {
            p.setPageControlButtonOffset((float)seekBar.getProgress() / (float)seekBar.getMax());
            p.setLeftRight(leftRight);
            Toast.makeText(context, "설정 완료", Toast.LENGTH_SHORT).show();
            finish();
        });

        this.findViewById(R.id.layout_reset).setOnClickListener(view -> {
            p.setPageControlButtonOffset(-1);
            p.setLeftRight(false);
            Toast.makeText(context, "기본값으로 설정됨", Toast.LENGTH_SHORT).show();
            finish();
        });

        this.findViewById(R.id.layout_cancel).setOnClickListener(view -> finish());

        this.findViewById(R.id.layout_reverse).setOnClickListener(view -> {
            leftRight = !leftRight;
            setButtonText();
        });
    }

    private void refreshSeekbar(){
        // set seekbar max to current screen width
        int max = getScreenWidth(getWindowManager().getDefaultDisplay());
        seekBar.setMax(max);

        // set button width to saved value
        float percentage = p.getPageControlButtonOffset();
        if(percentage != -1){
            params.width = (int)((float)max * percentage);
            left.setLayoutParams(params);
        }
        // set seekbar progress to current button width
        seekBar.setProgress(params.width);
    }

    private void setButtonText(){
        if(leftRight){
            left.setText(R.string.next_page);
            right.setText(R.string.prev_page);
        }else{
            right.setText(R.string.next_page);
            left.setText(R.string.prev_page);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refreshSeekbar();
    }
}

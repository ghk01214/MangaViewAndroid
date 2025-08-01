package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.appbar.AppBarLayout;
import com.google.gson.Gson;

import java.util.List;
import android.text.Html;

import ml.melun.mangaview.R;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.adapter.CustomSpinnerAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.NovelPage;
import ml.melun.mangaview.ui.CustomSpinner;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

// 소설을 만화 뷰어와 같은 UI로 보여주는 뷰어 액티비티 (ViewerActivity4)
public class ViewerActivity4 extends AppCompatActivity {
    
    private TextView novelContent;
    private ScrollView scrollView;
    private TextView toolbarTitle;
    private AppBarLayout appbar, appbarBottom;
    private ImageButton backButton, menuButton;
    private ImageButton next, prev;
    private Button novelFontBtn;
    private CustomSpinner spinner;
    private CustomSpinnerAdapter spinnerAdapter;
    
    private Manga manga;
    private NovelPage novelPage;
    private List<Manga> eps; // 현재 작품의 전체 에피소드 목록
    private boolean isLoading = false;
    private boolean toolbarShow = true;
    private Context context = this;
    
    private int currentFontSize = 16; // 기본 폰트 크기

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 다크 테마 적용
        if(p.getDarkTheme()){
            setTheme(R.style.AppTheme_NoActionBar);
        }
        
        setContentView(R.layout.activity_viewer4);
        
        // Intent로부터 만화 정보 가져오기
        Intent intent = getIntent();
        String mangaJson = intent.getStringExtra("manga");
        if(mangaJson != null){
            manga = new Gson().fromJson(mangaJson, Manga.class);
        }
        
        initViews();
        setupEventListeners();
        setupStatusBarPadding();
        
        // 소설 내용 로드
        loadNovelContent();
    }
    
    private void initViews() {
        novelContent = findViewById(R.id.novelContent);
        scrollView = findViewById(R.id.novelScrollView);
        toolbarTitle = findViewById(R.id.toolbar_title);
        appbar = findViewById(R.id.viewerAppbar);
        appbarBottom = findViewById(R.id.viewerAppbarBottom);
        backButton = findViewById(R.id.backButton);
        menuButton = findViewById(R.id.menuButton);
        next = findViewById(R.id.toolbar_next);
        prev = findViewById(R.id.toolbar_previous);
        novelFontBtn = findViewById(R.id.novelFontBtn);
        spinner = findViewById(R.id.toolbar_spinner);
        
        // 제목 설정
        if(manga != null && manga.getName() != null) {
            toolbarTitle.setText(manga.getName());
        } else {
            toolbarTitle.setText("소설 뷰어");
        }
        
        // 폰트 크기 적용
        novelContent.setTextSize(currentFontSize);
        
        // 길게 누르면 툴바 숨기기/보이기 토글
        View.OnLongClickListener longClickToggle = view -> {
            toggleToolbars();
            return true;
        };
        scrollView.setOnLongClickListener(longClickToggle);
        novelContent.setOnLongClickListener(longClickToggle);
    }
    
    private void setupEventListeners() {
        // 뒤로가기 버튼
        backButton.setOnClickListener(v -> finish());
        
        // 메뉴 버튼
        menuButton.setOnClickListener(v -> showMenu(v));
        
        // 폰트 크기 조정 버튼
        novelFontBtn.setOnClickListener(v -> showFontSizeMenu(v));
        
        
        
        // 이전/다음 에피소드 버튼
        prev.setOnClickListener(v -> {
            if (eps != null && manga != null) {
                int currentIndex = eps.indexOf(manga);
                if (currentIndex < eps.size() - 1) { // 다음 에피소드가 있다면 (이전 에피소드 버튼이므로 인덱스 증가)
                    manga = eps.get(currentIndex + 1);
                    loadNovelContent();
                } else {
                    Toast.makeText(this, "이전 에피소드가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        next.setOnClickListener(v -> {
            if (eps != null && manga != null) {
                int currentIndex = eps.indexOf(manga);
                if (currentIndex > 0) { // 이전 에피소드가 있다면 (다음 에피소드 버튼이므로 인덱스 감소)
                    manga = eps.get(currentIndex - 1);
                    loadNovelContent();
                } else {
                    Toast.makeText(this, "다음 에피소드가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // 스피너 활성화 및 설정
        spinner.setVisibility(View.VISIBLE);
        spinnerAdapter = new CustomSpinnerAdapter(context);
        spinnerAdapter.setListener((m, i) -> {
            if (!isLoading) {
                manga = m;
                loadNovelContent();
            }
        });
        spinner.setAdapter(spinnerAdapter);
    }
    
    private void showMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.novel_viewer_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_refresh:
                    loadNovelContent();
                    return true;
                
                default:
                    return false;
            }
        });
        
        popup.show();
    }
    
    private void showFontSizeMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add(0, 1, 0, "작게 (14sp)");
        popup.getMenu().add(0, 2, 0, "보통 (16sp)");
        popup.getMenu().add(0, 3, 0, "크게 (18sp)");
        popup.getMenu().add(0, 4, 0, "매우 크게 (20sp)");
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    currentFontSize = 14;
                    break;
                case 2:
                    currentFontSize = 16;
                    break;
                case 3:
                    currentFontSize = 18;
                    break;
                case 4:
                    currentFontSize = 20;
                    break;
            }
            novelContent.setTextSize(currentFontSize);
            Toast.makeText(this, "글꼴 크기: " + currentFontSize + "sp", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        popup.show();
    }
    
    private void toggleToolbars() {
        if (toolbarShow) {
            // 툴바 숨기기
            appbar.animate().translationY(-appbar.getHeight()).setDuration(300);
            appbarBottom.animate().translationY(appbarBottom.getHeight()).setDuration(300);
            toolbarShow = false;
            // 시스템 UI 숨기기 (상태바 포함)
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | 
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | 
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            // 툴바 보이기
            appbar.animate().translationY(0).setDuration(300);
            appbarBottom.animate().translationY(0).setDuration(300);
            toolbarShow = true;
            // 시스템 UI 보이기
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }
    
    private void setupStatusBarPadding() {
        // 상태바 높이 가져오기
        int statusBarHeight = getStatusBarHeight();
        
        // 상단 툴바에 상태바 높이만큼 패딩 추가
        appbar.setPadding(0, statusBarHeight, 0, 0);
    }
    
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    
    private void loadNovelContent(){
        if(isLoading || manga == null) return;
        
        isLoading = true;
        findViewById(R.id.novelProgressBar).setVisibility(View.VISIBLE);
        novelContent.setVisibility(View.GONE);
        
        // 에피소드 목록이 없으면 로드
        if (eps == null || eps.isEmpty()) {
            if (manga.getTitle() != null) {
                eps = manga.getTitle().getEps();
            }
        }
        
        new LoadNovelTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void updateEpisodeButtons() {
        if (eps != null && manga != null) {
            int currentIndex = eps.indexOf(manga);
            next.setEnabled(currentIndex > 0);
            prev.setEnabled(currentIndex < eps.size() - 1);
            next.setColorFilter(next.isEnabled() ? null : Color.BLACK);
            prev.setColorFilter(prev.isEnabled() ? null : Color.BLACK);
        } else {
            next.setEnabled(false);
            prev.setEnabled(false);
            next.setColorFilter(Color.BLACK);
            prev.setColorFilter(Color.BLACK);
        }
    }

    private void updateSpinner() {
        if (eps != null && manga != null) {
            spinnerAdapter.setData(eps, manga);
            spinner.setSelection(manga);
        }
    }    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // 소설 내용을 비동기적으로 로드하는 AsyncTask
    private class LoadNovelTask extends AsyncTask<Void, Void, Integer> {
        
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                novelPage = new NovelPage(manga);
                return novelPage.fetch(httpClient);
            } catch (Exception e) {
                e.printStackTrace();
                return 1;
            }
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            isLoading = false;
            findViewById(R.id.novelProgressBar).setVisibility(View.GONE);
            
            if(result == 0 && novelPage != null && novelPage.getContent() != null && 
               !novelPage.getContent().trim().isEmpty()) {
                // 성공적으로 로드된 경우
                novelContent.setText(novelPage.getContent());
                novelContent.setVisibility(View.VISIBLE);
                
                // 제목 업데이트
                if(novelPage.getTitle() != null && !novelPage.getTitle().isEmpty()){
                    toolbarTitle.setText(novelPage.getTitle());
                }
                updateEpisodeButtons();
                updateSpinner();
            } else {
                // 로드 실패한 경우
                String errorMessage = "소설을 불러올 수 없습니다.";
                if(novelPage != null && novelPage.getContent() != null && !novelPage.getContent().trim().isEmpty()) {
                    errorMessage = novelPage.getContent(); // 오류 메시지 표시
                }
                novelContent.setText(errorMessage);
                novelContent.setVisibility(View.VISIBLE);
                
                if(result != 0){
                    Toast.makeText(ViewerActivity4.this, "소설 로딩 실패", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}

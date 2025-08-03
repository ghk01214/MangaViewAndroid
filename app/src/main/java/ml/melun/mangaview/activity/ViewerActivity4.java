package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import ml.melun.mangaview.adapter.NovelTextAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Novel;
import java.util.ArrayList;
import ml.melun.mangaview.ui.CustomSpinner;
import ml.melun.mangaview.activity.CommentsActivity;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.showTokiCaptchaPopup;
import static ml.melun.mangaview.mangaview.Title.LOAD_CAPTCHA;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;

// 소설을 만화 뷰어와 같은 UI로 보여주는 뷰어 액티비티 (ViewerActivity4)
public class ViewerActivity4 extends AppCompatActivity {
    
    private RecyclerView strip;
    private NovelTextAdapter novelTextAdapter;
    private TextView toolbarTitle;
    private AppBarLayout appbar, appbarBottom;
    private ImageButton backButton, commentButton;
    private ImageButton next, prev;
    private Button pageBtn, fontBtn, viewerBtn1, viewerBtn2;
    private CustomSpinner spinner;
    private CustomSpinnerAdapter spinnerAdapter;
    
    private Manga manga;
    private Novel novel;
    private List<Manga> eps; // 현재 작품의 전체 에피소드 목록 (Manga 호환성 유지)
    private boolean isLoading = false;
    private boolean toolbarShow = true;
    private boolean toolbarshow = true; // 1번 뷰어와 동일한 변수명 사용
    private Context context = this;
    
    private int currentFontSize = 16; // 기본 폰트 크기
    private SharedPreferences readingPositionPrefs; // 읽기 위치 저장용 SharedPreferences
    private boolean isScrollingToSavedPosition = false; // 저장된 위치로 스크롤 중인지 여부

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        next = this.findViewById(R.id.toolbar_next);
        prev = this.findViewById(R.id.toolbar_previous);
        appbar = this.findViewById(R.id.viewerAppbar);
        toolbarTitle = this.findViewById(R.id.toolbar_title);
        appbarBottom = this.findViewById(R.id.viewerAppbarBottom);

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
        
        // SharedPreferences 초기화
        readingPositionPrefs = getSharedPreferences("novel_reading_positions", MODE_PRIVATE);
        
        initViews();
        setupEventListeners();
        setupStatusBarPadding();
        
        // 소설 내용 로드
        loadNovelContent();
    }
    
    private void initViews() {
        strip = findViewById(R.id.strip);
        toolbarTitle = findViewById(R.id.toolbar_title);
        
        // RecyclerView 설정
        strip.setLayoutManager(new LinearLayoutManager(this));
        novelTextAdapter = new NovelTextAdapter();
        strip.setAdapter(novelTextAdapter);
        
        // 다크모드에 따른 RecyclerView 배경색 설정
        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (isDarkMode) {
            strip.setBackgroundColor(Color.parseColor("#1A1A1A")); // 어두운 배경
        } else {
            strip.setBackgroundColor(Color.parseColor("#FFFFFF")); // 밝은 배경
        }
        appbar = findViewById(R.id.viewerAppbar);
        appbarBottom = findViewById(R.id.viewerAppbarBottom);
        backButton = findViewById(R.id.backButton);
        commentButton = findViewById(R.id.commentButton);
        next = findViewById(R.id.toolbar_next);
        prev = findViewById(R.id.toolbar_previous);
        viewerBtn1 = findViewById(R.id.viewerBtn1); // 글꼴 크기 버튼
        viewerBtn2 = findViewById(R.id.viewerBtn2); // 기능2 버튼
        spinner = findViewById(R.id.toolbar_spinner);
        
        // 기능1 버튼을 글꼴 크기 버튼으로 설정
        if (viewerBtn1 != null) {
            viewerBtn1.setText("글꼴");
        }
        
        // 오프라인 콘텐츠일 때 댓글 버튼 숨김 (1번 뷰어와 동일)
        if (manga != null && !manga.isOnline() && commentButton != null) {
            commentButton.setVisibility(View.GONE);
        }
        
        // 제목 설정
        if(manga != null && manga.getName() != null) {
            toolbarTitle.setText(manga.getName());
        } else {
            toolbarTitle.setText("소설 뷰어");
        }
        
        // NovelTextAdapter에 툴바 토글 리스너 설정
        novelTextAdapter.setClickListener(this::toggleToolbar);
        
        // 스크롤 리스너 설정 (툴바 자동 숨김/표시)
        setupScrollListener();
    }
    
    private void setupEventListeners() {
        // 뒤로가기 버튼
        backButton.setOnClickListener(v -> finish());
        
        // 댓글 버튼 (1번 뷰어와 동일한 방식)
        if (commentButton != null) {
            commentButton.setOnClickListener(v -> showComments());
        }
        
        // 글꼴 크기 버튼 (기능1 버튼)
        if (viewerBtn1 != null) {
            viewerBtn1.setOnClickListener(v -> showFontSizeMenu(v));
        }
        
        // 기능2 버튼을 메뉴 버튼으로 사용
        if (viewerBtn2 != null) {
            viewerBtn2.setText("메뉴");
            viewerBtn2.setOnClickListener(v -> showMenu(v));
        }
        
        
        
        // 이전/다음 에피소드 버튼 - Novel 클래스의 메서드 활용
        prev.setOnClickListener(v -> {
            if (novel != null) {
                Novel prevNovel = novel.prevEp(); // Novel의 prevEp() 메서드 사용
                if (prevNovel != null) {
                    manga = prevNovel.toManga(); // Novel을 Manga로 변환
                    loadNovelContent();
                }
            }
        });
        
        next.setOnClickListener(v -> {
            if (novel != null) {
                Novel nextNovel = novel.nextEp(); // Novel의 nextEp() 메서드 사용
                if (nextNovel != null) {
                    manga = nextNovel.toManga(); // Novel을 Manga로 변환
                    loadNovelContent();
                }
            }
        });
        
        // 스피너 활성화 및 설정
        spinner.setVisibility(View.VISIBLE);
        spinnerAdapter = new CustomSpinnerAdapter(context);
        spinnerAdapter.setListener((m, i) -> {
            if (!isLoading) {
                manga = m;
                novel = Novel.fromManga(manga); // Manga를 Novel로 변환
                loadNovelContent();
            }
        });
        spinner.setAdapter(spinnerAdapter);
    }
    
    private void showMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add(0, 1, 0, "새로고침");
        popup.getMenu().add(0, 2, 0, "글꼴 크기");
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: // 새로고침
                    loadNovelContent();
                    return true;
                case 2: // 글꼴 크기
                    showFontSizeMenu(view);
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
            // 어댑터에 폰트 크기 적용
            if (novelTextAdapter != null) {
                novelTextAdapter.setTextSize(currentFontSize);
            }
            return true;
        });
        
        popup.show();
    }
    
    // 1번 뷰어와 동일한 댓글 기능
    private void showComments() {
        if (novel != null && manga != null) {
            Intent commentActivity = new Intent(context, CommentsActivity.class);
            Gson gson = new Gson();
            commentActivity.putExtra("comments", gson.toJson(novel.getComments()));
            commentActivity.putExtra("bestComments", gson.toJson(novel.getBestComments()));
            commentActivity.putExtra("id", manga.getId());
            startActivity(commentActivity);
        } else {
            Toast.makeText(this, "댓글을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    
    // 1번 뷰어와 동일한 툴바 토글 메서드
    public void toggleToolbar(){
        if(toolbarshow){
            appbar.animate().translationY(-appbar.getHeight());
            appbarBottom.animate().translationY(+appbarBottom.getHeight());
            toolbarshow=false;
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        else {
            appbar.animate().translationY(0);
            appbarBottom.animate().translationY(0);
            toolbarshow = true;
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
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
        // 로딩 표시 (RecyclerView를 숨기는 대신 빈 상태로 설정)
        strip.setVisibility(View.GONE);
        
        // 로딩 시작 전에도 버튼과 스피너 업데이트 (항상 보이도록)
        updateEpisodeButtons();
        updateSpinner();
        
        // Novel 객체가 없으면 생성
        if (novel == null) {
            novel = Novel.fromManga(manga);
        }
        
        // 1번 뷰어와 동일한 방식으로 콜백과 함께 AsyncTask 실행
        new LoadNovelTask(manga, novelCallback).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    // 1번 뷰어의 refresh() 메서드와 동일한 역할
    private void refresh(){
        loadNovelContent();
    }

    private void updateEpisodeButtons() {
        // 버튼들이 항상 보이도록 visibility 설정
        if (next != null) next.setVisibility(View.VISIBLE);
        if (prev != null) prev.setVisibility(View.VISIBLE);
        
        // Novel 방식으로 다음/이전 에피소드 가능 여부 확인
        if (novel != null) {
            Novel nextNovel = novel.nextEp();
            Novel prevNovel = novel.prevEp();
            
            if (next != null) {
                next.setEnabled(nextNovel != null);
                if (next.isEnabled()) {
                    next.clearColorFilter();
                } else {
                    next.setColorFilter(Color.BLACK);
                }
            }
            if (prev != null) {
                prev.setEnabled(prevNovel != null);
                if (prev.isEnabled()) {
                    prev.clearColorFilter();
                } else {
                    prev.setColorFilter(Color.BLACK);
                }
            }
        } else if (eps != null && manga != null && !eps.isEmpty()) {
            // Novel 객체가 없으면 기존 방식 사용
            int currentIndex = eps.indexOf(manga);
            
            if (currentIndex >= 0) {
                if (next != null) {
                    next.setEnabled(currentIndex > 0);
                    if (next.isEnabled()) {
                        next.clearColorFilter();
                    } else {
                        next.setColorFilter(Color.BLACK);
                    }
                }
                if (prev != null) {
                    prev.setEnabled(currentIndex < eps.size() - 1);
                    if (prev.isEnabled()) {
                        prev.clearColorFilter();
                    } else {
                        prev.setColorFilter(Color.BLACK);
                    }
                }
            } else {
                // 인덱스를 찾지 못한 경우에도 버튼은 활성화
                if (next != null) {
                    next.setEnabled(true);
                    next.clearColorFilter();
                }
                if (prev != null) {
                    prev.setEnabled(true);
                    prev.clearColorFilter();
                }
            }
        } else {
            // eps가 없어도 버튼은 보이되 비활성화
            if (next != null) {
                next.setEnabled(false);
                next.setColorFilter(Color.BLACK);
            }
            if (prev != null) {
                prev.setEnabled(false);
                prev.setColorFilter(Color.BLACK);
            }
        }
    }

    private void updateSpinner() {
        // 스피너가 항상 보이도록 visibility 설정
        if (spinner != null) spinner.setVisibility(View.VISIBLE);
        
        if (eps != null && manga != null && !eps.isEmpty()) {
            if (spinnerAdapter != null && spinner != null) {
                spinnerAdapter.setData(eps, manga);
                spinner.setSelection(manga);
            }
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CAPTCHA) {
            // 1번 뷰어와 동일하게 refresh() 호출
            refresh();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 액티비티가 일시정지될 때 현재 읽기 위치 저장
        saveCurrentReadingPosition();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티가 종료될 때 현재 읽기 위치 저장
        saveCurrentReadingPosition();
    }
    
    // 1번 뷰어와 동일한 구조의 LoadNovelTask (만화 뷰어의 loadImages와 유사)
    private class LoadNovelTask extends AsyncTask<Void, Void, Integer> {
        private Manga m;
        private NovelLoadCallback callback;
        
        public LoadNovelTask(Manga manga, NovelLoadCallback callback) {
            this.m = manga;
            this.callback = callback;
        }
        
        @Override
        protected Integer doInBackground(Void... params) {
            if(m.isOnline()) {
                novel = new Novel(m);
                int result = novel.fetch(httpClient);
                return result;
            } else {
                // 오프라인 모드 처리 (필요시 구현)
                return 0;
            }
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            if(result == LOAD_CAPTCHA){
                showTokiCaptchaPopup(context, p);
                return;
            }
            
            // 1번 뷰어와 동일한 방식으로 콜백 호출
            if(callback != null) {
                callback.post(m, result);
            }
        }
    }
    
    // 1번 뷰어의 콜백 인터페이스와 유사한 소설 로드 콜백
    public interface NovelLoadCallback {
        void post(Manga m, Integer result);
    }
    
    // 1번 뷰어의 callback.post(m)과 동일한 역할을 하는 콜백 구현
    NovelLoadCallback novelCallback = new NovelLoadCallback() {
        @Override
        public void post(Manga m, Integer result) {
            isLoading = false;
            strip.setVisibility(View.VISIBLE);
            
            if(result == 0 && novel != null && novel.getContent() != null && 
               !novel.getContent().trim().isEmpty()) {
                // 성공적으로 로드된 경우
                String content = novel.getContent();
                
                // 제목 업데이트
                if(novel.getName() != null && !novel.getName().isEmpty()){
                    toolbarTitle.setText(novel.getName());
                }
                
                // 에피소드 목록 업데이트 (Novel에서 가져온 데이터 사용)
                if (novel.getEps() != null && !novel.getEps().isEmpty()) {
                    // Novel 에피소드 목록을 Manga 목록으로 변환
                    eps = new ArrayList<>();
                    for (Novel n : novel.getEps()) {
                        eps.add(n.toManga());
                    }
                }
                
                // RecyclerView에 소설 내용 표시
                novelTextAdapter.setContent(content);
                novelTextAdapter.setTextSize(currentFontSize);
                
                // 저장된 읽기 위치로 복원 (완료된 에피소드나 새 에피소드는 처음부터)
                restoreReadingPosition();
                
                updateEpisodeButtons();
                updateSpinner();
            } else {
                // 로드 실패한 경우
                String errorMessage = "소설을 불러올 수 없습니다.";
                if(novel != null && novel.getContent() != null && !novel.getContent().trim().isEmpty()) {
                    errorMessage = novel.getContent(); // 오류 메시지 표시
                }
                
                Toast.makeText(ViewerActivity4.this, errorMessage, Toast.LENGTH_LONG).show();
                
                // 실패해도 버튼과 스피너는 업데이트
                updateEpisodeButtons();
                updateSpinner();
                
                if(result != 0){
                    Toast.makeText(ViewerActivity4.this, "소설 로딩 실패", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
    
    // 스크롤 리스너 설정 (툴바 자동 숨김/표시 + 읽기 위치 저장)
    private void setupScrollListener() {
        strip.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private int lastScrollY = 0;
            private final int scrollThreshold = 20; // 스크롤 임계값
            
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // 저장된 위치로 스크롤 중일 때는 툴바 로직과 위치 저장을 건너뜀
                if (isScrollingToSavedPosition) {
                    return;
                }
                
                // 스크롤 방향에 따른 툴바 숨김/표시
                if (Math.abs(dy) > scrollThreshold) {
                    if (dy > 0 && toolbarshow) {
                        // 아래로 스크롤 시 툴바 숨김
                        hideToolbar();
                    } else if (dy < 0 && !toolbarshow) {
                        // 위로 스크롤 시 툴바 표시
                        showToolbar();
                    }
                }
                
                // 현재 스크롤 위치 저장 (사용자가 직접 스크롤할 때만)
                saveCurrentReadingPosition();
                
                lastScrollY = recyclerView.getScrollY();
            }
        });
    }
    
    // 읽기 위치 관리 메서드들
    private String getEpisodeKey() {
        if (manga != null) {
            return "episode_" + manga.getId();
        }
        return null;
    }
    
    private void saveCurrentReadingPosition() {
        String episodeKey = getEpisodeKey();
        if (episodeKey != null && strip.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) strip.getLayoutManager();
            int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
            int totalItems = novelTextAdapter.getItemCount();
            
            // 읽기 완료 여부 확인 (마지막 아이템이 보이면 완료)
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
            boolean isCompleted = (lastVisiblePosition >= totalItems - 1) && totalItems > 0;
            
            SharedPreferences.Editor editor = readingPositionPrefs.edit();
            editor.putInt(episodeKey + "_position", firstVisiblePosition);
            editor.putBoolean(episodeKey + "_completed", isCompleted);
            editor.apply();
        }
    }
    
    private void restoreReadingPosition() {
        String episodeKey = getEpisodeKey();
        if (episodeKey != null) {
            boolean isCompleted = readingPositionPrefs.getBoolean(episodeKey + "_completed", false);
            int savedPosition = readingPositionPrefs.getInt(episodeKey + "_position", 0);
            
            // 완료된 에피소드이거나 저장된 위치가 없으면 처음부터 시작
            if (isCompleted || savedPosition == 0) {
                strip.scrollToPosition(0);
            } else {
                // 저장된 위치로 스크롤
                isScrollingToSavedPosition = true;
                strip.post(() -> {
                    strip.scrollToPosition(savedPosition);
                    // 스크롤 완료 후 플래그 해제
                    strip.postDelayed(() -> {
                        isScrollingToSavedPosition = false;
                    }, 500);
                });
            }
        }
    }
    
    // 툴바 숨김
    private void hideToolbar() {
        if (toolbarshow) {
            appbar.animate().translationY(-appbar.getHeight()).setDuration(300);
            appbarBottom.animate().translationY(appbarBottom.getHeight()).setDuration(300);
            toolbarshow = false;
        }
    }
    
    // 툴바 표시
    private void showToolbar() {
        if (!toolbarshow) {
            appbar.animate().translationY(0).setDuration(300);
            appbarBottom.animate().translationY(0).setDuration(300);
            toolbarshow = true;
        }
    }
}

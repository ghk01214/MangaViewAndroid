package ml.melun.mangaview.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import ml.melun.mangaview.interfaces.MainActivityCallback;
import ml.melun.mangaview.R;
import ml.melun.mangaview.UrlUpdater;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.activity.MainActivity;
import ml.melun.mangaview.activity.TagSearchActivity;
import ml.melun.mangaview.adapter.MainAdapter;
import ml.melun.mangaview.adapter.MainWebtoonAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.ui.NpaFlexboxLayoutManager;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.openViewer;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;
import static ml.melun.mangaview.mangaview.MTitle.base_comic;
import static ml.melun.mangaview.mangaview.MTitle.base_webtoon;

// 메인 화면의 메인 탭(만화/웹툰)을 담당하는 프래그먼트
public class MainMain extends Fragment{

    RecyclerView mainRecycler; // 메인 콘텐츠를 보여줄 RecyclerView
    MainAdapter mainadapter; // 만화 탭 어댑터
    MainWebtoonAdapter mainWebtoonAdapter; // 웹툰 탭 어댑터
    Fragment fragment; // 프래그먼트 자신을 참조
    boolean wait = false; // URL 업데이트를 기다리는지 여부
    UrlUpdater.UrlUpdaterCallback callback; // URL 업데이트 완료 후 호출될 콜백
    MainActivityCallback mainActivityCallback; // MainActivity와 통신하기 위한 콜백

    final static int COMIC_TAB = 0; // 만화 탭 인덱스
    final static int WEBTOON_TAB = 1; // 웹툰 탭 인덱스

    boolean fragmentActive = false; // 프래그먼트가 활성화 상태인지 여부

    public void setWait(Boolean wait){
        this.wait = wait;
    }


    public static MainMain newInstance(){
        MainMain frag = new MainMain();
        frag.initializeCallback();
        return frag;
    }

    public MainMain(){

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mainActivityCallback = (MainActivity)getActivity();
    }

    // URL 업데이트 콜백 초기화
    public void initializeCallback(){
        callback = success -> {
            wait = false;
            // URL 업데이트가 성공하면 각 어댑터의 데이터를 새로고침
            if(mainadapter != null && fragmentActive) {
                mainadapter.fetch();
            }
            if(mainWebtoonAdapter != null && fragmentActive) {
                mainWebtoonAdapter.fetch();
            }
        };
    }

    public UrlUpdater.UrlUpdaterCallback getCallback(){
        return callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_main , container, false);


        TabLayout tabLayout = rootView.findViewById(R.id.mainTab);

        // 탭 생성
        TabLayout.Tab comicTab = tabLayout.newTab().setText("만화");
        TabLayout.Tab webtoonTab = tabLayout.newTab().setText("웹툰");
        tabLayout.addTab(comicTab);
        tabLayout.addTab(webtoonTab);

        // 마지막으로 선택했던 탭을 기본으로 선택
        if(p.getBaseMode() == base_comic)
            comicTab.select();
        else
            webtoonTab.select();

        // 탭 선택 리스너
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // 탭에 따라 RecyclerView의 어댑터를 교체하고, 기본 모드를 저장
                if(tab.getPosition() == COMIC_TAB){
                    mainRecycler.setAdapter(mainadapter);
                    p.setBaseMode(base_comic);
                }else if(tab.getPosition() == WEBTOON_TAB){
                    mainRecycler.setAdapter(mainWebtoonAdapter);
                    p.setBaseMode(base_webtoon);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        fragment = this;
        mainRecycler = rootView.findViewById(R.id.main_recycler);
        NpaFlexboxLayoutManager lm = new NpaFlexboxLayoutManager(getContext());
        mainRecycler.setLayoutManager(lm);

        // 어댑터 아이템 클릭 리스너
        MainAdapter.onItemClick listener = new MainAdapter.onItemClick() {

            @Override
            public void clickedTitle(Title t) {
                startActivity(episodeIntent(getContext(), t));
            }

            @Override
            public void clickedManga(Manga m) {
                openViewer(getContext(), m,-1);
            }

            @Override
            public void clickedGenre(String t) {
                // 장르(태그) 검색 실행
                Intent i = new Intent(getContext(), TagSearchActivity.class);
                i.putExtra("query",t);
                i.putExtra("mode",2);
                startActivity(i);
            }

            @Override
            public void clickedName(String t) {
                // 초성 검색 실행
                Intent i = new Intent(getContext(), TagSearchActivity.class);
                i.putExtra("query",t);
                i.putExtra("mode",3);
                startActivity(i);
            }

            @Override
            public void clickedRelease(String t) {
                // 발행 구분 검색 실행
                Intent i = new Intent(getContext(), TagSearchActivity.class);
                i.putExtra("query",t);
                i.putExtra("mode",4);
                startActivity(i);
            }

            @Override
            public void clickedMoreUpdated() {
                // 업데이트 목록 더보기
                Intent i = new Intent(getContext(), TagSearchActivity.class);
                i.putExtra("mode",5);
                startActivity(i);
            }

            @Override
            public void captchaCallback() {
                // 캡차 발생 시 팝업 표시
                Utils.showCaptchaPopup(getActivity(), 3, fragment, p);
            }

            @Override
            public void clickedSearch(String query) {
                // 검색어 클릭 시 검색 탭으로 전달
                mainActivityCallback.search(query);
            }

            @Override
            public void clickedRetry() {
                // 웹툰 탭 로딩 실패 시 재시도
                mainWebtoonAdapter.fetch();
            }
        };

        mainadapter = new MainAdapter(getContext());
        mainadapter.setMainClickListener(listener);

        mainWebtoonAdapter = new MainWebtoonAdapter(getContext());
        mainWebtoonAdapter.setListener(listener);

        // 현재 모드에 맞는 어댑터 설정
        if(p.getBaseMode() == base_comic)
            mainRecycler.setAdapter(mainadapter);
        else
            mainRecycler.setAdapter(mainWebtoonAdapter);

        // URL 업데이트를 기다리지 않는 경우 바로 데이터 로드 시작
        if(!wait) {
            mainadapter.fetch();
            mainWebtoonAdapter.fetch();
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        fragmentActive = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        fragmentActive = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 캡차 해결 후 돌아왔을 때 데이터 새로고침
        if(resultCode == RESULT_CAPTCHA && mainadapter!=null)
            mainadapter.fetch();
    }
}

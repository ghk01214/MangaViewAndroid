package ml.melun.mangaview.fragment;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.openViewer;
import static ml.melun.mangaview.Utils.popup;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;

// 메인 화면의 검색 탭을 담당하는 프래그먼트
public class MainSearch extends Fragment {
    SwipyRefreshLayout swipe; // 아래로 당겨서 더 로드하기 위한 레이아웃
    FloatingActionButton advSearchBtn; // 고급 검색 버튼 (현재 비활성화)
    TextView noresult; // 검색 결과 없음 텍스트
    private EditText searchBox; // 검색어 입력창
    RecyclerView searchResult; // 검색 결과를 보여줄 RecyclerView
    Spinner searchMode, baseMode; // 검색 종류(제목, 작가 등), 검색 대상(만화, 웹툰) 스피너
    TitleAdapter searchAdapter; // 검색 결과 어댑터
    Search search; // 검색 로직을 처리하는 객체
    Fragment fragment; // 프래그먼트 자신을 참조
    LinearLayoutCompat optionsPanel; // 검색 옵션(스피너)을 담는 패널
    String prequery = null; // 다른 곳에서 전달받은 검색어

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_search , container, false);

        // 뷰 초기화
        noresult = rootView.findViewById(R.id.noResult);
        searchBox = rootView.findViewById(R.id.searchBox);
        searchResult = rootView.findViewById(R.id.searchResult);
        searchResult.setLayoutManager(new NpaLinearLayoutManager(getContext()));
        searchMode = rootView.findViewById(R.id.searchMode);
        baseMode = rootView.findViewById(R.id.searchBaseMode);
        advSearchBtn = rootView.findViewById(R.id.advSearchBtn);
        swipe = rootView.findViewById(R.id.searchSwipe);
        optionsPanel = rootView.findViewById(R.id.searchOptionPanel);
        fragment = this;
        if(p.getDarkTheme()){
            searchMode.setPopupBackgroundResource(R.color.colorDarkWindowBackground);
            baseMode.setPopupBackgroundResource(R.color.colorDarkWindowBackground);
        }

        // 검색창에 포커스가 가면 옵션 패널을 보여줌
        searchBox.setOnFocusChangeListener((view, b) -> {
            if(b){
                optionsPanel.setVisibility(View.VISIBLE);
            }else{
                optionsPanel.setVisibility(View.GONE);
            }
        });

        // 고급 검색 버튼 (현재 비활성화)
        advSearchBtn.setOnClickListener(v -> {
            Toast.makeText(getContext(), "고급검색 기능 사용 불가", Toast.LENGTH_LONG).show();
        });

        // 검색창에서 엔터 키를 누르면 검색 실행
        searchBox.setOnKeyListener((v, keyCode, event) -> {
            if(event.getAction()==KeyEvent.ACTION_DOWN && keyCode ==KeyEvent.KEYCODE_ENTER){
                searchSubmit();
                return true;
            }
            return false;
        });

        // 스피너 선택 리스너
        AdapterView.OnItemSelectedListener mlistener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                optionUpdate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                optionUpdate();
            }
        };
        baseMode.setOnItemSelectedListener(mlistener);
        searchMode.setOnItemSelectedListener(mlistener);

        // 기본 검색 대상 설정
        baseMode.setSelection(p.getBaseMode()-1);


        // 아래로 당겨서 다음 페이지 로드
        swipe.setOnRefreshListener(direction -> {
            if(search==null) swipe.setRefreshing(false);
            else {
                if (!search.isLast()) {
                    new SearchManga().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else swipe.setRefreshing(false);
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 다른 곳에서 전달받은 검색어가 있으면 설정
        if(prequery != null){
            searchBox.setText(prequery);
            prequery = null;
        }
    }

    void optionUpdate(){
        // 옵션 변경 시 처리 (현재는 비어있음)
    }

    // 외부에서 검색어를 설정하기 위한 메서드
    public void setSearch(String prequery){
        this.prequery = prequery;
    }

    // 검색을 실행하는 메서드
    void searchSubmit(){
        String query = searchBox.getText().toString();
        if(query.length()>0) {
            swipe.setRefreshing(true);
            if(searchAdapter != null) searchAdapter.removeAll();
            else searchAdapter = new TitleAdapter(getContext());
            search = new Search(query,searchMode.getSelectedItemPosition(), baseMode.getSelectedItemPosition()+1);
            new SearchManga().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 캡차 해결 후 돌아왔을 때 검색 재시도
        if(resultCode == RESULT_CAPTCHA && searchAdapter!=null && search != null)
            searchSubmit();
    }

    // 검색을 비동기적으로 수행하는 AsyncTask
    private class SearchManga extends AsyncTask<String,String,Integer>{
        protected void onPreExecute(){
            super.onPreExecute();
        }
        protected Integer doInBackground(String... params){
            return search.fetch(httpClient);
        }
        @Override
        protected void onPostExecute(Integer res){
            super.onPostExecute(res);
            if(res != 0){
                // 오류 발생 시 (주로 캡차)
                Utils.showCaptchaPopup(getContext(), 4, fragment, p);
            }

            if(searchAdapter.getItemCount()==0) {
                // 첫 검색 결과일 경우 어댑터 새로 설정
                searchAdapter.addData(search.getResult());
                searchResult.setAdapter(searchAdapter);
                searchAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                    @Override
                    public void onLongClick(View view, int position) {
                        // 롱클릭 시 팝업 메뉴 표시 (즐겨찾기 추가/삭제)
                        Title title = searchAdapter.getItem(position);
                        popup(getContext(),view, position, title, 0, item -> {
                            switch(item.getItemId()){
                                case R.id.favAdd:
                                case R.id.favDel:
                                    p.toggleFavorite(title,0);
                                    break;
                            }
                            return false;
                        }, p);
                    }

                    @Override
                    public void onResumeClick(int position, int id) {
                        // 이어보기 클릭 시 바로 뷰어 실행
                        openViewer(getContext(),new Manga(id,"","", search.getBaseMode()),-1);
                    }

                    @Override
                    public void onItemClick(int position) {
                        // 아이템 클릭 시 에피소드 목록으로 이동
                        Intent episodeView = episodeIntent(getContext(), searchAdapter.getItem(position));
                        startActivity(episodeView);
                    }
                });
            }else{
                // 추가 검색 결과는 기존 어댑터에 추가
                searchAdapter.addData(search.getResult());
            }

            // 검색 결과 유무에 따라 "결과 없음" 텍스트 표시/숨김
            if(searchAdapter.getItemCount()>0) {
                noresult.setVisibility(View.GONE);
            }else{
                noresult.setVisibility(View.VISIBLE);
            }

            swipe.setRefreshing(false);
        }
    }
}

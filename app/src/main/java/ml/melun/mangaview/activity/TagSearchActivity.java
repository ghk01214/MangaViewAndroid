package ml.melun.mangaview.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.adapter.UpdatedAdapter;
import ml.melun.mangaview.mangaview.Bookmark;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.mangaview.UpdatedList;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.REQUEST_LOGIN;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.Utils.viewerIntent;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;
import static ml.melun.mangaview.mangaview.MTitle.base_comic;

// 태그, 작가, 발행 구분, 북마크 등 다양한 검색 결과를 표시하는 액티비티
public class TagSearchActivity extends AppCompatActivity {
    RecyclerView searchResult; // 검색 결과를 표시할 RecyclerView
    int mode; // 검색 모드 (0: 일반, 1: 작가, 2: 태그, 3: 초성, 4: 발행, 5: 최신 업데이트, 6: 검색 결과, 7: 북마크)
    String query; // 검색어
    TitleAdapter adapter; // 일반 검색 및 북마크 목록을 위한 어댑터
    UpdatedAdapter uadapter; // 최신 업데이트 만화 목록을 위한 어댑터
    Context context; // 현재 컨텍스트
    Search search; // 검색 기능을 담당하는 객체
    UpdatedList updated; // 최신 업데이트 만화 목록을 가져오는 객체
    TextView noresult; // 검색 결과가 없을 때 표시할 텍스트뷰
    SwipyRefreshLayout swipe; // 새로고침 기능을 위한 SwipyRefreshLayout
    Bookmark bookmark; // 북마크 기능을 담당하는 객체
    int baseMode; // 만화/웹툰 기본 모드


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(p.getDarkTheme()) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_search);
        context = this;
        searchResult = this.findViewById(R.id.tagSearchResult);
        noresult = this.findViewById(R.id.tagSearchNoResult);
        LinearLayoutManager lm = new NpaLinearLayoutManager(context);
        searchResult.setLayoutManager(lm);
        Intent i = getIntent();
        query = i.getStringExtra("query");
        mode = i.getIntExtra("mode",0);
        swipe = this.findViewById(R.id.tagSearchSwipe);
        baseMode = i.getIntExtra("baseMode", base_comic);

        ActionBar ab = getSupportActionBar();
        switch(mode){
            case 0:
                break;
            case 1:
                ab.setTitle("작가: "+query);
                break;
            case 2:
                ab.setTitle("태그: "+query);
                break;
            case 3:
            case 4:
                ab.setTitle("검색 결과");
                break;
            case 5:
                ab.setTitle("최신 업데이트");
                break;
            case 6:
                ab.setTitle("검색결과");
                break;
            case 7:
                ab.setTitle("북마크");
                break;
        }

        ab.setDisplayHomeAsUpEnabled(true);
        swipe.setRefreshing(true);

        if(mode == 5) {
            uadapter = new UpdatedAdapter(context);
            updated = new UpdatedList(p.getBaseMode());
            getUpdated gu = new getUpdated();
            gu.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            swipe.setOnRefreshListener(direction -> {
                 if (!updated.isLast()) {
                    getUpdated gu1 = new getUpdated();
                    gu1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else swipe.setRefreshing(false);
            });

        }else if(mode == 7){
            adapter = new TitleAdapter(context);
            bookmark = new Bookmark();
            getBookmarks gb = new getBookmarks();
            gb.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            swipe.setOnRefreshListener(direction -> {
                if (bookmark.isLast()) {
                    getBookmarks gb1 = new getBookmarks();
                    gb1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else swipe.setRefreshing(false);
            });

        }else {
            adapter = new TitleAdapter(context);
            search = new Search(query,mode,baseMode);
            searchManga sm = new searchManga();
            sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            swipe.setOnRefreshListener(direction -> {
                if (!search.isLast()) {
                    searchManga sm1 = new searchManga();
                    sm1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else swipe.setRefreshing(false);
            });
        }
    }
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_settings) {
            Intent settingIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingIntent, 0);
            return true;
        } else if (id == R.id.action_debug) {
            Intent debug = new Intent(this, DebugActivity.class);
            startActivity(debug);
            return true;
        } else if (id == R.id.action_select_from_calendar) {
            // 달력에서 날짜 선택
            if(mode == 5 && uadapter != null) {
                showDatePickerDialog();
            }
            return true;
        } else if (id == R.id.action_select_from_list) {
            // 목록에서 날짜 선택
            if(mode == 5 && uadapter != null) {
                showDateListDialog();
            }
            return true;
        } else if (id == R.id.action_show_all) {
            // 전체 보기
            if(mode == 5 && uadapter != null) {
                uadapter.clearDateFilter();
                getSupportActionBar().setTitle("최신 업데이트");
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(mode == 5) {
            // 최신 업데이트 모드일 때는 정렬 옵션이 있는 메뉴 사용
            getMenuInflater().inflate(R.menu.updated_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.main, menu);
        }
        return true;
    }

    // 달력을 이용한 날짜 선택 다이얼로그를 표시합니다.
    private void showDatePickerDialog() {
        // UTC+9 (KST) 기준으로 현재 날짜 설정
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+09:00"));
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            p.getDarkTheme() ? android.R.style.Theme_DeviceDefault_Dialog : android.R.style.Theme_DeviceDefault_Light_Dialog,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    // 선택된 날짜를 Calendar 객체로 생성
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    
                    // 선택된 날짜를 "일" 단위로 변환하여 필터링
                    filterByDay(selectedCalendar);
                }
            },
            year, month, day
        );
        
        // 달력에서 선택 가능한 최대 날짜를 UTC+9 기준 오늘로 제한
        Calendar maxDate = Calendar.getInstance(TimeZone.getTimeZone("GMT+09:00"));
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
        
        // 선택 가능한 최소 날짜를 1년 전으로 설정 (너무 오래된 날짜 방지)
        Calendar minDate = Calendar.getInstance(TimeZone.getTimeZone("GMT+09:00"));
        minDate.add(Calendar.YEAR, -1);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        
        datePickerDialog.show();
    }
    
    // 선택된 날짜(일)를 기준으로 만화를 필터링합니다.
    private void filterByDay(Calendar selectedDate) {
        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateStr = displayFormat.format(selectedDate.getTime());
        
        // 일 단위로 필터링
        uadapter.setDateFilterByDay(selectedDate);
        
        // 액션바 제목 업데이트
        getSupportActionBar().setTitle("최신 업데이트 (" + selectedDateStr + ")");
    }
    
    // 사용 가능한 날짜 목록을 보여주는 다이얼로그를 표시합니다.
    private void showDateListDialog() {
        ArrayList<String> availableDates = uadapter.getAvailableDates();
        
        if (availableDates.isEmpty()) {
            // 사용 가능한 날짜가 없는 경우
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (p.getDarkTheme()) builder = new AlertDialog.Builder(this, R.style.darkDialog);
            
            builder.setTitle("알림")
                   .setMessage("표시할 날짜가 없습니다.")
                   .setPositiveButton("확인", null)
                   .show();
            return;
        }
        
        // 날짜 목록을 배열로 변환
        String[] dateArray = availableDates.toArray(new String[0]);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (p.getDarkTheme()) builder = new AlertDialog.Builder(this, R.style.darkDialog);
        
        builder.setTitle("날짜 선택 (실제 데이터)")
               .setItems(dateArray, (dialog, which) -> {
                   String selectedDateStr = dateArray[which];
                   // 문자열 날짜로 직접 필터링
                   uadapter.setDateFilterByString(selectedDateStr);
                   
                   // 액션바 제목 업데이트
                   getSupportActionBar().setTitle("최신 업데이트 (" + selectedDateStr + ")");
               })
               .setNegativeButton("취소", null)
               .show();
    }


    private class getBookmarks extends AsyncTask<Void, Void, Integer>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            if(integer != 0){
                showCaptchaPopup(context, p);
            }
            if(adapter.getItemCount()==0) {
                adapter.addData(bookmark.getResult());
                searchResult.setAdapter(adapter);
                adapter.setClickListener(new TitleAdapter.ItemClickListener() {
                    @Override
                    public void onResumeClick(int position, int id) {
                        Intent viewer = viewerIntent(context, new Manga(id,"","",adapter.getItem(position).getBaseMode()));
                        viewer.putExtra("online",true);
                        startActivity(viewer);
                    }

                    @Override
                    public void onItemClick(int position) {
                        // start intent : Episode viewer
                        Title selected = adapter.getItem(position);
                        Intent episodeView = episodeIntent(context, selected);
                        episodeView.putExtra("online", true);
                        startActivity(episodeView);
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                        popup(view, position, adapter.getItem(position), 0);
                    }
                });
            }else{
                adapter.addData(bookmark.getResult());
            }

            if(adapter.getItemCount()>0) {
                noresult.setVisibility(View.GONE);
            }else{
                noresult.setVisibility(View.VISIBLE);
            }
            swipe.setRefreshing(false);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            return bookmark.fetch(httpClient);
        }
    }


    private class searchManga extends AsyncTask<String,String,Integer> {
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
                showCaptchaPopup(context, p);
            }
            if(adapter.getItemCount()==0) {
                adapter.addData(search.getResult());
                searchResult.setAdapter(adapter);
                adapter.setClickListener(new TitleAdapter.ItemClickListener() {
                    @Override
                    public void onResumeClick(int position, int id) {
                        Intent viewer = viewerIntent(context, new Manga(id,"","", search.getBaseMode()));
                        viewer.putExtra("online",true);
                        startActivity(viewer);
                    }

                    @Override
                    public void onItemClick(int position) {
                        // start intent : Episode viewer
                        Title selected = adapter.getItem(position);
                        Intent episodeView = episodeIntent(context, selected);
                        episodeView.putExtra("online", true);
                        startActivity(episodeView);
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                        popup(view, position, adapter.getItem(position), 0);
                    }
                });
            }else{
                adapter.addData(search.getResult());
            }

            if(adapter.getItemCount()>0) {
                noresult.setVisibility(View.GONE);
            }else{
                noresult.setVisibility(View.VISIBLE);
            }
            swipe.setRefreshing(false);
        }
    }

    private class getUpdated extends AsyncTask<String,String,String> {
        protected void onPreExecute(){
            super.onPreExecute();
        }
        protected String doInBackground(String... params){
            updated.fetch(httpClient);
            return null;
        }
        @Override
        protected void onPostExecute(String res){
            super.onPostExecute(res);
            if(updated.getResult().size() == 0 && uadapter.getItemCount() == 0){
                //error
                showCaptchaPopup(context, p);
            }
            if(uadapter.getItemCount()==0) {
                uadapter.addData(updated.getResult());
                searchResult.setAdapter(uadapter);
                uadapter.setOnClickListener(new UpdatedAdapter.onclickListener() {
                    @Override
                    public void onEpsClick(Title t) {
                        Intent eps = episodeIntent(context, t);
                        eps.putExtra("online", true);
                        startActivity(eps);
                    }

                    @Override
                    public void onClick(Manga m) {
                        //open viewer
                        Intent viewer = viewerIntent(context, m);
                        viewer.putExtra("online", true);
                        startActivityForResult(viewer,0);
                    }
                });
            }else{
                uadapter.addData(updated.getResult());
            }

            if(uadapter.getItemCount()>0) {
                noresult.setVisibility(View.GONE);
            }else{
                noresult.setVisibility(View.VISIBLE);
            }
            swipe.setRefreshing(false);
        }
    }
    void popup(View view, final int position, final Title title, final int m){
        PopupMenu popup = new PopupMenu(TagSearchActivity.this, view);
        //Inflating the Popup using xml file
        popup.getMenuInflater()
                .inflate(R.menu.title_options, popup.getMenu());
        popup.getMenu().removeItem(R.id.del);
        popup.getMenu().findItem(R.id.favAdd).setVisible(true);
        popup.getMenu().findItem(R.id.favDel).setVisible(true);
        if(p.findFavorite(title)>-1) popup.getMenu().removeItem(R.id.favAdd);
        else popup.getMenu().removeItem(R.id.favDel);


        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(item -> {
            switch(item.getItemId()){
                case R.id.del:
                    break;
                case R.id.favAdd:
                case R.id.favDel:
                    //toggle favorite
                    p.toggleFavorite(title,0);
                    break;
            }
            return true;
        });
        popup.show(); //showing popup menu
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_LOGIN){
            //login
            finish();
            startActivity(getIntent());
        }
        if(resultCode == RESULT_CAPTCHA){
            //captcha
            finish();
            startActivity(getIntent());
        }
    }
}

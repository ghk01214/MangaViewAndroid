package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import ml.melun.mangaview.Notice;
import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import okhttp3.Response;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.Utils.showPopup;

// 공지사항을 표시하는 액티비티
public class NoticesActivity extends AppCompatActivity {
    Boolean dark; // 다크 테마 여부
    Context context;
    List<Notice> notices; // 공지사항 목록
    SharedPreferences sharedPref; // 공지사항 저장을 위한 SharedPreferences
    ListView list; // 공지사항 목록을 표시할 ListView
    SwipyRefreshLayout swipe; // 아래로 당겨서 새로고침 기능을 위한 SwipyRefreshLayout
    ProgressBar progress; // 로딩 프로그레스바
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 테마 설정 (다크 모드 여부에 따라)
        if(dark = new Preference(this).getDarkTheme())setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notices);
        context = this;

        // 뷰 요소 초기화
        swipe = this.findViewById(R.id.noticeSwipe);
        list = this.findViewById(R.id.noticeList);
        progress = this.findViewById(R.id.progress);

        // 액션바 설정
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true); // 뒤로가기 버튼 활성화

        notices = new ArrayList<>();
        sharedPref = this.getSharedPreferences("mangaView", Context.MODE_PRIVATE);

        // 기존 공지사항 데이터 초기화 및 로드
        sharedPref.edit().putString("notices","").commit(); // 이전 notices 키 초기화 (오타 수정)
        notices = new Gson().fromJson(sharedPref.getString("notice", "[]"), new TypeToken<List<Notice>>(){}.getType());

        // null 객체 제거
        for(int i=notices.size()-1; i>=0;i--){
            if(notices.get(i)==null) notices.remove(i);
        }

        progress.setVisibility(View.VISIBLE); // 로딩 프로그레스바 표시

        // 아래로 당겨서 새로고침 리스너 설정
        swipe.setOnRefreshListener(direction -> {
            getNotices gn = new getNotices();
            gn.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });

        // 초기 공지사항 로드
        getNotices gn = new getNotices();
        gn.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    void showNotice(Notice notice){
        showPopup(context,notice.getTitle(),notice.getDate()+"\n\n"+notice.getContent());
    }


    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void populate(){
        //save notice titles to array
        try{
            String[] list = new String[notices.size()];
            for(int i=0;i<notices.size();i++){
                String title = notices.get(i).getTitle();
                if(title == null)
                    list[i] = "";
                else
                    list[i] = notices.get(i).getId() + ". " + title;
            }
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
            //populate listview and set click listener
            this.list.setAdapter(adapter);
            this.list.setOnItemClickListener((adapterView, view, position, id) -> {
                Notice target = notices.get(position);
                showNotice(target);
            });
        }catch (Exception e){
            showPopup(context,"오류",e.getMessage());
            e.printStackTrace();
        }
        //create arrayAdapter

    }

    private class getNotices extends AsyncTask<Void, Void, Integer> {
        List<Notice> loaded;
        protected void onPreExecute() {
            super.onPreExecute();
            sharedPref.edit().putLong("lastNoticeTime", System.currentTimeMillis()).commit();
        }
        protected Integer doInBackground(Void... params) {
            //mget all notices
            try {
                Response response = httpClient.get("https://raw.githubusercontent.com/junheah/MangaViewAndroid/master/etc/notices.json", new HashMap<>());
                String rawdata = response.body().string();
                response.close();
                loaded = new Gson().fromJson(rawdata, new TypeToken<List<Notice>>(){}.getType());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            try {
                for(Notice n: loaded){
                    if(n!=null){
                        int index = notices.indexOf(n);
                        if(index>-1) notices.set(index, n);
                        else notices.add(n);
                    }
                }
                Collections.sort(notices, (n1, n2) -> {
                    if(n1.getId() < n2.getId())
                        return 1;
                    else
                        return -1;
                });
            }catch (Exception e){
                //probably offline
            }
            sharedPref.edit().putString("notice", new Gson().toJson(notices)).commit();
            swipe.setRefreshing(false);
            progress.setVisibility(View.GONE);
            populate();
        }
    }
}

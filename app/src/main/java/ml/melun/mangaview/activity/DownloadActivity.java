package ml.melun.mangaview.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;

import ml.melun.mangaview.Downloader;
import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.SelectEpisodeAdapter;
import ml.melun.mangaview.mangaview.DownloadTitle;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.p;

// 만화 에피소드를 다운로드하는 액티비티
public class DownloadActivity extends AppCompatActivity {
    Title title; // 다운로드할 만화 작품 객체
    SelectEpisodeAdapter adapter; // 에피소드 선택 어댑터
    RecyclerView eplist; // 에피소드 목록을 표시할 RecyclerView
    boolean dark; // 다크 테마 여부
    JSONArray selected; // 선택된 에피소드들의 인덱스
    boolean singleSelect = true; // 단일 선택 모드 여부

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        eplist = this.findViewById(R.id.dl_eplist);
        Intent intent = getIntent();
        try {
            title = new Gson().fromJson(intent.getStringExtra("title"),new TypeToken<Title>(){}.getType());
            eplist.setLayoutManager(new NpaLinearLayoutManager(this));
            adapter = new SelectEpisodeAdapter(getApplicationContext(),title.getEps());
            adapter.setClickListener((view, position) -> adapter.select(position));
            eplist.setAdapter(adapter);
        }catch (Exception e){
            e.printStackTrace();
        }
        Button dl = findViewById(R.id.dl_btn);
        dl.setOnClickListener(v -> {
            if(adapter.getSelected(false).length()>0) {
                selected = adapter.getSelected(false);
                downloadClick();
            }else{
                Toast.makeText(getApplication(),"1개 이상의 화를 선택해 주세요", Toast.LENGTH_SHORT).show();
            }
        });
        Button dlAll = findViewById(R.id.dl_all_btn);
        dlAll.setOnClickListener(v -> {
            selected = adapter.getSelected(true);
            downloadClick();
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button selectionMode = findViewById(R.id.dl_mode_btn);
        selectionMode.setOnClickListener(view -> {
            if(singleSelect){
                singleSelect = false;
                selectionMode.setText("범위 선택 모드");
                adapter.setSelectionMode(singleSelect);
            }else{
                singleSelect = true;
                selectionMode.setText("단일 선택 모드");
                adapter.setSelectionMode(singleSelect);
            }
        });
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    private void downloadClick(){
        //download manga
        //ask for confirmation
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked
                    //check if download service is up and running
                    Intent downloader = new Intent(getApplicationContext(),Downloader.class);
                    downloader.setAction(Downloader.ACTION_QUEUE);
                    downloader.putExtra("title", new Gson().toJson(new DownloadTitle(title)));
                    downloader.putExtra("selected", selected.toString());

                    if (Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(downloader);
                    }else{
                        startService(downloader);
                    }
                    //queue title to service
                    Toast.makeText(getApplication(),"다운로드를 시작합니다.", Toast.LENGTH_LONG).show();
                    finish();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        };
        AlertDialog.Builder builder;
        if(dark) builder = new AlertDialog.Builder(this,R.style.darkDialog);
        else builder = new AlertDialog.Builder(this);
        builder.setMessage(title.getName()+ " 을(를) 다운로드 하시겠습니까?\n[ 총 "+selected.length()+"화 ]").setPositiveButton("네", dialogClickListener)
                .setNegativeButton("아니오", dialogClickListener).show();
    }
}

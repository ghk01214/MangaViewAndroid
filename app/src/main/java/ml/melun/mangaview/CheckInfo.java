package ml.melun.mangaview;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import ml.melun.mangaview.mangaview.CustomHttpClient;
import okhttp3.Response;

import static ml.melun.mangaview.Utils.checkConnection;
import static ml.melun.mangaview.Utils.showPopup;

// 앱의 업데이트, 공지사항 등을 확인하는 클래스
public class CheckInfo {
    Context context; // 애플리케이션 컨텍스트
    updateCheck uc; // 업데이트 확인을 위한 AsyncTask
    noticeCheck nc; // 공지사항 확인을 위한 AsyncTask
    SharedPreferences sharedPref; // 앱 설정을 저장하는 SharedPreferences
    CustomHttpClient client; // HTTP 통신을 위한 CustomHttpClient
    boolean silent = false; // 사용자에게 알림을 표시할지 여부
    public static final int COLOR_DARK = 1; // 다크 테마 상수
    public static final int COLOR_AUTO = 0; // 자동 테마 상수
    public static final int COLOR_LIGHT = 2; // 라이트 테마 상수
    int colormode = 0; // 현재 설정된 테마 색상 모드

    // 테마 색상 모드를 설정합니다.
    public void setColorMode(int c){
        this.colormode = c;
    }

    // 클래스 생성자. 초기화를 수행합니다。
    public CheckInfo(Context context, CustomHttpClient client, boolean silent){
        this.silent = silent;
        this.context = context;
        this.client = client;
        uc = new updateCheck();
        nc = new noticeCheck();
        sharedPref = context.getSharedPreferences("mangaView",Context.MODE_PRIVATE);
    }

    // 업데이트와 공지사항 확인을 모두 수행합니다.
    public void all(Boolean force){
        if(checkConnection(context)){
            // 인터넷 연결이 있을 때만 실행
            update(force);
            notice(force);
        }
    }

    // 앱 업데이트를 확인합니다.
    public Boolean update(Boolean force){
        if(!checkConnection(context)){
            if(!silent) Toast.makeText(context, "네트워크 연결이 없습니다.", Toast.LENGTH_SHORT).show();
            return false;
        }
        Long lastUpdateTime = sharedPref.getLong("lastUpdateTime", 0); // 마지막 업데이트 확인 시간
        Long updateCycle = sharedPref.getLong("updateCycle",900000); // 업데이트 확인 주기 (기본 15분)
        if(uc.getStatus()== AsyncTask.Status.RUNNING) {
            if(!silent) Toast.makeText(context, "이미 실행중입니다. 잠시후에 다시 시도해 주세요",Toast.LENGTH_SHORT).show();
            return false;
        }
        else{
            // 마지막 확인 시간 + 주기가 지났거나 강제 실행일 경우 업데이트 확인 실행
            if ((System.currentTimeMillis()>lastUpdateTime + updateCycle) || force)
                uc.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return true;
        }
    }

    // 새로운 공지사항을 확인합니다.
    public Boolean notice(Boolean force) {
        if(!checkConnection(context)){
            if(!silent) Toast.makeText(context, "네트워크 연결이 없습니다.", Toast.LENGTH_SHORT).show();
            return false;
        }
        Long lastUpdateTime = sharedPref.getLong("lastNoticeTime", 0); // 마지막 공지 확인 시간
        Long updateCycle = sharedPref.getLong("noticeCycle",900000); // 공지 확인 주기 (기본 15분)
        if (nc.getStatus() == AsyncTask.Status.RUNNING){
            if(!silent) Toast.makeText(context, "이미 실행중입니다. 잠시후에 다시 시도해 주세요",Toast.LENGTH_SHORT).show();
            return false;
        }
        else {
            // 마지막 확인 시간 + 주기가 지났거나 강제 실행일 경우 공지사항 확인 실행
            if ((System.currentTimeMillis()>lastUpdateTime + updateCycle) || force)
                nc.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return true;
        }
    }


    // 공지사항을 비동기적으로 확인하는 내부 클래스
    private class noticeCheck extends AsyncTask<Void, Void, Integer> {
        Notice notice; // 공지사항 데이터 모델

        // 작업 시작 전 마지막 확인 시간을 기록합니다.
        protected void onPreExecute() {
            super.onPreExecute();
            sharedPref.edit().putLong("lastNoticeTime", System.currentTimeMillis()).commit();
        }

        // 백그라운드에서 공지사항 JSON을 가져와 파싱합니다.
        protected Integer doInBackground(Void... params) {
            try {
                Response response = client.get("https://raw.githubusercontent.com/junheah/MangaViewAndroid/master/etc/notice.json", new HashMap<>());
                String rawdata = response.body().string();
                response.close();
                notice = new Gson().fromJson(rawdata, new TypeToken<Notice>(){}.getType());
                if(notice.id == -1) return -1; // 공지사항 id가 -1이면 실패로 간주
            }catch (Exception e){
                e.printStackTrace();
                return -1;
            }
            return 0;
        }

        // 작업 완료 후 결과를 받아 공지사항을 표시합니다.
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if(result == 0) showNotice(notice);
        }
    }

    // 앱 업데이트를 비동기적으로 확인하는 내부 클래스
    public class updateCheck extends AsyncTask<Void, Integer, Integer> {
        int version = 0; // 현재 앱 버전
        int newVersion = 0; // 최신 앱 버전
        JSONObject data; // 업데이트 정보 JSON 데이터

        // 작업 시작 전 현재 버전 정보를 가져오고 마지막 확인 시간을 기록합니다.
        protected void onPreExecute() {
            sharedPref.edit().putLong("lastUpdateTime", System.currentTimeMillis()).commit();
            super.onPreExecute();
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                version = pInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if(!silent) Toast.makeText(context, "업데이트 확인중..", Toast.LENGTH_SHORT).show();
        }

        // 백그라운드에서 최신 릴리즈 정보를 가져와 현재 버전과 비교합니다.
        protected Integer doInBackground(Void... params) {
            try {
                Response response = client.get("https://api.github.com/repos/junheah/MangaViewAndroid/releases/latest", new HashMap<>());
                String rawdata = response.body().string();
                response.close();
                data = new JSONObject(rawdata);
                newVersion = Integer.parseInt(data.getString("tag_name"));
                if(version<newVersion) {
                    return 1; // 새로운 버전이 있음
                }
            }catch(Exception e){
                e.printStackTrace();
                return -1; // 오류 발생
            }
            return 0; // 최신 버전임
        }

        // 작업 완료 후 결과에 따라 업데이트 프롬프트를 표시하거나 메시지를 보여줍니다.
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            switch(result){
                case -1:
                    if(!silent) Toast.makeText(context, "오류가 발생했습니다. 나중에 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                    break;
                case 0:
                    if(!silent) Toast.makeText(context, "최신버전 입니다.", Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    showPrompt(data); // 업데이트 프롬프트 표시
                    break;
            }
        }
    }

    // 사용자에게 업데이트 정보를 보여주고 다운로드 여부를 묻는 다이얼로그를 표시합니다.
    void showPrompt(JSONObject data){
        showPrompt(data, false);
    }

    // showPrompt의 오버로드된 메서드로, 라이트 테마 강제 여부를 설정할 수 있습니다.
    void showPrompt(JSONObject data, boolean forceLight){
        try{
            final String page = "https://junheah.github.io/MangaViewAndroid/"; // 앱 소개 페이지
            final String message = "버전: " + data.getString("tag_name") +"\n체인지 로그:\n"+ data.getString("body"); // 업데이트 내용
            final String url = data.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"); // 다운로드 URL
            AlertDialog.Builder builder;
            switch (colormode){
                case COLOR_DARK:
                    builder = new AlertDialog.Builder(context,R.style.darkDialog);
                    break;
                case COLOR_LIGHT:
                    builder = new AlertDialog.Builder(context);
                    break;
                default:
                case COLOR_AUTO:
                    if(new Preference(context).getDarkTheme()) builder = new AlertDialog.Builder(context,R.style.darkDialog);
                    else builder = new AlertDialog.Builder(context);
            }

            builder.setTitle("업데이트")
                    .setMessage(message)
                    .setPositiveButton("다운로드", (dialog, button) -> {
                        // 다운로더 서비스 시작
                        Intent downloader = new Intent(context.getApplicationContext(),Downloader.class);
                        downloader.setAction(Downloader.ACTION_UPDATE);
                        downloader.putExtra("url", url);
                        if (Build.VERSION.SDK_INT >= 26) {
                            context.startForegroundService(downloader);
                        }else{
                            context.startService(downloader);
                        }
                        Toast.makeText(context,"다운로드를 시작합니다.",Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("나중에", (dialog, button) -> {
                        // 아무것도 하지 않음
                    })
                    .setNeutralButton("사이트로 이동", (dialog, which) -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(page))))
                    .show();
        }catch (Exception e){
            e.printStackTrace();
            showPopup(context,"업데이터", "오류 발생");
        }
    }

    // 사용자에게 새로운 공지사항을 보여주고, 확인한 공지사항 목록에 추가합니다.
    void showNotice(Notice notice){
        try {
            SharedPreferences sharedPref = context.getSharedPreferences("mangaView", Context.MODE_PRIVATE);
            // 기존에 확인한 공지사항 목록 불러오기
            List<Notice> notices = new Gson().fromJson(sharedPref.getString("notice", "[]"), new TypeToken<List<Notice>>(){}.getType());
            if(notice!=null&&!notices.contains(notice)){
                // 새로운 공지사항이고, 아직 확인하지 않은 공지일 경우
                // 확인한 공지 목록에 추가
                notices.add(notice);
                // SharedPreferences에 저장
                sharedPref.edit().putString("notice", new Gson().toJson(notices)).commit();
                // 팝업으로 공지 내용 표시
                showPopup(context,notice.getTitle(),notice.getDate()+"\n\n"+notice.getContent());
            }

        }catch (Exception e){e.printStackTrace();}
    }

}

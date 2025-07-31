package ml.melun.mangaview;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Response;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

// URL을 자동으로 업데이트하는 AsyncTask
public class UrlUpdater extends AsyncTask<Void, Void, Boolean> {
    String result; // 업데이트된 URL
    String fetchUrl; // URL을 가져올 기본 주소
    boolean silent = false; // 토스트 메시지 표시 여부
    Context c; // 컨텍스트
    UrlUpdaterCallback callback; // 업데이트 완료 후 호출될 콜백

    // 생성자 (기본 URL 사용)
    public UrlUpdater(Context c){
        this.c = c;
        this.fetchUrl = p.getDefUrl();
    }

    // 생성자 (사일런트 모드, 콜백, 기본 URL 지정 가능)
    public UrlUpdater(Context c, boolean silent, UrlUpdaterCallback callback, String defUrl){
        this.c = c;
        this.silent = silent;
        this.callback = callback;
        this.fetchUrl = defUrl;
    }

    // 작업 시작 전 호출. 로딩 메시지를 표시합니다.
    protected void onPreExecute() {
        if(!silent) Toast.makeText(c, "자동 URL 설정중...", Toast.LENGTH_SHORT).show();
    }

    // 백그라운드에서 URL을 가져오는 작업을 수행합니다.
    protected Boolean doInBackground(Void... params) {
        return fetch();
    }

    // 실제 URL을 가져오는 메서드
    protected Boolean fetch(){
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.0.2; en-us; Galaxy Nexus Build/ICL53F) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/534.30");
            Response r = httpClient.get(fetchUrl, headers);
            if (r.code() == 302) { // 302 리다이렉션은 성공을 의미
                result = r.header("Location"); // 리다이렉션된 URL을 가져옴
                r.close();
                return true;
            } else{
                r.close();
                return false;
            }

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    // 작업 완료 후 호출. 결과에 따라 토스트 메시지를 표시하고 콜백을 호출합니다.
    protected void onPostExecute(Boolean r) {
        if(r && result !=null){
            p.setUrl(result);
            if(!silent)Toast.makeText(c, "자동 URL 설정 완료!", Toast.LENGTH_SHORT).show();
            if(callback!=null) callback.callback(true);
        }else{
            if(!silent)Toast.makeText(c, "자동 URL 설정 실패, 잠시후 다시 시도해 주세요", Toast.LENGTH_LONG).show();
            if(callback!=null) callback.callback(false);
        }
    }

    // URL 업데이트 완료 후 호출될 콜백 인터페이스
    public interface UrlUpdaterCallback{
        void callback(boolean success);
    }
}

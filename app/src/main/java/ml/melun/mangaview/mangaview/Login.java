package ml.melun.mangaview.mangaview;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.melun.mangaview.Preference;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.lang.System.currentTimeMillis;
import static ml.melun.mangaview.MainApplication.p;

// 로그인 과정을 처리하는 클래스
public class Login {
    private String user; // 사용자 아이디
    private String pass; // 사용자 비밀번호
    String cookie = ""; // 로그인 세션 유지를 위한 PHPSESSID 쿠키
    long currentTime = 0; // 캡차 이미지 요청에 사용되는 타임스탬프

    public Login(){
    }

    // 로그인에 사용할 아이디와 비밀번호를 설정합니다.
    public void set(String id, String pass){
        this.user = id;
        this.pass = pass;
    }

    // 로그인 준비 단계로, 캡차 이미지를 요청하고 세션 쿠키를 받아옵니다.
    public byte[] prepare(CustomHttpClient client, Preference p){
        Response r;
        int tries = 3; // 최대 3번 재시도
        while(tries > 0) {
            // kcaptcha_session.php에 요청하여 PHPSESSID 쿠키를 얻습니다.
            r = client.post(p.getUrl() + "/plugin/kcaptcha/kcaptcha_session.php", new FormBody.Builder().build(), new HashMap<>(),false);
            if(r.code() == 200) {
                List<String> setcookie = r.headers("Set-Cookie");
                for (String c : setcookie) {
                    if (c.contains("PHPSESSID=")) {
                        cookie = c.substring(c.indexOf("=") + 1, c.indexOf(";"));
                        client.setCookie("PHPSESSID",cookie);
                    }
                }
                break; // 성공 시 루프 종료
            }else {
                r.close();
                tries--;
            }
        }
        // 현재 시간을 기반으로 캡차 이미지를 요청합니다.
        currentTime = currentTimeMillis();
        r = client.mget("/plugin/kcaptcha/kcaptcha_image.php?t=" + currentTime, false);
        try {
            // 캡차 이미지를 byte 배열로 반환합니다.
            return r.body().bytes();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    // 아이디, 비밀번호, 캡차 답변으로 실제 로그인을 시도합니다.
    public Boolean submit(CustomHttpClient client, String answer){
        try{
            // 로그인 폼 데이터 생성
            RequestBody requestBody = new FormBody.Builder()
                    .addEncoded("auto_login", "on") // 자동 로그인 체크
                    .addEncoded("mb_id",user)
                    .addEncoded("mb_password",pass)
                    .addEncoded("captcha_key", answer)
                    .build();
            Map<String,String> headers = new HashMap<>();
            headers.put("Cookie", "PHPSESSID="+cookie+";");

            // login_check.php로 로그인 요청
            Response response = client.post(p.getUrl() + "/bbs/login_check.php", requestBody, headers);
            int responseCode = response.code();

            if(responseCode == 302) { // 302 리다이렉트 코드는 로그인 성공을 의미
                // 리다이렉션을 따라가서 로그인 완료 처리
                Map<String, String> cookies = new HashMap<>();
                cookies.put("PHPSESSID", cookie);
                client.mget("/?captcha_key="+answer+"&auto_login=on",false, cookies);
                response.close();
                return true; // 성공
            }
            else{
                response.close();
                return false; // 실패
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    // (사용되지 않는 것으로 보임) 쿠키 맵을 빌드합니다.
    public void buildCookie(Map<String,String> map){
        //java always passes by reference
        map.put("PHPSESSID", cookie);
    }

    // 로그인 세션(쿠키)이 유효한지 확인합니다.
    public boolean isValid(){
        return cookie !=null && cookie.length()>0;
    }

    // 저장된 세션 쿠키를 반환합니다.
    public String getCookie(Boolean format){
        if(format) return "PHPSESSID=" +cookie +';';
        return cookie;
    }

    public String getCookie() {
        return cookie;
    }
}

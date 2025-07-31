package ml.melun.mangaview.mangaview;


import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.CODE_SCOPED_STORAGE;

// OkHttp를 기반으로 한 커스텀 HTTP 클라이언트 클래스
public class CustomHttpClient {
    public OkHttpClient client; // OkHttp 클라이언트 인스턴스
    Map<String, String> cookies; // 세션 유지를 위한 쿠키 저장 맵
    public String agent = "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"; // 기본 User-Agent

    public CustomHttpClient(){
        System.out.println("http client create");
        this.cookies = new HashMap<>();
        // 안드로이드 구버전(10 미만)을 위한 TLS 설정
        if(android.os.Build.VERSION.SDK_INT < CODE_SCOPED_STORAGE) {
            // 일부 서버와의 호환성을 위해 레거시 암호화 스위트 추가
            // https://github.com/square/okhttp/issues/4053
            List<CipherSuite> cipherSuites = new ArrayList<>(ConnectionSpec.MODERN_TLS.cipherSuites());
            cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA);
            cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA);

            ConnectionSpec legacyTls = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .cipherSuites(cipherSuites.toArray(new CipherSuite[0]))
                    .build();

            // SSL 인증서를 신뢰하지 않는 클라이언트 빌더에 레거시 TLS 설정을 추가하여 클라이언트 생성
            this.client = getUnsafeOkHttpClient()
                    .connectionSpecs(Arrays.asList(legacyTls, ConnectionSpec.CLEARTEXT))
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();
        }else {
            // 안드로이드 10 이상에서는 기본 설정 사용
            this.client = getUnsafeOkHttpClient()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();
        }
    }

    // 쿠키를 설정합니다.
    public void setCookie(String k, String v){
        cookies.put(k, v);
    }
    // 모든 쿠키를 초기화합니다.
    public void resetCookie(){
        this.cookies = new HashMap<>();
    }

    // 특정 키의 쿠키 값을 가져옵니다.
    public String getCookie(String k){
        return cookies.get(k);
    }

    // 지정된 URL로 GET 요청을 보냅니다.
    public Response get(String url, Map<String, String> headers){
        Response response;
        try {
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .get();
            // 헤더 추가
            if(headers !=null)
                for(String k : headers.keySet()){
                    builder.addHeader(k, headers.get(k));
                }

            Request request = builder.build();
            response = this.client.newCall(request).execute();
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
        return response;
    }

    // 앱의 기본 URL과 경로를 조합하여 GET 요청을 보냅니다. (로그인 쿠키 포함 여부 설정 가능)
    public Response mget(String url, Boolean doLogin){
        return mget(url, doLogin, new HashMap<>());
    }
    // mget의 오버로드된 메서드 (기본적으로 로그인 쿠키 포함)
    public Response mget(String url){
        return mget(url,true);
    }


    // 앱의 기본 URL을 반환합니다.
    public String getUrl(){
        return p.getUrl();
    }


    // mget의 핵심 구현 메서드. 로컬 쿠키와 커스텀 쿠키를 조합하여 요청을 보냅니다.
    public Response mget(String url, Boolean doLogin, Map<String, String> customCookie){
        if(customCookie==null)
            customCookie = new HashMap<>();

        Map<String, String> cookie = new HashMap<>(this.cookies);
        cookie.putAll(customCookie);

        // 쿠키 맵을 문자열로 변환
        StringBuilder cbuilder = new StringBuilder();
        for(String key : cookie.keySet()){
            cbuilder.append(key);
            cbuilder.append('=');
            cbuilder.append(cookie.get(key));
            cbuilder.append("; ");
        }
        if(cbuilder.length()>2)
            cbuilder.delete(cbuilder.length()-2,cbuilder.length());

        // 헤더 설정 (쿠키, User-Agent, Referer)
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", cbuilder.toString());
        headers.put("User-Agent", agent);
        headers.put("Referer",p.getUrl());

        return get(p.getUrl()+url, headers);
    }

    // 지정된 URL로 POST 요청을 보냅니다.
    public Response post(String url, RequestBody body, Map<String,String> headers){
        return post(url,body,headers,false);
    }

    // POST 요청의 핵심 구현 메서드. 로컬 쿠키 포함 여부를 설정할 수 있습니다.
    public Response post(String url, RequestBody body, Map<String,String> headers, boolean localCookies){

        StringBuilder cs = new StringBuilder();
        // 헤더에 이미 쿠키가 있으면 가져옴
        if(headers.get("Cookie") != null)
            cs.append(headers.get("Cookie"));

        // 로컬 쿠키 추가
        if(localCookies)
            for(String key : this.cookies.keySet()){
                cs.append(key).append('=').append(this.cookies.get(key)).append("; ");
            }

        headers.put("Cookie", cs.toString());

        Response response = null;
        try {
            Request.Builder builder = new Request.Builder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36")
                    .url(url)
                    .post(body);

            for(String key: headers.keySet()){
                builder.addHeader(key, headers.get(key));
            }

            Request request = builder.build();
            response = this.client.newCall(request).execute();
        }catch (Exception e){
            e.printStackTrace();
        }
        return response;

    }


    public Response post(String url, RequestBody body){
        return post(url, body, new HashMap<>());
    }

    /*
    모든 SSL 인증서를 신뢰하는 OkHttpClient.Builder를 생성합니다.
    code source : https://gist.github.com/chalup/8706740
     */
    private static OkHttpClient.Builder getUnsafeOkHttpClient() {
        try {
            // 모든 인증서를 신뢰하는 TrustManager 생성
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType){
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType){
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            // TrustManager를 설치
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // SSLSocketFactory 생성
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // 생성된 SSLSocketFactory와 HostnameVerifier를 사용하여 OkHttpClient.Builder 반환
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}

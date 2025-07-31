package ml.melun.mangaview.mangaview;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ml.melun.mangaview.Preference;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.getNumberFromString;


// 만화 제목(작품)에 대한 상세 정보를 담는 클래스. MTitle을 상속받아 에피소드 목록, 북마크 정보 등을 추가로 가집니다.
public class Title extends MTitle {
    private List<Manga> eps = null; // 에피소드(Manga) 목록
    int bookmark = 0; // 로컬에 저장된 마지막으로 본 에피소드 위치
    Boolean bookmarked = false; // 서버에 북마크(선호작) 등록 여부
    String bookmarkLink = ""; // 북마크 토글에 사용되는 URL
    int rc = 0; // 추천 수

    // 배터리 상태 상수 (사용되지 않는 것으로 보임)
    public static final int BATTERY_EMPTY = 0;
    public static final int BATTERY_ONE_QUARTER = 1;
    public static final int BATTERY_HALF = 2;
    public static final int BATTERY_THREE_QUARTER = 3;
    public static final int BATTERY_FULL = 4;

    // 페이지 로딩 상태 상수
    public static final int LOAD_OK = 0; // 성공
    public static final int LOAD_CAPTCHA = 1; // 캡차 요구


    // 생성자
    public Title(String n, String t, String a, List<String> tg, String r, int id, int baseMode) {
        super(n, id, t, a, tg, r, baseMode);
    }

    // 작품 페이지 URL을 반환합니다.
    public String getUrl(){
        return '/' + baseModeStr(baseMode) + '/' + id;
    }


    // MTitle 객체로부터 Title 객체를 생성합니다.
    public Title(MTitle title){
        super(title.getName(), title.getId(), title.getThumb(), title.getAuthor(), title.getTags(), title.getRelease(), title.getBaseMode());
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString()  + " . " + eps;
    }

    // 에피소드 목록을 반환합니다.
    public List<Manga> getEps(){
        return eps;
    }

    // 서버 북마크(선호작) 여부를 반환합니다.
    public Boolean getBookmarked() {
        if(bookmarked==null) return false;
        return bookmarked;
    }

    // 웹에서 에피소드 목록과 상세 정보를 가져옵니다.
    public int fetchEps(CustomHttpClient client) {

        try {
            Response r = client.mget('/' + baseModeStr(baseMode) + '/' + id);
            // 웹툰의 경우 캡차가 있을 수 있음.
            if(r.code() == 302 && r.header("location").contains("captcha.php")){
                return LOAD_CAPTCHA;
            }
            String body = r.body().string();
            if(body.contains("Connect Error: Connection timed out")){
                // 광고 차단 등으로 인한 타임아웃 시 재시도
                r.close();
                fetchEps(client);
                return LOAD_OK;
            }
            Document d = Jsoup.parse(body);
            Element header = d.selectFirst("div.view-title");

            // 추가 정보 파싱 (추천, 북마크)
            try{
                Element infoTable = d.selectFirst("table.table");
                // 추천 수
                rc = Integer.parseInt(infoTable.selectFirst("button.btn-red").selectFirst("b").ownText());
                // 북마크 정보
                Element bookmark = infoTable.selectFirst("a#webtoon_bookmark");
                if(bookmark != null) {
                    // 로그인 상태
                    bookmarked = bookmark.hasClass("btn-orangered"); // btn-orangered 클래스가 있으면 북마크된 상태
                    bookmarkLink = bookmark.attr("href");
                }else{
                    // 비로그인 상태
                    bookmarked = false;
                    bookmarkLink = "";
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            // 썸네일 이미지
            try {
                thumb = header.selectFirst("div.view-img").selectFirst("img").attr("src");
            }catch (Exception e){}

            Elements infos = header.select("div.view-content");
            // 제목
            try {
                name = infos.get(1).selectFirst("b").ownText();
            }catch (Exception e){}
            tags = new ArrayList<>();

            // 작가, 분류, 발행구분 정보 파싱
            for(int i=1; i<infos.size(); i++){
                Element e = infos.get(i);
                try {
                    String type = e.selectFirst("strong").ownText();
                    switch (type) {
                        case "작가":
                            author = e.selectFirst("a").ownText();
                            break;
                        case "분류":
                            for (Element t : e.select("a"))
                                tags.add(t.ownText());
                            break;
                        case "발행구분":
                            release = e.selectFirst("a").ownText();
                            break;
                    }

                }catch (Exception e2){continue;}
            }

            // 에피소드 목록 파싱
            String title, date;
            Manga tmp;
            int id;
            eps = new ArrayList<>();
            try{
                for(Element e : d.selectFirst("ul.list-body").select("li.list-item")) {
                    Element titlee = e.selectFirst("a.item-subject");
                    id = getNumberFromString(titlee.attr("href").split(baseModeStr(baseMode)+'/')[1]);

                    title = titlee.ownText();

                    Elements infoe = e.selectFirst("div.item-details").select("span");
                    date = infoe.get(0).ownText();
                    // 조회수, 추천수 등 추가 정보가 있지만 여기서는 파싱하지 않음
                    tmp = new Manga(id, title, date, baseMode);
                    tmp.setMode(0);
                    eps.add(tmp);
                }
            }catch (Exception e){e.printStackTrace();}
            r.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
        return LOAD_OK;
    }

    // 서버에 북마크(선호작) 상태를 토글합니다.
    public boolean toggleBookmark(CustomHttpClient client, Preference p){
        RequestBody requestBody = new FormBody.Builder()
                .addEncoded("mode", bookmarked?"off":"on") // 현재 상태에 따라 on/off 결정
                .addEncoded("top","0")
                .addEncoded("js","on")
                .build();

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", p.getLogin().getCookie(true)); // 로그인 쿠키 추가
        Response r = client.post(bookmarkLink, requestBody, headers);
        try {
            JSONObject obj = new JSONObject(r.body().string());
            if(obj.getString("error").isEmpty() && !obj.getString("success").isEmpty()){
                // 성공
                bookmarked = !bookmarked; // 로컬 상태 변경
            }else{
                // 실패
                r.close();
                return false;
            }
        }catch (Exception e){
            if(r!=null) r.close();
            e.printStackTrace();
            return false;
        }
        if(r!=null) r.close();
        return true;
    }


    // 로컬 북마크(마지막으로 본 에피소드 위치)를 반환합니다.
    public int getBookmark(){
        return bookmark;
    }
    // 총 에피소드 개수를 반환합니다.
    public int getEpsCount(){ return eps.size();}

    // 최신 에피소드에 'NEW' 태그가 있는지 확인합니다.
    public Boolean isNew() throws Exception{
        if(eps!=null){
            return eps.get(0).getName().split(" ")[0].contains("NEW");
        }else{
            throw new Exception("not loaded");
        }
    }

    // 에피소드 목록을 설정합니다.
    public void setEps(List<Manga> list){
        eps = list;
    }

    // 에피소드 목록을 비웁니다.
    public void removeEps(){
        if(eps!=null) eps.clear();
    }

    // 로컬 북마크(마지막으로 본 에피소드 위치)를 설정합니다.
    public void setBookmark(int b){bookmark = b;}


    @Override
    public Title clone(){
        return new Title(name, thumb, author, tags, release, id, baseMode);
    }

    // 추천 수를 반환합니다.
    public int getRecommend_c() {
        return rc;
    }

    // 추천 수를 설정합니다.
    public void setRecommend_c(int recommend_c) {
        this.rc = recommend_c;
    }

    // Title 객체를 간소화된 MTitle 객체로 변환합니다.
    public MTitle minimize(){
        return new MTitle(name, id, thumb, author, tags, release, baseMode);
    }

    // 추천 수나 북마크 정보가 있는지 확인합니다.
    public boolean hasCounter(){
        return !(rc==0&&(bookmarkLink==null||bookmarkLink.length()==0));
    }

    // 문자열이 정수인지 확인합니다.
    public static boolean isInteger(String s) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),10) < 0) return false;
        }
        return true;
    }

    // 발행구분(release) 필드가 숫자가 아닐 경우(단행본 등) 북마크 기능을 사용할지 결정합니다.
    public boolean useBookmark(){
        return !isInteger(release);
    }

}


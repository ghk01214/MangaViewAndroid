package ml.melun.mangaview.mangaview;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import okhttp3.Response;

import static ml.melun.mangaview.mangaview.MTitle.baseModeStr;

// 만화 검색을 수행하는 클래스
public class Search {
    /* mode
    * 0 : 제목
    * 1 : 작가
    * 2 : 태그
    * 3 : 글자 (초성)
    * 4 : 발행
    * 5 : null (사용 안함)
    * 6 : 종합 (사용 안함)
    * 7-13: 웹툰 관련 (현재 코드에서는 사용되지 않음)
     */

    int baseMode; // 만화(0) 또는 웹툰(1) 등의 기본 소스

    // 생성자. 검색어, 검색 모드, 기본 소스를 받습니다.
    public Search(String q, int mode, int baseMode) {
        query = q;
        this.mode = mode;
        this.baseMode = baseMode;
    }


    public int getBaseMode() {
        return baseMode;
    }

    public Boolean isLast() {
        return last;
    }

    // 웹에서 검색 결과를 가져와 파싱합니다.
    public int fetch(CustomHttpClient client) {
        result = new ArrayList<>();
        if(!last) { // 마지막 페이지가 아닐 경우에만 실행
            try {
                // 검색 모드에 따라 URL 경로를 설정합니다.
                String searchUrl = "";
                switch(mode){
                    case 0: // 제목
                        searchUrl = "?bo_table="+baseModeStr(baseMode)+"&stx=";
                        break;
                    case 1: // 작가
                        searchUrl = "?bo_table="+baseModeStr(baseMode)+"&artist=";
                        break;
                    case 2: // 태그
                        searchUrl = "?bo_table="+baseModeStr(baseMode)+"&tag=";
                        break;
                    case 3: // 초성
                        searchUrl = "?bo_table="+baseModeStr(baseMode)+"&jaum=";
                        break;
                    case 4: // 발행
                        searchUrl = "?bo_table="+baseModeStr(baseMode)+"&publish=";
                        break;
                }


                // URL 인코딩된 검색어로 GET 요청을 보냅니다.
                Response response = client.mget('/' + baseModeStr(baseMode) + "/p" + page++ + searchUrl + URLEncoder.encode(query,"UTF-8"), true, null);
                String body = response.body().string();
                if(body.contains("Connect Error: Connection timed out")){
                    // 타임아웃 발생 시 재시도
                    response.close();
                    page--;
                    return fetch(client);
                }
                Document d = Jsoup.parse(body);
                d.outputSettings().charset(StandardCharsets.UTF_8);

                // 검색 결과 아이템들을 선택합니다.
                Elements titles = d.select("div.list-item");

                if(response.code()>=400){
                    // HTTP 오류 발생 시
                    return 1;
                } else if (titles.size() < 1)
                    // 결과가 없으면 마지막 페이지로 간주
                    last = true;

                String title;
                String thumb;
                String author;
                String release;
                int id;

                // 각 아이템을 순회하며 정보를 추출합니다.
                for(Element e : titles) {
                    try {
                        Element infos = e.selectFirst("div.img-item");
                        Element infos2 = infos.selectFirst("div.in-lable");

                        id = Integer.parseInt(infos2.attr("rel")); // ID
                        title = infos2.selectFirst("span").ownText(); // 제목
                        thumb = infos.selectFirst("img").attr("src"); // 썸네일 URL

                        Element ae = e.selectFirst("div.list-artist"); // 작가 정보
                        if (ae != null) author = ae.selectFirst("a").ownText();
                        else author = "";

                        Element re = e.selectFirst("div.list-publish"); // 발행 정보
                        if (re != null) release = re.selectFirst("a").ownText();
                        else release = "";

                        // 추출한 정보로 Title 객체를 생성하여 결과 리스트에 추가
                        result.add(new Title(title, thumb, author, null, release, id, baseMode));
                    }catch (Exception e2){
                        e2.printStackTrace();
                    }
                }
                response.close();
                // 한 페이지의 아이템 수가 35개 미만이면 마지막 페이지로 간주
                if (result.size() < 35)
                    last = true;

                // 결과가 없으면 페이지 번호를 되돌림
                if(result.size()==0)
                    page--;

            } catch (Exception e) {
                page--; // 예외 발생 시 페이지 번호 복구
                e.printStackTrace();
                return 1; // 오류 코드 반환
            }
        }
        return 0; // 성공 코드 반환
    }


    // 검색 결과를 반환합니다.
    public ArrayList<Title> getResult(){
        return result;
    }

    private final String query; // 검색어
    Boolean last = false; // 마지막 페이지인지 여부
    int mode; // 검색 모드
    int page = 1; // 현재 페이지 번호
    private ArrayList<Title> result; // 검색 결과 리스트
}

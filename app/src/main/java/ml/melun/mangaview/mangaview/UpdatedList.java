package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Response;


// 업데이트된 만화 목록을 가져오는 클래스
public class UpdatedList {
    Boolean last = false; // 마지막 페이지인지 여부
    ArrayList<UpdatedManga> result; // 파싱 결과를 담을 리스트
    int page = 1; // 현재 페이지 번호
    int baseMode; // 현재 만화 소스 (사용되지 않는 것으로 보임)

    public UpdatedList(int baseMode){
        this.baseMode = baseMode;
    }

    // 현재 페이지 번호를 반환합니다.
    public int getPage(){
        return this.page;
    }

    // 웹에서 업데이트된 만화 목록을 가져와 파싱합니다.
    public void fetch(CustomHttpClient client){
        // 페이지당 50개 아이템
        result = new ArrayList<>();
        String url = "/bbs/page.php?hid=update&page=";
        if(!last) { // 마지막 페이지가 아닐 경우에만 실행
            try {
                // 페이지 내용을 가져옴
                Response response= client.mget(url + page++,true,null);
                String body = response.body().string();
                if(body.contains("Connect Error: Connection timed out")){
                    // 타임아웃 발생 시 재시도
                    response.close();
                    fetch(client);
                    return;
                }
                Document document = Jsoup.parse(body);
                Elements items = document.select("div.post-row"); // 각 만화 아이템
                if (items == null || items.size() < 70) last = true; // 아이템 수가 70개 미만이면 마지막 페이지로 간주
                for(Element item : items){
                    try {
                        String img = item.selectFirst("img").attr("src"); // 썸네일 이미지 URL
                        String name = item.selectFirst("div.post-subject").selectFirst("a").ownText(); // 만화 제목
                        int id = Integer.parseInt(item
                                .selectFirst("div.pull-left")
                                .selectFirst("a")
                                .attr("href")
                                .split("comic/")[1]); // 만화 ID

                        Elements rightInfo = item.selectFirst("div.pull-right").select("p");

                        int tid = Integer.parseInt(rightInfo
                                .get(0)
                                .selectFirst("a")
                                .attr("href")
                                .split("comic/")[1]); // 타이틀 ID

                        String date = rightInfo.get(1).selectFirst("span").ownText(); // 업데이트 날짜


                        String at = item.selectFirst("div.post-text").ownText(); // 작가 및 태그 정보
                        //작가 작가 태그1,태그2,태그3 형식
                        String author = at.substring(0,at.lastIndexOf(' ')); // 작가

                        List<String> tags = Arrays.asList(at.substring(at.lastIndexOf(' ')).split(",")); // 태그 목록

                        // UpdatedManga 객체 생성 및 정보 설정
                        UpdatedManga tmp = new UpdatedManga(id, name, date, baseMode,author,tags);
                        tmp.setMode(0);
                        tmp.setTitle(new Title(name, img, author, tags, "", tid, MTitle.base_comic));
                        tmp.addThumb(img);
                        result.add(tmp);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
                response.close();
            } catch (Exception e) {
                e.printStackTrace();
                page--; // 오류 발생 시 페이지 번호 복구
            }
        }
    }

    // 파싱 결과를 반환합니다.
    public ArrayList<UpdatedManga> getResult() {
        return result;
    }
    // 마지막 페이지인지 여부를 반환합니다.
    public boolean isLast(){return last;}


}

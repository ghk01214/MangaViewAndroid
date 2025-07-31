package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

import static ml.melun.mangaview.mangaview.MTitle.base_comic;
import static ml.melun.mangaview.mangaview.MTitle.base_webtoon;

// 메인 페이지의 웹툰 관련 데이터를 파싱하는 클래스
public class MainPageWebtoon {
    String baseUrl; // 웹툰 메인 페이지의 기본 URL

    // 메인 페이지의 각 섹션 제목 상수
    public static final String normalNew="일반연재 최신", adultNew="성인웹툰 최신", gayNew="BL/GL 최신", comicNew="일본만화 최신",
            normalBest="일반연재 베스트", adultBest="성인웹툰 베스트", gayBest="BL/GL 베스트", comicBest="일본만화 베스트";
    // 각 섹션에 해당하는 div.main-box의 인덱스
    static final int nn=4,an=5,gn=6,cn=7,nb=8,ab=9,gb=10,cb=11;

    List<Ranking<?>> dataSet; // 파싱된 데이터를 담을 리스트 (각 섹션이 하나의 Ranking 객체)

    // 생성자. 객체 생성 시 바로 데이터를 가져옵니다.
    public MainPageWebtoon(CustomHttpClient client){
        fetch(client);
    }

    // 리다이렉션을 통해 실제 웹툰 메인 페이지의 URL을 가져옵니다.
    public String getUrl(CustomHttpClient client){
        Response r = client.mget("/site.php?id=1");
        if(r==null) return null;
        if(r.code() == 302){ // 302 리다이렉트 발생 시
            this.baseUrl = r.header("Location"); // Location 헤더에서 URL을 가져옴
        }else
            return null;
        r.close();
        return this.baseUrl;
    }

    // 웹툰 메인 페이지에서 데이터를 가져와 파싱합니다.
    public void fetch(CustomHttpClient client){
        // baseUrl이 없으면 getUrl을 통해 가져옴
        if(baseUrl == null || baseUrl.length()==0)
            if(getUrl(client)==null)
                return;
        try {
            Response r = client.get(baseUrl, null);
            String body = r.body().string();
            if(body.contains("Connect Error: Connection timed out")){
                // 타임아웃 발생 시 재시도
                r.close();
                fetch(client);
                return;
            }

            Document d = Jsoup.parse(body);
            Elements boxes = d.select("div.main-box"); // 각 섹션 컨테이너

            dataSet = new ArrayList<>();

            // 각 섹션별로 파싱 실행
            parseTitle(normalNew, boxes.get(nn).select("a"), base_webtoon);
            parseTitle(adultNew, boxes.get(an).select("a"), base_webtoon);
            parseTitle(gayNew, boxes.get(gn).select("a"), base_webtoon);
            parseTitle(comicNew, boxes.get(cn).select("a"), base_comic);
            parseTitle(normalBest, boxes.get(nb).select("a"), base_webtoon);
            parseTitle(adultBest, boxes.get(ab).select("a"), base_webtoon);
            parseTitle(gayBest, boxes.get(gb).select("a"), base_webtoon);
            parseTitle(comicBest, boxes.get(cb).select("a"), base_comic);

        }catch (Exception e){
            e.printStackTrace();
        }


    }

    // 파싱된 데이터셋을 반환합니다.
    public List<Ranking<?>> getDataSet(){
        return this.dataSet;
    }


    // HTML Elements에서 개별 작품 정보를 파싱하여 Ranking 객체에 추가하는 헬퍼 메서드
    public void parseTitle(String title, Elements es, int baseMode){
        Ranking<Title> ranking = new Ranking<>(title);
        Title tmp;
        String idString,idString1,name;
        int id;
        for(Element e : es){
            Element img = e.selectFirst("div.in-subject");
            if(img!=null){
                name = img.ownText(); // 제목
            }else{
                name = e.ownText();
            }
            idString = e.attr("href"); // href 속성에서 ID 추출
            idString1 = idString.substring(idString.lastIndexOf('/')+1);
            id = Integer.parseInt(idString1.substring(idString1.lastIndexOf('=')+1));
            tmp = new Title(name, "", "", null, "", id, baseMode);
            ranking.add(tmp);
        }
        dataSet.add(ranking);
    }

    // 데이터 로딩 전 보여줄 빈 데이터셋을 생성합니다.
    public static List<Ranking<?>> getBlankDataSet(){
        List<Ranking<?>> dataset = new ArrayList<>();
        dataset.add(new Ranking<>(normalNew));
        dataset.add(new Ranking<>(adultNew));
        dataset.add(new Ranking<>(gayNew));
        dataset.add(new Ranking<>(comicNew));
        dataset.add(new Ranking<>(normalBest));
        dataset.add(new Ranking<>(adultBest));
        dataset.add(new Ranking<>(adultBest));
        dataset.add(new Ranking<>(gayBest));
        return dataset;
    }



}

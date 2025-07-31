package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import okhttp3.Response;

import static ml.melun.mangaview.mangaview.MTitle.base_comic;


// 메인 페이지(만화)의 데이터를 파싱하는 클래스
public class MainPage {
    List<Manga> recent; // 최근 업데이트된 만화 목록
    List<Manga> favUpdate; // 선호작 업데이트 (현재 사용 안함)
    List<Manga> onlineRecent; // 실시간 인기 (현재 사용 안함)
    List<RankingTitle> ranking; // 종합 랭킹 (제목 기준)

    // 주간 랭킹을 반환합니다.
    public List<RankingManga> getWeeklyRanking() {
        return weeklyRanking;
    }

    List<RankingManga> weeklyRanking; // 주간 랭킹 (에피소드 기준)

    // 웹에서 메인 페이지 데이터를 가져와 파싱합니다.
    void fetch(CustomHttpClient client) {

        recent = new ArrayList<>();
        ranking = new ArrayList<>();
        weeklyRanking = new ArrayList<>();

        favUpdate = new ArrayList<>();
        onlineRecent = new ArrayList<>();

        try{
            Response r = client.mget("",true,null);
            String body = r.body().string();
            if(body.contains("Connect Error: Connection timed out")){
                // 타임아웃 발생 시 재시도
                r.close();
                fetch(client);
                return;
            }
            Document d = Jsoup.parse(body);
            r.close();

            // 변수 선언
            int id;
            String name;
            String thumb;
            Manga mtmp;
            Element infos;
            Title ttmp;

            // 최근 업데이트된 만화 파싱
            for(Element e : d.selectFirst("div.miso-post-gallery").select("div.post-row")){
                id = Integer.parseInt(e.selectFirst("a").attr("href").split("comic/")[1]);
                infos = e.selectFirst("div.img-item");
                thumb = infos.selectFirst("img").attr("src");
                name = infos.selectFirst("b").ownText();

                mtmp = new Manga(id, name, "", base_comic);
                mtmp.addThumb(thumb);
                recent.add(mtmp);
            }

            // 종합 랭킹(제목) 파싱
            int i=1;
            for(Element e : d.select("div.miso-post-gallery").last().select("div.post-row")){
                id = Integer.parseInt(e.selectFirst("a").attr("href").split("comic/")[1]);
                infos = e.selectFirst("div.img-item");
                thumb = infos.selectFirst("img").attr("src");
                name = infos.selectFirst("div.in-subject").ownText();

                ranking.add(new RankingTitle(name, thumb, "", null, "", id, base_comic, i++));
            }

            // 주간 랭킹(에피소드) 파싱
            i=1;
            for(Element e : d.select("div.miso-post-list").last().select("li.post-row")){
                infos = e.selectFirst("a");
                id = Integer.parseInt(infos.attr("href").split("comic/")[1]);
                name = infos.ownText();

                weeklyRanking.add(new RankingManga(id, name, "", base_comic, i++));
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        // 이전 버전의 파싱 코드 (현재 주석 처리됨)
/*
        try{
            Response response = client.mget("");
            Document doc = Jsoup.parse(response.body().string());

            Elements list = doc.selectFirst("div.msm-post-gallery").select("div.post-row");
            for(Element e:list){
                String[] tmp_idStr = e.selectFirst("a").attr("href").toString().split("=");
                int tmp_id = Integer.parseInt(tmp_idStr[tmp_idStr.length-1]);
                String tmp_thumb = e.selectFirst("img").attr("src").toString();
                String tmp_title = e.selectFirst("img").attr("alt").toString();
                Manga tmp = new Manga(tmp_id,tmp_title,"");
                tmp.addThumb(tmp_thumb);
                recent.add(tmp);
            }
            Elements rankingWidgets = doc.select("div.rank-manga-widget");

            // online data
            Elements fav= rankingWidgets.get(0).select("li");
            rankingWidgetLiParser(fav, favUpdate);

            Elements rec = rankingWidgets.get(1).select("li");
            rankingWidgetLiParser(rec, onlineRecent);

            // ranking
            Elements rank = rankingWidgets.get(2).select("li");
            rankingWidgetLiParser(rank, ranking);

            //close response
            response.close();


        }catch (Exception e){
            e.printStackTrace();
        }
*/
    }

    // 랭킹 정보를 담기 위해 Title 클래스를 상속받은 내부 클래스
    public static class RankingTitle extends Title{
        int ranking; // 순위
        public RankingTitle(String n, String t, String a, List<String> tg, String r, int id, int baseMode, int ranking) {
            super(n, t, a, tg, r, id, baseMode);
            this.ranking = ranking;
        }

        public int getRanking() {
            return ranking;
        }
    }
    // 랭킹 정보를 담기 위해 Manga 클래스를 상속받은 내부 클래스
    public static class RankingManga extends Manga{
        int ranking; // 순위
        public RankingManga(int i, String n, String d, int baseMode, int ranking) {
            super(i, n, d, baseMode);
            this.ranking = ranking;
        }

        public int getRanking() {
            return ranking;
        }
    }

    // 이전 버전에서 사용되던 랭킹 위젯 파서 (현재 사용 안함)
    void rankingWidgetLiParser(Elements input, List output){
        for(Element e: input){
            String[] tmp_link = e.selectFirst("a").attr("href").split("=");
            int tmp_id = Integer.parseInt(tmp_link[tmp_link.length-1]);
            String tmp_title = e.selectFirst("div.subject").ownText();
            output.add(new Manga(tmp_id, tmp_title,"", base_comic));
        }
    }
    // 생성자. 객체 생성 시 바로 데이터를 가져옵니다.
    public MainPage(CustomHttpClient client) {
        fetch(client);
    }

    public List<Manga> getRecent() {
        return recent;
    }

    public List<Manga> getFavUpdate() {
        return favUpdate;
    }

    public List<Manga> getOnlineRecent() {
        return onlineRecent;
    }

    public List<RankingTitle> getRanking() { return ranking; }
}

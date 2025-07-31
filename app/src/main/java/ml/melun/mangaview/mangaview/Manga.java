package ml.melun.mangaview.mangaview;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.*;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.Response;

import static ml.melun.mangaview.Utils.CODE_SCOPED_STORAGE;
import static ml.melun.mangaview.mangaview.MTitle.baseModeStr;
import static ml.melun.mangaview.mangaview.MTitle.base_comic;
import static ml.melun.mangaview.mangaview.Title.LOAD_CAPTCHA;
import static ml.melun.mangaview.mangaview.Title.LOAD_OK;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.documentfile.provider.DocumentFile;

// 만화의 한 회차(에피소드)를 나타내는 클래스
public class Manga {
    /*
    mode:
    0 = 온라인
    1 = 오프라인 - 구버전
    2 = 오프라인 - 구버전(모아) (title.data)
    3 = 오프라인 - 최신(토끼) (title.gson)
    4 = 오프라인 - 신버전(모아) (title.gson)
     */

    int baseMode = base_comic; // 만화 소스 (코믹, 웹툰 등)

    // 생성자
    public Manga(int i, String n, String d, int baseMode) {
        id = i;
        name = n;
        date = d;
        this.baseMode = baseMode;
    }

    public int getBaseMode() {
        return this.baseMode;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addThumb(String src) {
        thumb = src;
    }

    public String getDate() {
        return date;
    }

    // 이미지 목록을 직접 설정합니다. (주로 오프라인 모드에서 사용)
    public void setImgs(List<String> imgs) {
        this.imgs = imgs;
    }

    public String getThumb() {
        if (thumb == null) return "";
        return thumb;
    }

    // 웹에서 만화 데이터를 가져옵니다. (기본)
    public int fetch(CustomHttpClient client) {
        return fetch(client, true, null);
    }

    // 웹에서 만화 데이터를 가져옵니다. (쿠키 사용)
    public int fetch(CustomHttpClient client, Map<String, String> cookies) {
        return fetch(client, false, cookies);
    }

    // 웹에서 만화 데이터를 가져오는 핵심 메서드
    public int fetch(CustomHttpClient client, boolean doLogin, Map<String, String> cookies) {
        mode = 0; // 온라인 모드로 설정
        imgs = new ArrayList<>();
        eps = new ArrayList<>();
        comments = new ArrayList<>();
        bcomments = new ArrayList<>();
        int tries = 0;

        // 이미지 목록을 가져올 때까지 최대 2번 재시도
        while (imgs.size() == 0 && tries < 2) {
            Response r = client.mget(  baseModeStr(baseMode) + '/' + id, false, cookies);
            try {
                // 캡차 페이지로 리다이렉트되면 캡차 필요 코드를 반환
                if (r.code() == 302 && r.header("location").contains("captcha.php")) {
                    return LOAD_CAPTCHA;
                }
                String body = r.body().string();
                r.close();
                // 타임아웃 발생 시 재시도
                if (body.contains("Connect Error: Connection timed out")) {
                    r.close();
                    tries = 0;
                    continue;
                }

                Document d = Jsoup.parse(body);

                // 에피소드 제목 파싱
                name = d.selectFirst("div.toon-title").ownText();

                // 상위 작품(Title) 정보 파싱
                Element navbar = d.selectFirst("div.toon-nav");
                int tid = Integer.parseInt(navbar.select("a")
                        .last()
                        .attr("href")
                        .split(baseModeStr(baseMode) + '/')[1]
                        .split("\\?")[0]);

                if (title == null) title = new Title(name, "", "", null, "", tid, baseMode);

                // 다른 에피소드 목록 파싱
                for (Element e : navbar.selectFirst("select").select("option")) {
                    String idstr = e.attr("value");
                    if (idstr.length() > 0)
                        eps.add(new Manga(Integer.parseInt(idstr), e.ownText(), "", baseMode));
                }

                // 이미지 URL 목록 파싱 (스크립트 내 인코딩된 데이터 디코딩)
                String script = d.select("div.view-padding").get(1).selectFirst("script").data();
                StringBuilder encodedData = new StringBuilder();
                encodedData.append('%');
                for (String line : script.split("\n")) {
                    if (line.contains("html_data+=")) {
                        encodedData.append(line.substring(line.indexOf('\'') + 1, line.lastIndexOf('\'')).replaceAll("[.]", "%"));
                    }
                }
                if (encodedData.lastIndexOf("%") == encodedData.length() - 1)
                    encodedData.deleteCharAt(encodedData.length() - 1);
                String imgdiv = URLDecoder.decode(encodedData.toString(), "UTF-8");

                Document id = Jsoup.parse(imgdiv);
                for (Element e : id.select("img")) {
                    String style = e.attr("style");
                    if (style.length() == 0) {
                        boolean flag = false;
                        for (Attribute a : e.attributes()) {
                            if (a.getKey().contains("data")) {
                                String img = a.getValue();
                                if (!img.isEmpty() && !img.contains("blank") && !img.contains("loading")) {
                                    flag = true;
                                    if (img.startsWith("/"))
                                        imgs.add(client.getUrl() + img);
                                    else
                                        imgs.add(img);
                                }
                            }
                        }
                        if (!flag) {
                            String img = e.attr("src");
                            if (!img.isEmpty() && !img.contains("blank") && !img.contains("loading")) {
                                if (img.startsWith("/"))
                                    imgs.add(client.getUrl() + img);
                                else
                                    imgs.add(img);
                            }
                        }
                    }
                }

                // 댓글 파싱
                Element commentdiv = d.selectFirst("div#viewcomment");


                try {
                    if (commentdiv != null) {
                        // 일반 댓글 섹션들을 여러 방법으로 시도
                        Element regularCommentsSection = commentdiv.selectFirst("section#bo_vc");
                        if (regularCommentsSection != null) {
                            for (Element e : regularCommentsSection.select("div.media")) {
                                try {
                                    comments.add(parseComment(e));
                                } catch (Exception e3) {
                                    e3.printStackTrace();
                                }
                            }
                        } else {
                            // 일반 댓글 섹션이 없으면 전체 댓글 영역에서 찾기
                            for (Element e : commentdiv.select("div.media")) {
                                try {
                                    Comment comment = parseComment(e);
                                    comments.add(comment);
                                } catch (Exception e3) {
                                    e3.printStackTrace();
                                }
                            }
                        }
                        
                        // 베스트 댓글
                        Element bestCommentsSection = commentdiv.selectFirst("section#bo_vcb");
                        if (bestCommentsSection != null) {
                            for (Element e : bestCommentsSection.select("div.media")) {
                                try {
                                    bcomments.add(parseComment(e));
                                } catch (Exception e3) {
                                    e3.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            } catch (Exception e2) {
                e2.printStackTrace();
            }
            if (r != null) {
                r.close();
            }
            tries++;
        }
        return LOAD_OK;
    }

    // HTML Element에서 댓글 정보를 파싱하는 헬퍼 메서드
    private Comment parseComment(Element e) {
        String user; // 작성자
        String icon; // 작성자 아이콘 URL
        String content; // 내용
        String timestamp; // 작성 시간
        int likes; // 좋아요 수
        int level; // 작성자 레벨
        String lvlstr;
        int indent; // 들여쓰기 수준 (대댓글)
        String indentstr;

        // 들여쓰기 파싱
        indentstr = e.attr("style");
        if (indentstr.length() > 0)
            indent = Integer.parseInt(indentstr.substring(indentstr.lastIndexOf(':') + 1, indentstr.lastIndexOf('p'))) / 64;
        else
            indent = 0;

        // 아이콘 파싱
        Element icone = e.selectFirst(".media-object");
        if (icone.is("img"))
            icon = icone.attr("src");
        else
            icon = "";

        Element header = e.selectFirst("div.media-heading");
        Element userSpan = header.selectFirst("span.member");
        user = userSpan.ownText();
        // 레벨 파싱
        if (userSpan.hasClass("guest"))
            level = 0;
        else {
            lvlstr = userSpan.selectFirst("img").attr("src");
            level = Integer.parseInt(lvlstr.substring(lvlstr.lastIndexOf('/') + 1, lvlstr.lastIndexOf('.')));
        }
        timestamp = header.selectFirst("span.media-info").ownText();

        Element cbody = e.selectFirst("div.media-content");
        content = cbody.selectFirst("div:not([class])").ownText();

        // 좋아요 수 파싱
        Elements cspans = cbody.selectFirst("div.cmt-good-btn").select("span");
        likes = Integer.parseInt(cspans.get(cspans.size() - 1).ownText());
        return new Comment(user, timestamp, icon, content, indent, likes, level);
    }


    // 다른 에피소드 목록을 반환합니다.
    public List<Manga> getEps() {
        return eps;
    }

    // 상위 작품(Title) 객체를 반환합니다.
    public Title getTitle() {
        return title;
    }

    // 이미지 URL 목록을 반환합니다. 오프라인 모드일 경우 로컬 파일 경로를 읽어 반환합니다.
    public List<String> getImgs(Context context) {
        if (mode != 0) { // 오프라인 모드일 경우
            if (imgs == null) {
                imgs = new ArrayList<>();
                // Scoped Storage (Android 10 이상) 대응
                if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE) {
                    DocumentFile[] offimgs = DocumentFile.fromTreeUri(context, Uri.parse(offlinePath)).listFiles();
                    Arrays.sort(offimgs, (documentFile, t1) -> documentFile.getName().compareTo(t1.getName()));
                    for (DocumentFile f : offimgs) {
                        imgs.add(f.getUri().toString());
                    }
                } else { // 구버전 안드로이드
                    File[] offimgs = new File(offlinePath).listFiles();
                    Arrays.sort(offimgs);
                    for (File img : offimgs) {
                        imgs.add(img.getAbsolutePath());
                    }
                }
            }
        }
        return imgs;
    }

    // 일반 댓글 목록을 반환합니다.
    public List<Comment> getComments() {
        return comments;
    }

    // 베스트 댓글 목록을 반환합니다.
    public List<Comment> getBestComments() {
        return bcomments;
    }

    public int getSeed() {
        return seed;
    }

    // Manga 객체를 JSON 문자열로 변환합니다.
    public String toString() {
        JSONObject tmp = new JSONObject();
        try {
            tmp.put("id", id);
            tmp.put("name", name);
            tmp.put("date", date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tmp.toString();
    }

    public void setTitle(Title title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object obj) {
        return this.id == ((Manga) obj).getId();
    }

    @Override
    public int hashCode() {
        return id;
    }

    public void setOfflinePath(String offlinePath) {
        this.offlinePath = offlinePath;
    }

    public String getOfflinePath() {
        return this.offlinePath;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    // 현재 에피소드의 URL을 반환합니다.
    public String getUrl() {
        return '/' + baseModeStr(baseMode) + '/' + id;
    }

    // 북마크 기능을 사용할 수 있는 에피소드인지 확인합니다.
    public boolean useBookmark() {
        return id > 0 && (mode == 0 || mode == 3);
    }

    // 온라인 에피소드인지 확인합니다.
    public boolean isOnline() {
        return id > 0 && mode == 0;
    }

    // 다음 에피소드를 반환합니다.
    public Manga nextEp() {
        if (isOnline()) {
            if (eps == null || eps.size() == 0) {
                return null;
            }
            else {
                int index = eps.indexOf(this);
                if (index > 0) return eps.get(index - 1);
                else return null;
            }
        }
        else {
            return nextEp;
        }
    }

    // 이전 에피소드를 반환합니다.
    public Manga prevEp() {
        if (isOnline()) {
            if (eps == null || eps.size() == 0) {
                return null;
            }
            else {
                int index = eps.indexOf(this);
                if (index < eps.size() - 1) return eps.get(index + 1);
                else return null;
            }
        }
        else {
            return prevEp;
        }
    }

    public void setPrevEp(Manga m) {
        this.prevEp = m;
    }

    public void setNextEp(Manga m) {
        this.nextEp = m;
    }

    private final int id; // 에피소드 ID
    String name; // 에피소드 제목
    List<Manga> eps; // 동일 작품의 다른 에피소드 목록
    List<String> imgs; // 이미지 URL 목록
    List<Comment> comments, bcomments; // 일반 댓글, 베스트 댓글 목록
    String offlinePath; // 오프라인 저장 경로
    String thumb; // 썸네일 URL
    Title title; // 상위 작품(Title) 객체
    String date; // 발행일
    int seed; // (사용되지 않는 것으로 보임)
    int mode; // 온라인/오프라인 모드
    transient Listener listener; // 리스너
    Manga nextEp, prevEp; // 이전/다음 에피소드

    public interface Listener {
        void setMessage(String msg);
    }
}


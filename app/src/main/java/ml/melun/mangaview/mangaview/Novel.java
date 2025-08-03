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
import static ml.melun.mangaview.mangaview.MTitle.base_novel;
import static ml.melun.mangaview.mangaview.Title.LOAD_CAPTCHA;
import static ml.melun.mangaview.mangaview.Title.LOAD_OK;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import ml.melun.mangaview.mangaview.Comment;

// 소설의 한 회차(에피소드)를 나타내는 클래스
public class Novel {
    /*
    mode:
    0 = 온라인
    1 = 오프라인 - 구버전
    2 = 오프라인 - 구버전(모아) (title.data)
    3 = 오프라인 - 최신(토끼) (title.gson)
    4 = 오프라인 - 신버전(모아) (title.gson)
     */

    int baseMode = base_novel; // 소설 소스

    private int id;
    private String name;
    private String date;
    private String thumb = "";
    private String content = ""; // 소설 텍스트 내용
    private Title title; // 상위 작품 정보
    private int mode = 0; // 로드 모드
    private List<Novel> eps = new ArrayList<>(); // 에피소드 목록
    private List<Comment> comments = new ArrayList<>(); // 댓글 목록
    private List<Comment> bcomments = new ArrayList<>(); // 베스트 댓글 목록
    private String offlinePath; // 오프라인 저장 경로
    private int seed = 0; // 시드 값
    private transient Listener listener; // 리스너
    private Novel nextEp, prevEp; // 이전/다음 에피소드

    // 생성자
    public Novel(int i, String n, String d, int baseMode) {
        id = i;
        name = n;
        date = d;
        this.baseMode = baseMode;
    }

    // Manga 호환성을 위한 생성자
    public Novel(Manga manga) {
        this.id = manga.getId();
        this.name = manga.getName();
        this.date = manga.getDate();
        this.baseMode = base_novel;
        this.title = manga.getTitle();
        this.mode = manga.getMode();
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

    public String getThumb() {
        if (thumb == null) return "";
        return thumb;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Title getTitle() {
        return title;
    }

    public void setTitle(Title title) {
        this.title = title;
    }

    public int getMode() {
        return mode;
    }

    public List<Novel> getEps() {
        return eps;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public List<Comment> getBestComments() {
        return bcomments;
    }

    // 웹에서 소설 데이터를 가져옵니다. (기본)
    public int fetch(CustomHttpClient client) {
        return fetch(client, true, null);
    }

    // 웹에서 소설 데이터를 가져옵니다. (쿠키 사용)
    public int fetch(CustomHttpClient client, Map<String, String> cookies) {
        return fetch(client, false, cookies);
    }

    // 웹에서 소설 데이터를 가져오는 핵심 메서드
    public int fetch(CustomHttpClient client, boolean doLogin, Map<String, String> cookies) {
        mode = 0; // 온라인 모드로 설정
        eps = new ArrayList<>();
        comments = new ArrayList<>();
        bcomments = new ArrayList<>();
        int tries = 0;

        // 소설 내용을 가져올 때까지 최대 2번 재시도
        while (content.isEmpty() && tries < 2) {
            Response r = client.mget(baseModeStr(baseMode) + '/' + id, false, cookies);
            try {
                // 캡차 페이지로 리다이렉트되면 캡차 필요 코드를 반환
                if (r.code() == 302 && r.header("location") != null && r.header("location").contains("captcha.php")) {
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
                Element titleElement = d.selectFirst("div.toon-title");
                if (titleElement != null) {
                    name = titleElement.ownText();
                }

                // 상위 작품(Title) 정보 파싱
                Element navbar = d.selectFirst("div.toon-nav");
                if (navbar != null) {
                    Elements navLinks = navbar.select("a");
                    if (!navLinks.isEmpty()) {
                        String href = navLinks.last().attr("href");
                        if (href.contains(baseModeStr(baseMode) + '/')) {
                            int tid = Integer.parseInt(href.split(baseModeStr(baseMode) + '/')[1].split("\\?")[0]);
                            if (title == null) title = new Title(name, "", "", null, "", tid, baseMode);
                        }
                    }

                    // 다른 에피소드 목록 파싱
                    Element selectElement = navbar.selectFirst("select");
                    if (selectElement != null) {
                        for (Element e : selectElement.select("option")) {
                            String idstr = e.attr("value");
                            if (idstr.length() > 0) {
                                Novel novel = new Novel(Integer.parseInt(idstr), e.ownText(), "", baseMode);
                                novel.setTitle(title); // 상위 작품 정보 설정
                                eps.add(novel);
                            }
                        }
                    }
                }

                // 소설 내용 파싱
                content = parseNovelContent(d);



                // 댓글 파싱
                parseComments(d);

            } catch (Exception e) {
                e.printStackTrace();
                content = "소설 내용을 불러오는 중 오류가 발생했습니다: " + e.getMessage();
                return 1;
            }
            tries++;
        }

        return content.isEmpty() ? 1 : LOAD_OK;
    }

    // 소설 내용 파싱
    private String parseNovelContent(Document doc) {
        StringBuilder contentBuilder = new StringBuilder();
        boolean contentFound = false;

        try {
            // 1. 일반적인 소설 컨텐츠 선택자들을 순서대로 시도 (우선순위 순)
            String[] contentSelectors = {
                "#novel_content",           // ID가 novel_content인 요소 (최우선)
                ".view-img",                // 클래스가 view-img인 요소 (스크린샷에서 확인)
                ".novel-content",           // 클래스가 novel-content인 요소
                ".view-content",            // 클래스가 view-content인 요소
                "#content",                 // ID가 content인 요소
                ".content",                 // 클래스가 content인 요소
                ".text-content",            // 클래스가 text-content인 요소
                "[data-content]",           // data-content 속성이 있는 요소
                ".post-content",            // 클래스가 post-content인 요소
                ".entry-content",           // 클래스가 entry-content인 요소
                "main",                     // main 태그
                "article"                   // article 태그
            };

            for (String selector : contentSelectors) {
                if (contentFound) break;

                Element element = doc.selectFirst(selector);
                if (element != null) {
                    // 불필요한 요소들 제거
                    element.select("script, style, noscript, .ad, .advertisement, .banner, .adsense, [id*=ad], [class*=ad], " +
                                 ".navigation, .nav, .menu, .header, .footer, .sidebar, .comments, .comment, " +
                                 ".share, .social, .related, .recommend, .tag, .category, .breadcrumb, " +
                                 ".author-info, .post-meta, .post-info, .date, .views, .likes").remove();

                    // p 태그들 먼저 시도
                    Elements paragraphs = element.select("p");
                    if (!paragraphs.isEmpty()) {
                        for (Element p : paragraphs) {
                            String paragraphText = p.text().trim();
                            // 짧거나 의미없는 텍스트 제외
                            if (!paragraphText.isEmpty() && paragraphText.length() > 15 && 
                                !isUnwantedText(paragraphText)) {
                                contentBuilder.append(paragraphText).append("\n\n");
                                contentFound = true;
                            }
                        }
                    }

                    if (!contentFound) {
                        // p 태그가 없으면 전체 텍스트 사용
                        String text = element.text().trim();
                        if (text.length() > 100 && !isUnwantedText(text)) {
                            contentBuilder.append(text).append("\n\n");
                            contentFound = true;
                            break;
                        }
                    }
                }
            }

            // 2. 컨테이너 클래스들 시도
            if (!contentFound) {
                Elements containerElements = doc.select(".view-padding, .container, .wrapper, .main-content");
                for (Element container : containerElements) {
                    // 컨테이너 내의 p 태그들 먼저 시도
                    Elements paragraphs = container.select("p");
                    if (!paragraphs.isEmpty()) {
                        for (Element p : paragraphs) {
                            String paragraphText = p.text().trim();
                            if (!paragraphText.isEmpty() && paragraphText.length() > 15 && 
                                !isUnwantedText(paragraphText)) {
                                contentBuilder.append(paragraphText).append("\n\n");
                                contentFound = true;
                            }
                        }
                    }
                    if (contentFound) break;
                }
            }

            // 3. 최후의 수단으로 div 태그들 검색
            if (!contentFound) {
                Elements divs = doc.select("div");
                for (Element div : divs) {
                    String divText = div.ownText().trim(); // 자식 요소 제외하고 직접 텍스트만
                    if (divText.length() > 100 && !isUnwantedText(divText)) {
                        contentBuilder.append(divText).append("\n\n");
                        contentFound = true;
                        break;
                    }
                }
            }

            String result = contentBuilder.toString().trim();
            return result;

        } catch (Exception e) {
            return "소설 내용을 파싱하는 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    // 댓글 파싱
    private void parseComments(Document d) {
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
    }

    // 개별 댓글 파싱
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

    /**
     * 원하지 않는 텍스트인지 확인하는 메서드
     */
    private boolean isUnwantedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        
        String lowerText = text.toLowerCase();
        
        // 원하지 않는 텍스트 패턴들
        String[] unwantedPatterns = {
            "댓글", "comment", "공유", "share", "좋아요", "like", "추천", "recommend",
            "관련", "related", "인기", "popular", "이전", "previous", "다음", "next",
            "메뉴", "menu", "로그인", "login", "회원가입", "register", "sign up",
            "copyright", "저작권", "배너", "banner", "광고", "advertisement",
            "사이트맵", "sitemap", "이용약관", "terms", "개인정보", "privacy",
            "미리보기", "preview", "목록", "list", "홈", "home", "검색", "search",
            "javascript", "document.", "function", "var ", "let ", "const ", "return",
            "날짜", "date", "시간", "time", "조회", "view", "작성자", "author",
            "태그", "tag", "카테고리", "category", "소설가", "작가"
        };
        
        for (String pattern : unwantedPatterns) {
            if (lowerText.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        
        // 너무 짧거나 숫자/특수문자만 있는 텍스트
        if (text.length() < 10 || text.matches("^[\\d\\s\\p{Punct}]+$")) {
            return true;
        }
        
        return false;
    }

    // Manga 호환성을 위한 메서드들
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Novel novel = (Novel) obj;
        return id == novel.id;
    }

    public int hashCode() {
        return Integer.hashCode(id);
    }

    public String toString() {
        return name;
    }

    // Manga와 Novel 간 변환을 위한 메서드들
    public Manga toManga() {
        Manga manga = new Manga(id, name, date, baseMode);
        manga.setTitle(title);
        manga.setMode(mode);
        return manga;
    }

    public static Novel fromManga(Manga manga) {
        Novel novel = new Novel(manga.getId(), manga.getName(), manga.getDate(), base_novel);
        novel.setTitle(manga.getTitle());
        novel.setMode(manga.getMode());
        return novel;
    }

    // Manga 호환성을 위한 추가 메서드들
    public int getSeed() {
        return seed;
    }

    public void setOfflinePath(String offlinePath) {
        this.offlinePath = offlinePath;
    }

    public String getOfflinePath() {
        return offlinePath;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getUrl() {
        return baseModeStr(baseMode) + "/" + id;
    }

    public boolean useBookmark() {
        return false; // 소설에서는 북마크 사용 안 함
    }

    public boolean isOnline() {
        return mode == 0;
    }

    public boolean isOffline() {
        return mode != 0;
    }

    public Novel nextEp() {
        if (nextEp != null) {
            return nextEp;
        }
        if (eps != null && !eps.isEmpty()) {
            int currentIndex = eps.indexOf(this);
            if (currentIndex >= 0 && currentIndex > 0) {
                return eps.get(currentIndex - 1); // 다음 에피소드 (인덱스가 작을수록 최신)
            }
        }
        return null;
    }

    public Novel prevEp() {
        if (prevEp != null) {
            return prevEp;
        }
        if (eps != null && !eps.isEmpty()) {
            int currentIndex = eps.indexOf(this);
            if (currentIndex >= 0 && currentIndex < eps.size() - 1) {
                return eps.get(currentIndex + 1); // 이전 에피소드 (인덱스가 클수록 이전)
            }
        }
        return null;
    }

    public void setPrevEp(Novel novel) {
        this.prevEp = novel;
    }

    public void setNextEp(Novel novel) {
        this.nextEp = novel;
    }

    // 오프라인 관련 메서드들
    public void saveOffline(Context context, Uri uri) {
        // 오프라인 저장 기능 (향후 구현)
        // 소설 내용을 텍스트 파일로 저장하는 기능
    }

    // 리스너 인터페이스
    public interface Listener {
        void setMessage(String msg);
    }
}
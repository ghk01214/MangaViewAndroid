package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;

import okhttp3.Response;

import static ml.melun.mangaview.mangaview.MTitle.baseModeStr;

// 소설 페이지의 텍스트 내용을 가져오는 클래스
public class NovelPage {
    private String title;
    private String content;
    private Manga manga;
    private int retryCount = 0; // 재시도 횟수 추적
    private static final int MAX_RETRIES = 3; // 최대 재시도 횟수
    
    public NovelPage(Manga manga){
        this.manga = manga;
    }
    
    public String getTitle(){
        return title;
    }
    
    public String getContent(){
        return content;
    }
    
    // 웹에서 소설 내용을 가져와 파싱합니다.
    public int fetch(CustomHttpClient client){
        try {
            // 디버그용 테스트 소설 제공
            if(manga.getId() == 999999) {
                title = "테스트 소설";
                content = "이것은 테스트용 소설 내용입니다.\n\n" +
                         "첫 번째 문단입니다. 여기에는 소설의 시작 부분이 들어갑니다.\n\n" +
                         "두 번째 문단입니다. 스토리가 전개되는 부분입니다.\n\n" +
                         "세 번째 문단입니다. 클라이맥스 부분입니다.\n\n" +
                         "마지막 문단입니다. 결말 부분입니다.\n\n" +
                         "텍스트 뷰어가 정상적으로 작동하는지 확인하기 위한 긴 텍스트입니다. " +
                         "스크롤이 잘 작동하는지, 텍스트가 적절히 표시되는지 테스트할 수 있습니다.";
                return 0;
            }
            
            // 소설 페이지 URL 생성
            String url = "/" + baseModeStr(manga.getBaseMode()) + "/" + manga.getId();
            
            Response response = client.mget(url, true, null);
            String body = response.body().string();
            
            if(body.contains("Connect Error: Connection timed out")){
                // 타임아웃 발생 시 재시도 (최대 횟수 제한)
                response.close();
                retryCount++;
                if(retryCount < MAX_RETRIES){
                    return fetch(client);
                } else {
                    content = "연결 타임아웃이 반복 발생했습니다. 나중에 다시 시도해주세요.";
                    return 1;
                }
            }
            
            Document doc = Jsoup.parse(body);
            doc.outputSettings().charset(StandardCharsets.UTF_8);
            
            if(response.code() >= 400){
                response.close();
                content = "HTTP 오류 발생: " + response.code() + " - " + response.message();
                return 1; // HTTP 오류
            }
            
            // 캡차나 차단 페이지 감지
            if(body.contains("captcha") || body.contains("cloudflare") || 
               body.contains("Just a moment") || body.contains("Please wait") ||
               body.contains("DDoS protection") || body.contains("Access denied")){
                response.close();
                content = "사이트에서 접근을 차단했습니다. 잠시 후 다시 시도해주세요.";
                return 1; // 캡차/차단 감지
            }
            
            // 제목 추출
            Element titleElement = doc.selectFirst("h1.view-title, .title, h1");
            if(titleElement != null){
                title = titleElement.text().trim();
            }
            
            // 소설 내용 추출 - 여러 가능한 선택자 시도
            Elements contentElements = doc.select(".view-content, .content, .novel-content, .text-content, #content");
            
            StringBuilder contentBuilder = new StringBuilder();
            
            if(!contentElements.isEmpty()){
                for(Element contentElement : contentElements){
                    // 스크립트, 스타일, 광고 등 불필요한 요소 제거
                    contentElement.select("script, style, .ad, .advertisement, .banner").remove();
                    
                    String text = contentElement.text();
                    if(text != null && !text.trim().isEmpty()){
                        contentBuilder.append(text.trim()).append("\n\n");
                    }
                }
            } else {
                // 기본 선택자로 본문 내용 추출 시도
                Element bodyElement = doc.selectFirst("body");
                if(bodyElement != null){
                    bodyElement.select("script, style, nav, header, footer, .menu, .navigation, .ad, .advertisement").remove();
                    
                    Elements paragraphs = bodyElement.select("p, div.text, .paragraph");
                    for(Element p : paragraphs){
                        String text = p.text().trim();
                        if(text.length() > 20){ // 의미있는 텍스트만 추가
                            contentBuilder.append(text).append("\n\n");
                        }
                    }
                }
            }
            
            content = contentBuilder.toString().trim();
            
            // 내용이 너무 짧으면 전체 텍스트 추출 시도
            if(content.length() < 100){
                Elements allText = doc.select("*");
                contentBuilder = new StringBuilder();
                
                for(Element element : allText){
                    if(element.tagName().matches("p|div|span|h[1-6]")){
                        String text = element.ownText().trim();
                        if(text.length() > 10){
                            contentBuilder.append(text).append("\n");
                        }
                    }
                }
                
                content = contentBuilder.toString().trim();
            }
            
            response.close();
            
            // 내용이 여전히 비어있으면 오류 반환
            if(content == null || content.trim().isEmpty()){
                content = "소설 내용을 찾을 수 없습니다.";
                return 1;
            }
            
            return 0; // 성공
            
        } catch (Exception e) {
            e.printStackTrace();
            content = "소설 내용을 불러오는 중 오류가 발생했습니다: " + e.getMessage();
            return 1;
        }
    }
}
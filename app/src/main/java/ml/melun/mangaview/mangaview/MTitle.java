package ml.melun.mangaview.mangaview;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

// 만화 작품의 기본 정보(메타데이터)를 담는 클래스. Title 클래스의 간소화된 버전으로, 주로 목록 표시에 사용됩니다.
public class MTitle{
    String name; // 제목
    int id; // 고유 ID
    String thumb; // 썸네일 이미지 URL
    String author; // 작가
    List<String> tags; // 태그 목록
    String release; // 발행 정보 (예: 주간, 월간, 완결)
    String path; // 오프라인 저장 경로
    int baseMode = base_comic; // 기본값은 만화(comic)

    public MTitle(){

    }

    // 생성자
    public MTitle(String name, int id, String thumb, String author, List<String> tags, String release, int baseMode) {
        this.name = name.replace("\"", ""); // 이름에서 따옴표 제거
        this.id = id;
        this.thumb = thumb;
        this.tags = tags;
        this.release = release;
        this.author = author;
        this.baseMode = baseMode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    // 작품 종류(baseMode)를 반환합니다. auto일 경우 comic으로 기본값을 설정합니다.
    public int getBaseMode() {
        if(baseMode == base_auto)
            baseMode = base_comic;
        return baseMode;
    }

    // 작품 종류를 한글 문자열로 반환합니다.
    public String getBaseModeStr(){
        return baseModeKorStr(baseMode);
    }

    public void setBaseMode(int baseMode) {
        this.baseMode = baseMode;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getThumb() {
        return thumb;
    }

    public String getAuthor() {
        if(author == null) return "";
        return author;
    }

    public List<String> getTags(){
        if(tags==null) return new ArrayList<>();
        return tags;
    }

    public String getRelease() {
        return release;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name.replace("\"", "");
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    @Override
    public MTitle clone() {
        return new MTitle(name, id, thumb, author, tags, release, baseMode);
    }

    // 작품 종류 상수
    public static final int base_auto = 0; // 자동
    public static final int base_comic = 1; // 만화
    public static final int base_webtoon = 2; // 웹툰

    // 작품 종류(int)를 URL에 사용될 영문 문자열로 변환합니다.
    public static String baseModeStr(int mode){
        switch(mode){
            case base_comic:
                return "comic";
            case base_webtoon:
                return "webtoon";
            default:
                return "comic";
        }
    }
    // 작품 종류(int)를 화면에 표시될 한글 문자열로 변환합니다.
    public static String baseModeKorStr(int mode){
        switch(mode){
            case base_comic:
                return "만화";
            case base_webtoon:
                return "웹툰";
            default:
                return "만화";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return name + " . " + id + " . " +  thumb + " . " + author + " . " + baseMode;
    }

    // 객체 동등성 비교. baseMode와 id가 모두 같아야 같은 객체로 취급합니다.
    @Override
    public boolean equals(Object obj) {
        return ((MTitle)obj).getBaseMode() == this.baseMode && ((MTitle)obj).getId() == this.id ;
    }
}

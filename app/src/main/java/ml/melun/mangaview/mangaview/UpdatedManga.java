package ml.melun.mangaview.mangaview;

import java.util.List;

// 업데이트된 만화 정보를 나타내는 클래스. Manga 클래스를 상속받아 작가와 태그 정보를 추가로 가집니다.
public class UpdatedManga extends Manga{
    String author; // 작가
    List<String> tag; // 태그 목록

    // 생성자
    UpdatedManga(int i, String n, String d, int baseMode, String author, List<String> tag){
        super(i, n, d, baseMode);
        this.author = author;
        this.tag = tag;
    }

    // 작가 이름을 반환합니다.
    public String getAuthor() {
        return author;
    }

    // 태그 목록을 반환합니다.
    public List<String> getTag() {
        return tag;
    }
}

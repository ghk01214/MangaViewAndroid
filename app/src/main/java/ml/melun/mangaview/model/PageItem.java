package ml.melun.mangaview.model;

import androidx.annotation.Nullable;

import ml.melun.mangaview.mangaview.Manga;

// 만화 뷰어의 단일 페이지를 나타내는 데이터 모델 클래스
public class PageItem{
    public static final int FIRST = 0; // 페이지의 첫 번째 부분 (또는 단일 페이지)
    public static final int SECOND = 1; // 페이지의 두 번째 부분 (분할된 페이지의 경우)

    // 생성자
    public PageItem(int index, String img, Manga manga) {
        this.index = index;
        this.img = img;
        this.manga = manga;
        this.side = FIRST; // 기본값은 첫 번째 부분
    }

    // 생성자 (분할된 페이지의 면을 지정)
    public PageItem(int index, String img, Manga manga, int side){
        this.index = index;
        this.img = img;
        this.manga = manga;
        this.side = side;
    }

    // 객체의 해시코드를 반환. 만화 ID, 페이지 인덱스, 면을 기반으로 생성
    @Override
    public int hashCode() {
        return manga.getId()*10000 + index*10 + this.side;
    }

    // 객체 동등성 비교. 페이지 인덱스와 만화 객체가 같으면 true를 반환
    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof PageItem){
            PageItem p = (PageItem)obj;
            return p.index == this.index && p.manga.equals(this.manga);
        }else
            return false;
    }

    public int index; // 페이지 번호
    public int side; // 페이지의 면 (FIRST 또는 SECOND)
    public String img; // 페이지 이미지 URL
    public Manga manga; // 이 페이지가 속한 만화 객체
}
package ml.melun.mangaview.interfaces;

// MainActivity와 Fragment 간의 통신을 위한 콜백 인터페이스
public interface MainActivityCallback{
    // 검색을 요청하는 메서드. 검색어를 인자로 받습니다.
    void search(String query);
}
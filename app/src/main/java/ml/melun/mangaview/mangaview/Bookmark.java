package ml.melun.mangaview.mangaview;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.Preference;

// 사용자의 북마크(선호작) 정보를 가져오는 클래스
public class Bookmark {
    List<MTitle> result; // 북마크된 작품 목록
    int page = -1; // 현재 페이지 (현재 구현에서는 사용되지 않음)
    boolean last = false; // 마지막 페이지인지 여부

    public Bookmark(){

    }

    // 웹에서 북마크 목록을 가져옵니다. (현재 미구현 상태)
    public int fetch(CustomHttpClient client){
        result = new ArrayList<>();
        //todo: 이 기능을 구현해야 합니다.
        return 0;
    }

    // 마지막 페이지인지 여부를 반환합니다.
    public boolean isLast() {
        return !last;
    }

    // 북마크 목록을 반환합니다.
    public List<MTitle> getResult(){
        return this.result;
    }

    // 서버의 북마크 목록을 가져와 로컬 즐겨찾기에 동기화(가져오기)합니다.
    public static int importBookmark(Preference p, CustomHttpClient client){
        try {
            Bookmark b = new Bookmark();
            List<MTitle> bookmarks = new ArrayList<>();
            // 모든 페이지의 북마크를 가져올 때까지 반복
            while (b.isLast()) {
                if (b.fetch(client) == 0) // fetch가 성공하면
                    bookmarks.addAll(b.getResult()); // 결과에 추가
                else
                    return 1; // 실패 시 1 반환
            }

            // 가져온 북마크 목록을 순회하며 로컬 즐겨찾기에 없는 항목을 추가
            for (MTitle t : bookmarks) {
                if (p.findFavorite(t) < 0) // 즐겨찾기에 없는 경우
                    p.toggleFavorite(t, 0); // 즐겨찾기에 추가
            }
        }catch (Exception e){
            return 1; // 예외 발생 시 1 반환
        }
        return 0; // 성공 시 0 반환
    }
}

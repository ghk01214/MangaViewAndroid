package ml.melun.mangaview.mangaview;

import java.util.List;

// 다운로드용 만화 작품 정보를 나타내는 클래스. MTitle을 상속받아 에피소드 목록을 가집니다.
public class DownloadTitle extends MTitle {
    private List<Manga> eps; // 다운로드할 (또는 다운로드된) 에피소드 목록

    // Title 객체로부터 DownloadTitle 객체를 생성합니다.
    public DownloadTitle(Title t){
        super(t.getName(), t.getId(), t.getThumb(), t.getAuthor(), t.getTags(), t.getRelease(), t.getBaseMode());
        this.eps = t.getEps();
    }

    // 에피소드 목록을 반환합니다.
    public List<Manga> getEps() {
        return eps;
    }

    // 에피소드 목록을 설정합니다.
    public void setEps(List<Manga> eps) {
        this.eps = eps;
    }
}

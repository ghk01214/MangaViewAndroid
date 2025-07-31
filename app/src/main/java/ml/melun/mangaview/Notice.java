package ml.melun.mangaview;

// 공지사항 데이터를 나타내는 클래스
public class Notice{
    int id = -1; // 공지사항 고유 ID
    String title, date, content; // 제목, 날짜, 내용

    // 생성자
    Notice(int id, String title, String date, String content){
        this.id = id;
        this.title = title;
        this.date = date;
        this.content = content;
    }

    // 객체 동등성 비교 (ID를 기준으로 비교)
    @Override
    public boolean equals(Object obj) {
        return this.id == ((Notice)obj).getId();
    }

    // 해시코드 반환 (ID를 기준으로 생성)
    @Override
    public int hashCode() {
        return id;
    }

    // 공지사항 ID를 반환합니다.
    public int getId() {
        return id;
    }

    // 공지사항 내용을 반환합니다.
    public String getContent() {
        return content;
    }

    // 공지사항 날짜를 반환합니다.
    public String getDate() {
        return date;
    }

    // 공지사항 제목을 반환합니다.
    public String getTitle() {
        return title;
    }
}

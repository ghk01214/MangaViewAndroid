package ml.melun.mangaview.mangaview;

// 만화의 댓글 정보를 담는 데이터 모델 클래스
public class Comment {

    // 생성자
    public Comment(String user, String ts, String icon, String content, int indent, int likes, int level) {
        this.user = user; // 작성자 이름
        this.icon = icon; // 작성자 아이콘 URL
        this.content = content; // 댓글 내용
        this.timestamp = ts; // 작성 시간
        this.indent = indent; // 들여쓰기 (대댓글 깊이)
        this.likes = likes; // 좋아요 수
        this.level = level; // 작성자 레벨
    }

    public String getContent() {return content;}
    public String getUser() {return user;}
    public String getIcon() {return icon;}
    public String getTimestamp() { return timestamp;}
    public int getIndent() { return indent; }
    public int getLikes() { return likes; }
    public int getLevel() { return level; }

    String content, user, icon, timestamp;
    int indent = 0;
    int likes = 0;
    int level = 0;
}

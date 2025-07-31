package ml.melun.mangaview.mangaview;

import java.util.ArrayList;
import java.util.Collection;

// 랭킹 목록을 나타내는 클래스. 이름을 가진 ArrayList라고 볼 수 있습니다.
public class Ranking<E> extends ArrayList<E> {
    String name; // 랭킹의 이름 (예: "일간 랭킹", "주간 랭킹")

    // 생성자. 랭킹 이름을 초기화합니다.
    public Ranking(String name){
        super();
        this.name = name;
    }

    // 생성자. 다른 컬렉션과 랭킹 이름으로 초기화합니다.
    public Ranking(Collection<? extends E> c, String name) {
        super(c);
        this.name = name;
    }

    // 랭킹의 이름을 반환합니다.
    public String getName(){
        return this.name;
    }
}

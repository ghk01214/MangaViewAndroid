package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.MainPage;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.mangaview.MTitle.base_comic;

// 메인 페이지의 만화 탭에 표시될 데이터를 관리하고 뷰를 생성하는 RecyclerView 어댑터
public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context mainContext;
    LayoutInflater mInflater, itemInflater;
    MainUpdatedAdapter uadapter; // 최신 업데이트 만화 섹션의 어댑터
    onItemClick mainClickListener; // 아이템 클릭 리스너
    boolean dark, loaded = false; // 다크 테마 여부, 데이터 로드 완료 여부

    List<Object> data; // 표시할 데이터 목록

    // 뷰 타입 상수
    final static int TITLE = 0; // 작품 제목 (Title) 타입
    final static int MANGA = 1; // 만화 에피소드 (Manga) 타입
    final static int TAG = 2; // 태그 타입
    final static int HEADER = 3; // 헤더 타입
    final static int UPDATED = 4; // 최근 업데이트된 만화 섹션 타입

    ButtonHeader addh; // '더보기' 버튼이 있는 헤더
    Header besth, updh, hish, weekh; // 각 섹션의 헤더

    public MainAdapter(Context main) {
        super();
        mainContext = main;
        dark = p.getDarkTheme();
        this.mInflater = LayoutInflater.from(main);
        this.itemInflater = LayoutInflater.from(main);

        data = new ArrayList<>();

        uadapter = new MainUpdatedAdapter(main);
        addh = new ButtonHeader("최신 업데이트 만화", () -> mainClickListener.clickedMoreUpdated());

        // 초기 데이터 구조 설정
        data.add(addh); // 최신 업데이트 만화 헤더
        data.add(null); // 최신 업데이트 만화 목록 (MainUpdatedAdapter가 관리)

        updh = new Header("북마크 업데이트");
        data.add(updh);

        hish = new Header("최근에 본 만화");
        data.add(hish);

        weekh = new Header("주간 베스트");
        data.add(weekh);

        besth = new Header("일본만화 베스트");
        data.add(besth);

        data.add(new Header("이름"));
        for(String s : mainContext.getResources().getStringArray(R.array.tag_name)){
            data.add(new NameTag(s));
        }
        data.add(new Header("장르"));
        for(String s : mainContext.getResources().getStringArray(R.array.tag_genre)){
            data.add(new GenreTag(s));
        }
        data.add(new Header("발행"));
        for(String s : mainContext.getResources().getStringArray(R.array.tag_release)){
            data.add(new ReleaseTag(s));
        }

        setHasStableIds(true);
        notifyDataSetChanged();
        uadapter.setLoad("URL 업데이트중..."); // 초기 로딩 메시지 설정
    }

    // 메인 페이지 데이터를 가져옵니다.
    public void fetch(){
        uadapter.setLoad(); // 최신 업데이트 만화 섹션 로딩 상태로 설정
        new MainFetcher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public long getItemId(int position) {
        if(data.get(position) == null) return -1;
        return data.get(position).hashCode();
    }


    @Override
    public int getItemViewType(int position) {
        Object o = data.get(position);
        if(o == null)
            return UPDATED;
        else if(o instanceof Title)
            return TITLE;
        else if(o instanceof Manga)
            return MANGA;
        else if(o instanceof Header)
            return HEADER;
        else if(o instanceof Tag)
            return TAG;
        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        switch (viewType){
            case TITLE:
                v = mInflater.inflate(R.layout.main_item_ranking,parent,false);
                return new TitleHolder(v);
            case MANGA:
                v = mInflater.inflate(R.layout.main_item_ranking,parent,false);
                return new MangaHolder(v);
            case HEADER:
                v = mInflater.inflate(R.layout.item_main_header,parent,false);
                return new HeaderHolder(v);
            case UPDATED:
                v = mInflater.inflate(R.layout.item_main_updated_list, parent, false);
                return new AddedHolder(v);
            case TAG:
                v = mInflater.inflate(R.layout.item_main_tag,parent,false);
                return new TagHolder(v);
        }
        return null;
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int t = getItemViewType(position);
        switch (t){
            case TITLE:
                ((TitleHolder)holder).setTitle((Title)data.get(position),0);
                break;
            case MANGA:
                ((MangaHolder) holder).setManga((Manga)data.get(position),0);
                break;
            case HEADER:
                ((HeaderHolder)holder).setHeader((Header)data.get(position));
                break;
            case UPDATED:
                // 이 뷰 타입은 AddedHolder에서 자체적으로 처리하므로 여기서는 특별한 바인딩 없음
                break;
            case TAG:
                ((TagHolder) holder).tag.setText(data.get(position).toString());
                break;
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // 최신 업데이트 만화 목록을 표시하는 뷰홀더
    class AddedHolder extends RecyclerView.ViewHolder{
        RecyclerView updatedList;
        public AddedHolder(View itemView) {
            super(itemView);
            updatedList = itemView.findViewById(R.id.main_tag);
            LinearLayoutManager lm = new NpaLinearLayoutManager(mainContext);
            lm.setOrientation(RecyclerView.HORIZONTAL);
            updatedList.setLayoutManager(lm);
            updatedList.setAdapter(uadapter);
            uadapter.setClickListener(new MainUpdatedAdapter.OnClickCallback() {
                @Override
                public void onclick(Manga m) {
                    mainClickListener.clickedManga(m);
                }

                @Override
                public void refresh() {
                    fetch(); // 데이터 새로고침
                    mainClickListener.clickedRetry(); // 재시도 콜백 호출
                }
            });
        }
    }

    // 만화 에피소드 아이템을 표시하는 뷰홀더
    class MangaHolder extends RecyclerView.ViewHolder{

        TextView text;
        CardView card;
        TextView rank;
        View rankLayout;

        public MangaHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.header_title);
            card = itemView.findViewById(R.id.ranking_card);
            rank = itemView.findViewById(R.id.ranking_rank);
            rankLayout = itemView.findViewById(R.id.ranking_rank_layout);
            if(card!=null){
                if(dark) {
                    card.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
                }
            }
        }

        public void setManga(Manga m, int r){
            text.setText(m.getName());
            card.setOnClickListener(v -> {
                if(m!=null && m.getId()>0)
                    mainClickListener.clickedManga(m);
            });

            if(m instanceof MainPage.RankingManga){ // 랭킹 정보가 있는 만화일 경우 순위 표시
                rankLayout.setVisibility(View.VISIBLE);
                rank.setText(String.valueOf(((MainPage.RankingManga)m).getRanking()));
            }else{
                rankLayout.setVisibility(View.GONE);
            }
        }
    }

    // 헤더를 표시하는 뷰홀더
    class HeaderHolder extends RecyclerView.ViewHolder{ // 헤더 텍스트뷰
        TextView text;
        ImageView button; // 버튼 이미지뷰
        View container; // 컨테이너 뷰

        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.header_title);
            button = itemView.findViewById(R.id.header_button);
            container = itemView.findViewById(R.id.header_container);
            if(dark)
                this.button.setColorFilter(Color.WHITE);
            else
                this.button.setColorFilter(Color.DKGRAY);
        }

        public void setHeader(Header h){
            this.text.setText(h.header);
            if(h instanceof ButtonHeader){ // 버튼이 있는 헤더일 경우
                this.container.setClickable(true);
                this.button.setVisibility(View.VISIBLE);
                this.container.setOnClickListener(view -> ((ButtonHeader)h).callback()); // 클릭 리스너 설정
            }else{ // 일반 헤더일 경우
                this.container.setClickable(false);
                this.button.setVisibility(View.GONE);
            }
        }
    }

    // 작품 제목 아이템을 표시하는 뷰홀더
    class TitleHolder extends RecyclerView.ViewHolder{
        TextView text;
        CardView card;
        TextView rank;
        View rankLayout;

        TitleHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.header_title);
            card = itemView.findViewById(R.id.ranking_card);
            rank = itemView.findViewById(R.id.ranking_rank);
            rankLayout = itemView.findViewById(R.id.ranking_rank_layout);
            if(card!=null){
                if(dark) {
                    card.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
                }
            }
        }
        public void setTitle(Title t, int r){
            text.setText(t.getName());
            card.setOnClickListener(v -> {
                if(t!=null && t.getId()>0)
                    mainClickListener.clickedTitle(t);
            });

            if(t instanceof MainPage.RankingTitle){ // 랭킹 정보가 있는 작품일 경우 순위 표시
                rankLayout.setVisibility(View.VISIBLE);
                rank.setText(String.valueOf(((MainPage.RankingTitle)t).getRanking()));
            }else{
                rankLayout.setVisibility(View.GONE);
            }
        }
    }

    // 태그 아이템을 표시하는 뷰홀더
    class TagHolder extends RecyclerView.ViewHolder{
        TextView tag;
        CardView card;
        public TagHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.mainTagCard);
            tag = itemView.findViewById(R.id.main_tag_text);

            if (dark)
                card.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
            else
                card.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorBackground));

            card.setOnClickListener(v -> {
                Tag t = (Tag) data.get(getAdapterPosition());
                if(t instanceof NameTag) mainClickListener.clickedName(t.tag);
                else if(t instanceof GenreTag) mainClickListener.clickedGenre(t.tag);
                else if(t instanceof ReleaseTag) mainClickListener.clickedRelease(t.tag);
            });
        }
    }

    // 태그 기본 클래스
    static class Tag{
        public String tag;
        public Tag(String tag){this.tag=tag;}
        @NonNull
        @Override
        public String toString() {
            return tag;
        }
    }

    // 이름 태그 클래스
    class NameTag extends Tag{
        public NameTag(String tag) {
            super(tag);
        }
    }

    // 장르 태그 클래스
    class GenreTag extends Tag{
        public GenreTag(String tag) {
            super(tag);
        }
    }

    // 발행 구분 태그 클래스
    class ReleaseTag extends Tag{
        public ReleaseTag(String tag) {
            super(tag);
        }
    }

    // 헤더 기본 클래스
    static class Header{
        public String header;

        public Header(String header) {
            this.header = header;
        }
    }

    // 버튼이 있는 헤더 클래스
    class ButtonHeader extends Header{
        public String header;
        Runnable callback;

        public ButtonHeader(String header, Runnable callback) {
            super(header);
            this.header = header;
            this.callback = callback;
        }

        public void callback() {
            callback.run();
        }
    }

    // 결과 없음 만화 객체
    static class NoResultManga extends Manga{
        public NoResultManga() {
            super(-1, "결과 없음", "", base_comic);
        }
    }

    // 메인 클릭 리스너를 설정합니다.
    public void setMainClickListener(onItemClick main) {
        this.mainClickListener = main;
    }

    // 아이템 클릭 이벤트를 위한 인터페이스
    public interface onItemClick{
        void clickedManga(Manga m); // 만화 에피소드 클릭 시
        void clickedGenre(String t); // 장르 태그 클릭 시
        void clickedName(String t); // 이름 태그 클릭 시
        void clickedRelease(String t); // 발행 구분 태그 클릭 시
        void clickedTitle(Title t); // 작품 제목 클릭 시
        void clickedMoreUpdated(); // 최신 업데이트 만화 더보기 클릭 시
        void captchaCallback(); // 캡차 발생 시
        void clickedSearch(String query); // 검색어 클릭 시
        void clickedRetry(); // 재시도 클릭 시
    }

    // 메인 페이지 데이터를 비동기적으로 가져오는 AsyncTask
    private class MainFetcher extends AsyncTask<Void, Integer, MainPage> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected MainPage doInBackground(Void... params) {
            Map<String,String> cookie = new HashMap<>();
            Login login = p.getLogin();
            if(login!=null && login.isValid()){
                p.getLogin().buildCookie(cookie);
            }
            return new MainPage(httpClient);
        }

        @Override
        protected void onPostExecute(MainPage u) {
            super.onPostExecute(u);
            // 데이터 로드 후 어댑터 업데이트
            if(u.getRecent().size() == 0){
                // 캡차 발생 시
                mainClickListener.captchaCallback();
            }
            uadapter.setData(u.getRecent()); // 최신 업데이트 만화 데이터 설정

            // 기존의 '결과 없음' 아이템 제거
            for(int i=data.size()-1; i>=0; i--){
                if(data.get(i) instanceof NoResultManga) {
                    data.remove(i);
                    notifyItemRemoved(i);
                }
            }

            // 주간 베스트 데이터 추가
            int i = data.indexOf(weekh);
            if(i>-1) {
                if (u.getWeeklyRanking().size() == 0 && !(data.get(i+1) instanceof NoResultManga)){ // 결과가 없으면 '결과 없음' 표시
                    data.add(++i, new NoResultManga());
                    notifyItemInserted(i);
                }
                else {
                    for (MainPage.RankingManga m : u.getWeeklyRanking()) {
                        data.add(++i, m);
                        notifyItemInserted(i);
                    }
                }
            }

            i = data.indexOf(besth);
            if(i>-1) {
                if (u.getRanking().size() == 0){
                    data.add(++i, new NoResultManga());
                    notifyItemInserted(i);
                }
                else {
                    for (MainPage.RankingTitle t : u.getRanking()) {
                        data.add(++i, t);
                        notifyItemInserted(i);
                    }
                }
            }

            i = data.indexOf(hish);
            if(i>-1) {
                if (u.getOnlineRecent().size() == 0){
                    data.add(++i, new NoResultManga());
                    notifyItemInserted(i);
                }
                else {
                    for (Manga m : u.getOnlineRecent()) {
                        data.add(++i, m);
                        notifyItemInserted(i);
                    }
                }
            }

            i = data.indexOf(updh);
            if(i>-1){
                if(u.getFavUpdate().size() == 0){
                    data.add(++i, new NoResultManga());
                    notifyItemInserted(i);
                } else{
                    for(Manga m : u.getFavUpdate()){
                        data.add(++i, m);
                        notifyItemInserted(i);
                    }
                }
            }
        }
    }

}



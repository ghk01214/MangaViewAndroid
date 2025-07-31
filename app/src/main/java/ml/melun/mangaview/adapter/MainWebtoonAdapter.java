package ml.melun.mangaview.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.MainPageWebtoon;
import ml.melun.mangaview.mangaview.Ranking;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

// 메인 페이지의 웹툰 탭에 표시될 데이터를 관리하고 뷰를 생성하는 RecyclerView 어댑터
public class MainWebtoonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    LinearLayoutManager manager;
    Context context;
    boolean dark;
    LayoutInflater inflater;
    List<Ranking<?>> dataSet; // 웹툰 랭킹 데이터를 담을 리스트
    MainAdapter.onItemClick listener; // 아이템 클릭 리스너

    final static int HEADER = 23; // 헤더 뷰 타입

    // 각 랭킹 섹션의 뷰 타입
    final static int NN = 24; // 일반연재 최신
    final static int AN = 25; // 성인웹툰 최신
    final static int GN = 26; // BL/GL 최신
    final static int CN = 27; // 일본만화 최신
    final static int NB = 28; // 일반연재 베스트
    final static int AB = 29; // 성인웹툰 베스트
    final static int GB = 30; // BL/GL 베스트
    final static int CB = 31; // 일본만화 베스트

    int[] headers = {0,1,2,3,4,5,6,7}; // 각 헤더의 위치를 저장하는 배열

    public MainWebtoonAdapter(Context context){
        this.context = context;
        this.dark = p.getDarkTheme();
        manager = new NpaLinearLayoutManager(context);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        inflater = LayoutInflater.from(context);
        dataSet = MainPageWebtoon.getBlankDataSet(); // 초기 빈 데이터셋 설정
        setLoading(); // 로딩 상태 표시
        setHasStableIds(false);
        setLoading();
    }

    // 데이터를 비동기적으로 가져옵니다.
    public void fetch(){
        new Fetcher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // 로딩 상태를 표시하도록 데이터셋을 초기화합니다.
    public void setLoading(){
        dataSet = MainPageWebtoon.getBlankDataSet();
        notifyDataSetChanged();
    }

    // 클릭 리스너를 설정합니다.
    public void setListener(MainAdapter.onItemClick listener){
        this.listener = listener;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == HEADER){
            return new HeaderHolder(inflater.inflate(R.layout.item_main_header, parent, false));
        }else{
            return new ItemHolder(inflater.inflate(R.layout.main_item_ranking, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if(dataSet.size()==0)
            return;
        if(type == HEADER){
            HeaderHolder h = (HeaderHolder) holder;
            for(int i=0; i<headers.length; i++){
                if(headers[i] == position) {
                    h.title.setText(dataSet.get(i).getName()); // 헤더 제목 설정
                    break;
                }
            }
        }else if(type<=CB){ // 아이템 뷰 타입일 경우
            ItemHolder h = (ItemHolder) holder;
            int setIndex = type-24; // 데이터셋 인덱스 계산
            int realPosition = position-(headers[setIndex])-1; // 실제 아이템 위치 계산
            Object d = dataSet.get(setIndex).get(realPosition);
            h.rank.setText(String.valueOf(realPosition+1)); // 순위 설정
            if(d instanceof String){
                h.content.setText((String)d);
            }else if(d instanceof Title){
                h.content.setText(((Title) d).getName()); // 제목 설정
            }
            h.card.setOnClickListener(view -> {
                listener.clickedTitle((Title)d); // 제목 클릭 시 리스너 호출
            });
        }
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    // 헤더 위치를 새로고침합니다.
    public void refreshHeaders(){
        headers[0] = 0;
        for(int i=1; i<dataSet.size(); i++){
            headers[i]=headers[i-1]+dataSet.get(i-1).size()+1;
        }
    }

    @Override
    public int getItemViewType(int position) {
        // 첫 번째 헤더
        if(position == 0)
            return HEADER;
        refreshHeaders();
        // 마지막 아이템
        if(position>headers[headers.length-1]){
            return headers.length+24-1;
        }
        // 중간 아이템
        for(int i=1; i<headers.length; i++){
            if(position == headers[i])
                return HEADER;
            else if(position>headers[i-1] && position<headers[i])
                return i+23;
        }

        return -1;
    }

    // 위젯을 업데이트하고 데이터 변경을 알립니다.
    public void updateWidgets(){
        refreshHeaders();
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        int sum = headers.length;
        for(Ranking<?> r : dataSet){
            sum+= r.size();
        }
        return sum;
    }

    // 아이템 뷰홀더 클래스
    class ItemHolder extends RecyclerView.ViewHolder{
        TextView rank; // 순위 텍스트뷰
        TextView content; // 내용 텍스트뷰
        CardView card; // 카드뷰
        public ItemHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.ranking_card);
            rank = itemView.findViewById(R.id.ranking_rank);
            content = itemView.findViewById(R.id.header_title);
            if(dark)
                card.setBackgroundColor(ContextCompat.getColor(context, R.color.colorDarkBackground));
        }
    }

    // 헤더 뷰홀더 클래스
    static class HeaderHolder extends RecyclerView.ViewHolder{
        TextView title; // 헤더 제목 텍스트뷰
        public HeaderHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.header_title);
        }
    }

    // 데이터를 비동기적으로 가져오는 AsyncTask
    private class Fetcher extends AsyncTask<Void, Integer, MainPageWebtoon> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected MainPageWebtoon doInBackground(Void... params) {
            return new MainPageWebtoon(httpClient);
        }

        @Override
        protected void onPostExecute(MainPageWebtoon main) {
            super.onPostExecute(main);
            dataSet = main.getDataSet();
            if(dataSet == null)
                dataSet = main.getBlankDataSet();
            for (Ranking<?> r : dataSet) {
                if (r==null || r.size() == 0) {
                    // 캡차 또는 로딩 실패 시 콜백 호출
                    listener.captchaCallback();
                    return;
                }
            }
            updateWidgets(); // 위젯 업데이트
        }
    }
}


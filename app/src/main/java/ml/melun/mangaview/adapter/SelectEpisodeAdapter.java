package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;


import java.util.Arrays;
import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;

import static ml.melun.mangaview.MainApplication.p;

// 에피소드 선택 목록을 표시하는 RecyclerView 어댑터
public class SelectEpisodeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Manga> data; // 에피소드 데이터 리스트
    private final LayoutInflater mInflater;
    private final Context mainContext;
    boolean favorite = false; // (사용되지 않는 것으로 보임)
    TypedValue outValue;
    boolean[] selected; // 각 에피소드의 선택 여부
    ItemClickListener mClickListener; // 아이템 클릭 리스너
    boolean dark; // 다크 테마 여부
    boolean single = true; // 단일 선택 모드 여부 (false면 범위 선택)
    int rs = -1, re = -1; // 범위 선택 시작/끝 인덱스

    // 생성자
    public SelectEpisodeAdapter(Context context, List<Manga> list) {
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.data = list;
        outValue = new TypedValue();
        selected = new boolean[list.size()];
        Arrays.fill(selected,Boolean.FALSE); // 모든 에피소드를 선택되지 않은 상태로 초기화
        dark = p.getDarkTheme();
        mainContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // 뷰홀더 생성
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_episode, parent, false);
        return new ViewHolder(view);
    }

    // 뷰홀더에 데이터 바인딩
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder h = (ViewHolder) holder;
        try {
            Manga m = data.get(position);
            h.episode.setText(m.getName()); // 에피소드 이름 설정
            h.date.setText(m.getDate()); // 날짜 설정
            if (selected[position]) { // 선택된 에피소드 배경색 변경
                if(dark) h.itemView.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.selectedDark));
                else h.itemView.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.selected));
            } else {
                h.itemView.setBackgroundColor(Color.TRANSPARENT); // 선택되지 않은 에피소드 배경색 투명
            }

            // 범위 선택 중인 에피소드 배경색 변경
            if(position == rs || position == re){
                h.itemView.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.rangeSelected));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // 총 아이템 개수 반환
    @Override
    public int getItemCount() {
        return data.size();
    }

    // 에피소드를 선택/해제합니다.
    public void select(int position){
        if(single) { // 단일 선택 모드
            selected[position] = !selected[position]; // 선택 상태 토글
            notifyItemChanged(position); // 해당 아이템 갱신
        }else{ // 범위 선택 모드
            // 범위 시작점 설정
            if(rs == -1 && re == -1){
                rs = position;
            }
            // 선택된 위치가 범위 시작점과 같으면 범위 해제
            else if(position == rs){
                rs = -1;
                re = -1;
            }
            // 범위 시작점은 설정되었고 끝점은 설정되지 않았을 때
            else if(rs != -1 && re == -1){
                re = position;
                // 시작점부터 끝점까지 모든 에피소드 선택 상태 토글
                while(rs != re){
                    selected[rs] = !selected[rs];
                    notifyItemChanged(rs);

                    if(rs>re) rs--;
                    else rs++;
                }
                selected[rs] = !selected[rs];
                notifyItemChanged(rs);

                rs = -1;
                re = -1;
            }
            notifyItemChanged(rs);
            notifyItemChanged(re);
            notifyItemChanged(position);
        }
    }

    // 선택 모드를 설정합니다 (단일 선택 또는 범위 선택).
    public void setSelectionMode(boolean single){
        this.single = single;
        int tmps = rs;
        int tmpe = re;
        rs = -1;
        re = -1;
        notifyItemChanged(tmps);
        notifyItemChanged(tmpe);
    }

    // 뷰홀더 클래스
    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView episode, date; // 에피소드 이름, 날짜 텍스트뷰
        ViewHolder(View itemView) {
            super(itemView);
            episode = itemView.findViewById(R.id.episode);
            date = itemView.findViewById(R.id.date);
            // 테마에 따라 텍스트 색상 설정
            if(dark){
                date.setTextColor(Color.WHITE);
                episode.setTextColor(Color.WHITE);
            }
            else{
                date.setTextColor(Color.BLACK);
                episode.setTextColor(Color.BLACK);
            }
            // 아이템 클릭 리스너 설정
            itemView.setOnClickListener(v -> mClickListener.onItemClick(v,getAdapterPosition()));
        }
    }

    // 클릭 리스너를 설정합니다.
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // 선택된 에피소드들의 인덱스를 JSONArray로 반환합니다.
    public JSONArray getSelected(boolean all){
        JSONArray tmp = new JSONArray();
        for(int i=0; i<selected.length;i++){
            if(selected[i]) tmp.put(i);
            else if(all) tmp.put(i);
        }
        return tmp;
    }

    // 아이템 클릭 이벤트를 위한 인터페이스
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}


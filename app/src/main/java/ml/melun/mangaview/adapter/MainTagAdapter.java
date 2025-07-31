package ml.melun.mangaview.adapter;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import java.util.Arrays;
import java.util.List;

import ml.melun.mangaview.R;

import static ml.melun.mangaview.MainApplication.p;

// 메인 화면에서 태그, 작가, 발행 구분 등을 표시하고 선택하는 RecyclerView 어댑터
public class MainTagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    Context mcontext; // 컨텍스트
    List<String> tags; // 표시할 태그(또는 이름, 발행 구분) 목록
    boolean[] selected; // 각 아이템의 선택 여부
    LayoutInflater mInflater; // 레이아웃 인플레이터
    private tagOnclick mClickListener; // 아이템 클릭 리스너
    int type; // 어댑터의 종류 (0: 태그, 1: 이름, 2: 발행 구분)
    boolean dark; // 다크 테마 여부
    boolean singleSelect = false; // 단일 선택 모드 여부
    int selection = -1; // 단일 선택 모드에서 선택된 아이템의 인덱스

    // 생성자
    public MainTagAdapter(Context m, List<String> t , int type) {
        mcontext = m;
        tags = t;
        this.type = type;
        this.mInflater = LayoutInflater.from(m);
        dark = p.getDarkTheme();
        selected = new boolean[t.size()];
        Arrays.fill(selected,Boolean.FALSE); // 모든 아이템을 선택되지 않은 상태로 초기화
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // 단일 선택 모드 설정
    public void setSingleSelect(boolean b){
        singleSelect = b;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;
        switch (type){
            case 0: // 태그
                view = mInflater.inflate(R.layout.item_main_tag, parent, false);
                break;
            case 1: // 이름 (초성)
                view = mInflater.inflate(R.layout.item_main_name, parent, false);
                break;
            case 2: // 발행 구분
                view = mInflater.inflate(R.layout.item_main_tag, parent, false);
                break;
        }
        return new tagHolder(view);
    }

    // 특정 위치의 아이템 선택 상태를 토글합니다.
    public void toggleSelect(int position){
        if(singleSelect){ // 단일 선택 모드일 경우
            if(position == selection) selection = -1; // 이미 선택된 아이템을 다시 클릭하면 해제
            else{
                if(selection>-1){ // 이전에 선택된 아이템이 있으면 해제하고 새로 선택
                    int tmp = selection;
                    selection = position;
                    notifyItemChanged(tmp);
                }else{
                    selection = position;
                }
                notifyItemChanged(position);
            }
        }


        selected[position] = !selected[position]; // 선택 상태 토글
        notifyItemChanged(position); // 해당 아이템 갱신
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        tagHolder h = (tagHolder) holder;
        h.tag.setText(tags.get(position)); // 텍스트 설정
        if(singleSelect){ // 단일 선택 모드일 경우
            if(selection==position){ // 선택된 아이템의 배경색 변경
                if (dark)
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.selectedDark));
                else
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.selected));
            }else{ // 선택되지 않은 아이템의 배경색 변경
                if (dark)
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.colorDarkBackground));
                else
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.colorBackground));
            }
        }else { // 다중 선택 모드일 경우
            if (selected[position]) { // 선택된 아이템의 배경색 변경
                if (dark)
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.selectedDark));
                else
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.selected));
            } else { // 선택되지 않은 아이템의 배경색 변경
                if (dark)
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.colorDarkBackground));
                else
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.colorBackground));
            }
        }
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    // 클릭 리스너를 설정합니다.
    public void setClickListener(tagOnclick t){
        this.mClickListener = t;
    }

    // 뷰홀더 클래스
    class tagHolder extends RecyclerView.ViewHolder{
        TextView tag; // 태그 텍스트뷰
        CardView card; // 카드뷰
        public tagHolder(View itemView) {
            super(itemView);
            switch (type){
                case 0:
                case 2:
                    card = itemView.findViewById(R.id.mainTagCard);
                    tag = itemView.findViewById(R.id.main_tag_text);
                    break;
                case 1:
                    card = itemView.findViewById(R.id.mainNameCard);
                    tag = itemView.findViewById(R.id.main_name_text);
                    break;
            }


            card.setOnClickListener(v -> mClickListener.onClick(getAdapterPosition(), tags.get(getAdapterPosition())));
        }
    }

    // 선택된 태그(값)들을 콤마로 구분된 문자열로 반환합니다.
    public String getSelectedValues(){
        StringBuilder res = new StringBuilder();
        for(int i=0; i<tags.size(); i++){
            if(selected[i]){
                if(res.length()>0) res.append(',').append(tags.get(i));
                else res = new StringBuilder(tags.get(i));
            }
        }
        return res.toString();
    }

    // 선택된 태그(인덱스)들을 콤마로 구분된 문자열로 반환합니다.
    public String getSelectedIndex(){
        StringBuilder res = new StringBuilder();
        if(singleSelect) {
            if(selection>-1) return Integer.toString(type==2 ? selection+1 : selection);
            else return "";
        }

        for(int i=0; i<tags.size(); i++){
            if(selected[i]){
                if(res.length()>0) res.append(",").append(type == 2 ? i + 1 : i);
                else res = new StringBuilder(Integer.toString(type == 2 ? i + 1 : i));
            }
        }
        return res.toString();
    }

    // 태그 클릭 이벤트를 위한 인터페이스
    public interface tagOnclick{
        void onClick(int position, String value);
    }
}



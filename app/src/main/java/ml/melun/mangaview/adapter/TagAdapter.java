package ml.melun.mangaview.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ml.melun.mangaview.R;
import static ml.melun.mangaview.MainApplication.p;

// 태그 목록을 표시하는 RecyclerView 어댑터
public class TagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    Context mcontext; // 컨텍스트
    List<String> tags; // 표시할 태그 목록
    LayoutInflater mInflater; // 레이아웃 인플레이터
    private tagOnclick mClickListener; // 태그 클릭 리스너
    boolean dark; // 다크 테마 여부

    // 생성자
    public TagAdapter(Context m, List<String> t) {
        mcontext = m;
        tags = t;
        this.mInflater = LayoutInflater.from(m);
        dark = p.getDarkTheme();
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_tag, parent, false);
        return new tagHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        tagHolder h = (tagHolder) holder;
        h.tag.setText(tags.get(position)); // 태그 텍스트 설정
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    // 클릭 리스너를 설정합니다.
    public void setClickListener(tagOnclick t){
        this.mClickListener = t;
    }

    // ViewHolder 클래스
    class tagHolder extends RecyclerView.ViewHolder{
        TextView tag; // 태그 텍스트뷰
        CardView card; // 태그를 감싸는 카드뷰
        public tagHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.tagCard);
            tag = itemView.findViewById(R.id.tag);
            // 카드뷰 클릭 시 리스너 호출
            card.setOnClickListener(v -> mClickListener.onClick(tags.get(getAdapterPosition())));
        }
    }

    // 태그 클릭 이벤트를 위한 인터페이스
    public interface tagOnclick{
        void onClick(String tag);
    }
}



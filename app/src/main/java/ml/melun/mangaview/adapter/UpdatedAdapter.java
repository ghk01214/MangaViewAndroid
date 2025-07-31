package ml.melun.mangaview.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.mangaview.UpdatedManga;

import static ml.melun.mangaview.MainApplication.p;

// 업데이트된 만화 목록을 표시하는 RecyclerView 어댑터
public class UpdatedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    ArrayList<UpdatedManga> mData; // 표시할 데이터 리스트
    onclickListener olisten; // 클릭 리스너
    boolean save; // 데이터 절약 모드 여부
    boolean dark; // 다크 테마 여부
    private final LayoutInflater mInflater;

    public UpdatedAdapter(Context main) {
        super();
        context = main;
        mData = new ArrayList<>();
        save = p.getDataSave();
        dark = p.getDarkTheme();
        this.mInflater = LayoutInflater.from(main);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // 데이터를 추가하고 RecyclerView에 알립니다.
    public void addData(ArrayList<UpdatedManga> data){
        int oSize = mData.size();
        mData.addAll(data);
        notifyItemRangeInserted(oSize,data.size());
    }

    public void setOnClickListener(onclickListener click){
        olisten = click;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_updated_list, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        viewHolder h = (viewHolder) holder;
        UpdatedManga m = mData.get(position);
        h.text.setText(m.getName());
        h.date.setText(m.getDate());

        // 데이터 절약 모드가 아닐 때만 썸네일 로드
        if(m.getThumb().length()>1 && !save) {
            Glide.with(h.thumb)
                .load(ml.melun.mangaview.Utils.getGlideUrl(m.getThumb()))
                .error(R.mipmap.ic_launcher)
                .into(h.thumb);
        } else h.thumb.setImageBitmap(null);
        if(save) h.thumb.setVisibility(View.GONE);

        // 봤던 작품 표시
        if(p.getBookmark(m.getTitle())>0)
            h.seen.setVisibility(View.VISIBLE);
        else
            h.seen.setVisibility(View.GONE);

        // 즐겨찾기 작품 표시
        if(p.findFavorite(m.getTitle())>-1)
            h.fav.setVisibility(View.VISIBLE);
        else
            h.fav.setVisibility(View.GONE);

        // 태그 목록 표시
        StringBuilder tags = new StringBuilder();
        for (String s :m.getTag()) {
            tags.append(s).append(" ");
        }
        h.tags.setText(tags.toString());
        h.author.setText(m.getAuthor());
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    // ViewHolder 클래스
    class viewHolder extends RecyclerView.ViewHolder{
        TextView text, date, author, tags;
        ImageView thumb, seen, fav;
        CardView card;
        ImageButton viewEps;
        View tagContainer;

        public viewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.Title);
            date = itemView.findViewById(R.id.date);
            card = itemView.findViewById(R.id.updatedCard);
            thumb = itemView.findViewById(R.id.Thumb);
            viewEps = itemView.findViewById(R.id.epsButton);
            seen = itemView.findViewById(R.id.seenIcon);
            fav = itemView.findViewById(R.id.favIcon);
            author =itemView.findViewById(R.id.TitleAuthor);
            tags = itemView.findViewById(R.id.TitleTag);
            tagContainer = itemView.findViewById(R.id.TitleTagContainer);

            // 다크 테마 적용
            if(dark){
                card.setBackgroundColor(ContextCompat.getColor(context, R.color.colorDarkBackground));
                viewEps.setBackgroundColor(ContextCompat.getColor(context, R.color.resumeDark));
            }

            // 클릭 리스너 설정
            card.setOnClickListener(v -> olisten.onClick(mData.get(getAdapterPosition())));
            viewEps.setOnClickListener(v -> olisten.onEpsClick(mData.get(getAdapterPosition()).getTitle()));
        }
    }

    // 클릭 리스너 인터페이스
    public interface onclickListener {
        void onClick(Manga m); // 아이템 클릭 (뷰어로 바로 이동)
        void onEpsClick(Title t); // '회차보기' 버튼 클릭 (에피소드 목록으로 이동)
    }
}
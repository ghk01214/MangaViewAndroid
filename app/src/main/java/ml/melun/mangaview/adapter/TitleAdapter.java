package ml.melun.mangaview.adapter;
import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.MTitle;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.p;

// 만화 작품 목록을 표시하는 RecyclerView 어댑터 (검색 필터링 기능 포함)
public class TitleAdapter extends RecyclerView.Adapter<TitleAdapter.ViewHolder> implements Filterable {

    private ArrayList<Title> mData; // 원본 데이터 리스트
    private ArrayList<Title> mDataFiltered; // 필터링된 데이터 리스트
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener; // 아이템 클릭 리스너
    private Context mainContext;
    boolean dark = false; // 다크 테마 여부
    boolean save; // 데이터 절약 모드 여부
    boolean resume = true; // 이어보기 버튼 표시 여부
    boolean updated = false; // (사용되지 않는 것으로 보임)
    boolean forceThumbnail = false; // 썸네일 강제 표시 여부 (데이터 절약 모드에서도)
    String path = ""; // (사용되지 않는 것으로 보임)
    Filter filter; // 검색 필터
    boolean searching = false; // 현재 검색 중인지 여부

    public TitleAdapter(Context context) {
        init(context);
    }
    public TitleAdapter(Context context, boolean online) {
        init(context);
        forceThumbnail = !online;
    }

    // 썸네일 강제 표시 여부를 설정합니다.
    public void setForceThumbnail(boolean b){
        this.forceThumbnail = b;
    }

    // 어댑터 초기화
    void init(Context context){
        p = new Preference(context);
        dark = p.getDarkTheme();
        save = p.getDataSave();
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.mData = new ArrayList<>();
        this.mDataFiltered = new ArrayList<>();
        // 검색 필터 구현
        filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String query = charSequence.toString();
                if(query.isEmpty() || query.length() == 0){ // 검색어가 없으면 전체 데이터 표시
                    mDataFiltered = mData;
                    searching = false;
                }else{ // 검색어가 있으면 필터링
                    searching = true;
                    ArrayList<Title> filtered = new ArrayList<>();
                    for(Title t : mData){
                        // 제목 또는 작가에 검색어가 포함되면 추가
                        if(t.getName().toLowerCase().contains(query.toLowerCase()) || t.getAuthor().toLowerCase().contains(query.toLowerCase()))
                            filtered.add(t);
                    }
                    mDataFiltered = filtered;
                }
                FilterResults res = new FilterResults();
                res.values = mDataFiltered;
                return res;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mDataFiltered = (ArrayList<Title>) filterResults.values;
                notifyDataSetChanged(); // 데이터 변경 알림
            }
        };
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    // 모든 데이터를 제거합니다.
    public void removeAll(){
        int originSize = mData.size();
        mData.clear();
        mDataFiltered.clear();
        notifyItemRangeRemoved(0,originSize);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_title, parent, false);
        return new ViewHolder(view);
    }

    // 데이터를 추가합니다. Title 또는 MTitle 객체를 받을 수 있습니다.
    public void addData(List<?> t){
        int oSize = mData.size();
        for(Object d:t){
            if(d instanceof Title){
                ((Title) d).setBookmark(p.getBookmark((Title)d));
                mData.add((Title)d);
            } else if(d instanceof MTitle){
                Title d2 = new Title((MTitle)d);
                d2.setBookmark(p.getBookmark((MTitle) d));
                mData.add(d2);
            }
        }
        mDataFiltered = mData; // 필터링된 데이터도 업데이트
        notifyItemRangeInserted(oSize,t.size()); // 추가된 범위 알림
    }

    // 데이터를 새로 설정합니다. 기존 데이터는 모두 제거됩니다.
    public void setData(List<?> t){
        clearData();
        addData(t);
    }

    // 모든 데이터를 지우고 RecyclerView를 새로고침합니다.
    public void clearData(){
        mData.clear();
        mDataFiltered.clear();
        notifyDataSetChanged();
    }


    // 특정 아이템을 목록의 맨 위로 이동시킵니다.
    public void moveItemToTop(int from){
        if(!searching) { // 검색 중이 아닐 때
            mData.add(0, mData.get(from));
            mData.remove(from + 1);
            for (int i = from; i > 0; i--) {
                notifyItemMoved(i, i - 1);
            }
        }else{ // 검색 중일 때
            Title t = mDataFiltered.get(from);
            int index = mData.indexOf(t);
            mData.add(0, mData.get(index));
            mData.remove(index + 1);
        }
    }

    // 특정 위치의 아이템을 제거합니다.
    public void remove(int pos){
        if(!searching) { // 검색 중이 아닐 때
            mData.remove(pos);
            notifyItemRemoved(pos);
        }else{ // 검색 중일 때
            Title t = mDataFiltered.get(pos);
            int index = mData.indexOf(t);
            mData.remove(index);
            mDataFiltered.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Title data = mDataFiltered.get(position);
        String title = data.getName();
        String thumb = data.getThumb();
        String author = data.getAuthor();
        StringBuilder tags = new StringBuilder();
        int bookmark = data.getBookmark();

        holder.tagContainer.setVisibility(View.VISIBLE);
        holder.baseModeStr.setText(data.getBaseModeStr()); // 만화/웹툰 구분 표시

        // 태그 표시
        for (String s : data.getTags()) {
            tags.append(s).append(" ");
        }
        holder.tags.setText(tags.toString());

        holder.name.setText(title);
        holder.author.setText(author);

        // 추천 수 등 카운터 정보 표시
        if(data.hasCounter()){
            holder.counterContainer.setVisibility(View.VISIBLE);
            holder.recommend_c.setText(String.valueOf(data.getRecommend_c()));
        }else{
            holder.counterContainer.setVisibility(View.GONE);
        }

        // 썸네일 이미지 로드 (데이터 절약 모드 및 강제 표시 여부에 따라)
        if(thumb.length()>1 && (!save || forceThumbnail)) Glide.with(holder.thumb).load(thumb).into(holder.thumb);
        else holder.thumb.setImageBitmap(null);
        if(save && !forceThumbnail) holder.thumb.setVisibility(View.GONE);

        // 이어보기 버튼 표시 (북마크가 있고 이어보기 모드일 때)
        if(bookmark>0 && resume) holder.resume.setVisibility(View.VISIBLE);
        else holder.resume.setVisibility(View.GONE);

    }

    @Override
    public int getItemCount() {
        if(mDataFiltered != null)
            return mDataFiltered.size();
        return 0;
    }

    // ViewHolder 클래스
    class ViewHolder extends RecyclerView.ViewHolder{
        TextView name; // 제목
        ImageView thumb, fav; // 썸네일, 즐겨찾기 아이콘 (fav는 사용되지 않는 것으로 보임)
        TextView author; // 작가
        TextView tags; // 태그
        TextView recommend_c, battery_c, comment_c, bookmark_c; // 추천 수, 배터리, 댓글, 북마크 (배터리, 댓글, 북마크는 사용되지 않는 것으로 보임)
        TextView baseModeStr; // 만화/웹툰 구분
        ImageButton resume; // 이어보기 버튼
        CardView card; // 아이템 카드뷰

        View tagContainer; // 태그 컨테이너
        View counterContainer; // 카운터 컨테이너

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.Title);
            thumb = itemView.findViewById(R.id.Thumb);
            author =itemView.findViewById(R.id.TitleAuthor);
            tags = itemView.findViewById(R.id.TitleTag);
            card = itemView.findViewById(R.id.titleCard);
            resume = itemView.findViewById(R.id.epsButton);
            recommend_c = itemView.findViewById(R.id.TitleRecommend_c);
            battery_c = itemView.findViewById(R.id.TitleBattery_c);
            comment_c = itemView.findViewById(R.id.TitleComment_c);
            bookmark_c = itemView.findViewById(R.id.TitleBookmark_c);
            baseModeStr = itemView.findViewById(R.id.TitleBaseMode);

            tagContainer = itemView.findViewById(R.id.TitleTagContainer);
            counterContainer = itemView.findViewById(R.id.TitleCounterContainer);

            // 다크 테마 적용
            if(dark){
                card.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
                resume.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.resumeDark));
            }
            // 클릭 리스너 설정
            card.setOnClickListener(v -> mClickListener.onItemClick(getAdapterPosition()));
            card.setOnLongClickListener(v -> {
                mClickListener.onLongClick(v, getAdapterPosition());
                return true;
            });
            resume.setOnClickListener(v -> mClickListener.onResumeClick(getAdapterPosition(), p.getBookmark(mDataFiltered.get(getAdapterPosition()))));
        }
    }

    // 이어보기 버튼 표시 여부를 설정합니다.
    public void setResume(boolean resume){
        this.resume = resume;
    }

    // 특정 위치의 아이템을 반환합니다.
    public Title getItem(int index) {
        return mDataFiltered.get(index);
    }

    // 클릭 리스너를 설정합니다.
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // 아이템 클릭 이벤트를 위한 인터페이스
    public interface ItemClickListener {
        void onItemClick(int position); // 아이템 클릭 시
        void onLongClick(View view, int position); // 아이템 롱클릭 시
        void onResumeClick(int position, int id); // 이어보기 버튼 클릭 시
    }

    // 필터 객체를 반환합니다.
    @Override
    public Filter getFilter() {
        return filter;
    }
}
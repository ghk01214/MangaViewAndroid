package ml.melun.mangaview.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.content.res.Resources;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.mangaview.MTitle.base_auto;

// 메인 화면의 최근 업데이트된 만화 목록을 표시하는 RecyclerView 어댑터
public class MainUpdatedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    List<Manga> mData; // 표시할 만화 데이터 리스트
    Context context;
    LayoutInflater mInflater;
    boolean loaded = false; // 데이터 로드 완료 여부
    OnClickCallback monclick; // 클릭 콜백 인터페이스
    boolean dark, save; // 다크 테마 여부, 데이터 절약 모드 여부
    Resources res;

    public MainUpdatedAdapter(Context c) {
        context = c;
        this.mInflater = LayoutInflater.from(c);
        dark = p.getDarkTheme();
        save = p.getDataSave();
        this.res = context.getResources();

        setHasStableIds(true);
    }

    // 로딩 상태를 표시합니다.
    public void setLoad(){
        setLoad("로드중...");
    }

    // 로딩 메시지와 함께 로딩 상태를 표시합니다.
    public void setLoad(String msg){
        if(mData != null){
            int size = mData.size();
            mData.clear();
            loaded = false;
            notifyItemRangeRemoved(0,size);
        }
        else
            mData = new ArrayList<>();
        Manga loading = new Manga(0,msg,"", base_auto);
        loading.addThumb("");
        mData.add(loading);
        notifyItemInserted(0);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_updated, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        viewHolder h = (viewHolder) holder;
        h.title.setText(mData.get(position).getName());
        String thumb = mData.get(position).getThumb();
        h.thumb.setColorFilter(null);
        // 썸네일 이미지 설정
        if(thumb != null && thumb.length()==0)
            h.thumb.setImageResource(android.R.color.transparent);
        else if(thumb != null && thumb.equals("reload")) { // 새로고침 아이콘 표시
            h.thumb.setImageDrawable(ResourcesCompat.getDrawable(res, R.drawable.ic_refresh, null));
            h.thumb.setColorFilter(dark ? Color.WHITE : Color.DKGRAY);
        }else if(save) // 데이터 절약 모드일 경우 기본 아이콘 표시
            h.thumb.setImageDrawable(ResourcesCompat.getDrawable(res, R.mipmap.ic_launcher, null));
        else // 일반적인 경우 Glide로 썸네일 로드
            Glide.with(h.thumb).load(thumb).into(h.thumb);
    }


    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    // 클릭 콜백 인터페이스
    public interface OnClickCallback {
        void onclick(Manga m); // 만화 클릭 시
        void refresh(); // 새로고침 요청 시
    }

    // 뷰홀더 클래스
    class viewHolder extends RecyclerView.ViewHolder{
        ImageView thumb; // 썸네일 이미지뷰
        TextView title; // 제목 텍스트뷰
        CardView card; // 카드뷰
        public viewHolder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.main_new_thumb);
            title = itemView.findViewById(R.id.main_new_name);
            title.setEllipsize(TextUtils.TruncateAt.MARQUEE); // 마르퀴 효과
            title.setMarqueeRepeatLimit(-1);
            title.setSingleLine(true);
            title.setSelected(true);
            card = itemView.findViewById(R.id.updatedCard);
            card.setOnClickListener(v -> {
                if(loaded){ // 데이터 로드 완료 시 클릭 이벤트 처리
                    monclick.onclick(mData.get(getAdapterPosition()));
                }else // 로드 중일 때 새로고침 요청
                    monclick.refresh();
            });
            // 다크 테마 적용
            if(dark){
                card.setBackgroundColor(ContextCompat.getColor(context, R.color.colorDarkBackground));
            }

        }
    }

    // 클릭 리스너를 설정합니다.
    public void setClickListener(OnClickCallback o){
        this.monclick = o;
    }

    // 데이터를 설정하고 어댑터를 업데이트합니다.
    public void setData(List<Manga> data){
        mData = data;
        if(mData.size()==0){ // 결과가 없을 경우
            Manga none = new Manga(0,"결과 없음","", base_auto);
            none.addThumb("reload"); // 새로고침 아이콘 표시
            mData.add(none);
            notifyItemChanged(0);
            loaded = false;
        }else {
            notifyItemChanged(0);
            notifyItemRangeInserted(1, mData.size() - 1);
            loaded = true;
        }

    }
}


package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.getGlideUrl;

import ml.melun.mangaview.R;
import ml.melun.mangaview.activity.ViewerActivity;
import ml.melun.mangaview.interfaces.StringCallback;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.model.PageItem;


// 스트립(세로 스크롤) 뷰어의 페이지를 관리하는 RecyclerView 어댑터
public class StripAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final LayoutInflater mInflater;
    private final Context mainContext;
    private StripAdapter.ItemClickListener mClickListener; // 아이템 클릭 리스너
    boolean autoCut; // 자동 자르기 모드 여부
    boolean reverse; // 역순 읽기 모드 여부
    int __seed; // 디코더 시드
    Decoder d; // 이미지 디코더
    int width; // 화면 너비
    int count = 0; // 현재 로드된 만화(Manga) 객체의 수
    final static int MaxStackSize = 2; // 최대 스택 크기 (이전/다음 만화 로드 시 사용)
    ViewerActivity.InfiniteScrollCallback callback; // 무한 스크롤 콜백
    Title title; // 현재 보고 있는 작품의 Title 객체

    List<Object> items; // 표시할 아이템 목록 (PageItem 또는 InfoItem)

    public List<Object> getItems(){
        return items;
    }

    // 정보 아이템 (이전/다음 에피소드 정보 표시용)
    public static class InfoItem{
        public InfoItem(Manga prev, Manga next) {
            // 이전/다음 에피소드 정보를 설정합니다.
            if(next == null)
                this.next = prev.nextEp();
            else
                this.next = next;
            if(prev == null)
                this.prev = next.prevEp();
            else
                this.prev = prev;
        }

        @Override
        public int hashCode() {
            // 해시코드 생성 (이전/다음 에피소드 ID 기반)
            return (next==null?1:next.getId()) * (prev==null?1:prev.getId());
        }

        public Manga next; // 다음 에피소드
        public Manga prev; // 이전 에피소드
    }

    @Override
    public long getItemId(int position) {
        Object o = items.get(position);
        return o.hashCode();
    }

    // 현재 만화 뒤에 새로운 만화(에피소드)를 추가합니다.
    public void appendManga(Manga m){
        if(items == null)
            items = new ArrayList<>();
        int prevsize = items.size();
        if(items.size() == 0)
            items.add(new InfoItem(m.prevEp(), m)); // 첫 만화일 경우 이전 에피소드 정보 추가
        List<String> imgs = m.getImgs(mainContext);
        for(int i=0; i<imgs.size(); i++){
            items.add(new PageItem(i,imgs.get(i),m)); // 페이지 아이템 추가
            if(autoCut)
                items.add(new PageItem(i,imgs.get(i),m,PageItem.SECOND)); // 자동 자르기 모드일 경우 두 번째 부분 추가
        }
        items.add(new InfoItem(m, m.nextEp())); // 다음 에피소드 정보 추가
        notifyItemRangeInserted(prevsize, items.size()-prevsize); // 추가된 범위 알림
        count++;
        if(count>MaxStackSize){
            popFirst(); // 최대 스택 크기를 초과하면 첫 번째 만화 제거
        }
    }

    // 현재 만화 앞에 새로운 만화(에피소드)를 삽입합니다.
    public void insertManga(Manga m){
        if(items == null || items.size() == 0) {
            appendManga(m);
            return;
        }
        int prevsize = items.size();
        List<String> imgs = m.getImgs(mainContext);
        for(int i=imgs.size()-1; i>=0; i--){
            if(autoCut)
                items.add(0,new PageItem(0,imgs.get(i),m,PageItem.SECOND)); // 자동 자르기 모드일 경우 두 번째 부분 삽입
            items.add(0,new PageItem(i,imgs.get(i),m)); // 페이지 아이템 삽입
        }
        items.add(0, new InfoItem(null, m)); // 이전 에피소드 정보 삽입

        notifyItemRangeInserted(0, items.size()-prevsize); // 삽입된 범위 알림
        count++;

        if(count>MaxStackSize){
            popLast(); // 최대 스택 크기를 초과하면 마지막 만화 제거
        }
    }

    // 첫 번째 만화(에피소드)를 제거합니다.
    public void popFirst(){
        int size = 0;
        for(int i=1; i<items.size(); i++){
            if(items.get(i) instanceof InfoItem){
                size = i;
                break;
            }
        }
        if (size > 0) {
            items.subList(0, size).clear();
        }
        count--;
        notifyItemRangeRemoved(0,size);
    }

    // 마지막 만화(에피소드)를 제거합니다.
    public void popLast(){
        int rsize = 0;
        items.remove(items.size()-1);
        for(int i=items.size()-2; i>=0; i--){
            if(items.get(i) instanceof InfoItem){
                rsize = i;
                break;
            }
        }
        if (items.size() > rsize + 1) {
            items.subList(rsize + 1, items.size()).clear();
        }
        count--;
        notifyItemRangeRemoved(rsize+1,items.size()-rsize);
    }

    // 생성자
    public StripAdapter(Context context, Manga manga, Boolean cut, int width, Title title, ViewerActivity.InfiniteScrollCallback callback) {
        autoCut = cut;
        this.callback = callback;
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        reverse = p.getReverse();
        __seed = manga.getSeed();
        d = new Decoder(manga.getSeed(), manga.getId());
        this.width = width;
        this.title = title;
        setHasStableIds(true);
        appendManga(manga); // 초기 만화 추가
    }

    // 모든 이미지를 미리 로드합니다.
    public void preloadAll(){
        for(Object o : items) {
            if(o instanceof PageItem) {
                Object url = ((PageItem) o).manga.isOnline() ? getGlideUrl(((PageItem)o).img) : ((PageItem)o).img;
                Glide.with(mainContext)
                        .load(url)
                        .preload();
            }
        }
    }

    final static int IMG = 0; // 이미지 뷰 타입
    final static int INFO = 1; // 정보 뷰 타입

    @Override
    public int getItemViewType(int position) {
        if(items.get(position) instanceof PageItem)
            return IMG;
        else if(items.get(position) instanceof InfoItem)
            return INFO;
        else
            return -1;
    }

    // 모든 아이템을 제거합니다.
    public void removeAll(){
        int size = items.size();
        items.clear();
        notifyItemRangeRemoved(0, size);
    }

    @Override
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == IMG) {
            View view = mInflater.inflate(R.layout.item_strip, parent, false);
            return new ImgViewHolder(view);
        }else{
            //INFO
            View view = mInflater.inflate(R.layout.item_strip_info, parent, false);
            return new InfoViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int pos) {
        int type = getItemViewType(pos);
        if(type == IMG) {
            ((ImgViewHolder)holder).frame.setImageResource(R.drawable.placeholder);
            ((ImgViewHolder)holder).refresh.setVisibility(View.VISIBLE);
            glideBind((ImgViewHolder)holder, pos); // 이미지 로드
        }else if(type == INFO){
            // 정보 아이템 바인딩
            ((InfoViewHolder) holder).loading.setVisibility(View.INVISIBLE);
            Manga prev = ((InfoItem)items.get(pos)).prev;
            Manga next = ((InfoItem)items.get(pos)).next;

            if(prev == null){
                prev = next.prevEp();
            }else if(next == null){
                next = prev.nextEp();
            }

            ((InfoViewHolder) holder).prevInfo.setText(prev == null ? "첫 화" : prev.getName());
            ((InfoViewHolder) holder).nextInfo.setText(next == null ? "마지막 화" : next.getName());

            ViewerActivity.InfiniteLoadCallback r = new ViewerActivity.InfiniteLoadCallback() {
                @Override
                public void prevLoaded(Manga m) {
                    ((InfoViewHolder) holder).loading.setVisibility(View.INVISIBLE);
                    ((InfoViewHolder) holder).prevInfo.setText(m==null?"오류":m.getName());
                }

                @Override
                public void nextLoaded(Manga m) {
                    ((InfoViewHolder) holder).loading.setVisibility(View.INVISIBLE);
                    ((InfoViewHolder) holder).nextInfo.setText(m==null?"오류":m.getName());
                }
            };

            Manga m;
            if(pos == 0){ // 첫 번째 정보 아이템 (이전 에피소드 로드)
                ((InfoViewHolder) holder).loading.setVisibility(View.VISIBLE);
                m = callback.prevEp(r, next);
                ((InfoItem)items.get(pos)).prev = m;
                ((InfoViewHolder) holder).prevInfo.setText(m==null? "첫 화":"이전 화");
            }else if(pos == items.size()-1){ // 마지막 정보 아이템 (다음 에피소드 로드)
                ((InfoViewHolder) holder).loading.setVisibility(View.VISIBLE);
                m = callback.nextEp(r, prev);
                ((InfoItem)items.get(pos)).next = m;
                ((InfoViewHolder) holder).nextInfo.setText(m==null? "마지막 화":"다음 화");
            }
        }
    }



    // Glide를 사용하여 이미지를 로드하고 디코딩하여 ImageView에 바인딩합니다.
    void glideBind(ImgViewHolder holder, int pos){
        PageItem item = ((PageItem)items.get(pos));
        Object url = item.manga.isOnline() ? getGlideUrl(item.img) : item.img;
        if (autoCut) { // 자동 자르기 모드일 경우
            Glide.with(holder.frame)
                    .asBitmap()
                    .load(url)
                    .placeholder(R.drawable.placeholder)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, Transition<? super Bitmap> transition) {
                            bitmap = d.decode(bitmap, width); // 이미지 디코딩
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            if (width > height) { // 가로가 더 긴 이미지 (두 페이지)
                                if (item.side == PageItem.FIRST) { // 첫 번째 부분
                                    if (reverse) // 역순 읽기
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, width / 2, height));
                                    else
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, width / 2, 0, width / 2, height));
                                } else { // 두 번째 부분
                                    if (reverse)
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, width / 2, 0, width / 2, height));
                                    else
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, width / 2, height));
                                }
                            } else { // 세로가 더 긴 이미지 (단일 페이지)
                                if (item.side == PageItem.FIRST) {
                                    holder.frame.setImageBitmap(bitmap);
                                } else { // 두 번째 부분은 빈 비트맵으로 처리
                                    holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap.getWidth(), 1, Bitmap.Config.ARGB_8888));
                                }
                            }
                            holder.refresh.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            holder.frame.setImageDrawable(placeholder);
                            holder.refresh.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            holder.frame.setImageResource(R.drawable.placeholder);
                            holder.refresh.setVisibility(View.VISIBLE);
                        }
                    });
        } else { // 자동 자르기 모드가 아닐 경우
            Glide.with(holder.frame)
                    .asBitmap()
                    .load(url)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            resource = d.decode(resource, width); // 이미지 디코딩
                            holder.frame.setImageBitmap(resource);
                            holder.refresh.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            holder.frame.setImageDrawable(placeholder);
                            holder.refresh.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            holder.frame.setImageResource(R.drawable.placeholder);
                            holder.refresh.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    // 총 아이템 개수를 반환합니다.
    @Override
    public int getItemCount() {
        return items.size();
    }

    // 현재 화면에 보이는 페이지 아이템을 반환합니다.
    public PageItem getCurrentVisiblePage(){
        return current;
    }

    PageItem current; // 현재 보이는 페이지 아이템

    boolean needUpdate = true; // 정보 업데이트 필요 여부

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        // 뷰가 화면에 붙을 때 북마크 및 정보 업데이트 처리
        int layoutPos = holder.getLayoutPosition();
        int type = getItemViewType(layoutPos);
        if(type == IMG) {
            PageItem pi = (PageItem) items.get(layoutPos);
            current = pi;
            if(pi.manga.useBookmark()){ // 북마크 사용 가능한 만화일 경우
                int index = pi.index;
                if (index == 0) {
                    p.removeViewerBookmark(pi.manga); // 첫 페이지면 뷰어 북마크 제거
                } else {
                    p.setViewerBookmark(pi.manga, index); // 뷰어 북마크 설정
                }
            }
            p.setBookmark(title, pi.manga.getId()); // 작품의 북마크 설정
            if(needUpdate){
                needUpdate = false;
                callback.updateInfo(pi.manga); // 정보 업데이트 콜백 호출
            }
        } else if(type == INFO){
            needUpdate = true;
        }
    }

    // 이미지 뷰홀더 클래스
    public class ImgViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ImageView frame; // 이미지를 표시할 ImageView
        ImageButton refresh; // 새로고침 버튼
        ImgViewHolder(View itemView) {
            super(itemView);
            frame = itemView.findViewById(R.id.frame);
            refresh = itemView.findViewById(R.id.refreshButton);
            refresh.setOnClickListener(v -> {
                notifyItemChanged(getAdapterPosition()); // 이미지 새로고침
            });
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }
        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick();
        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }

    // 정보 뷰홀더 클래스
    public class InfoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView prevInfo, nextInfo; // 이전/다음 에피소드 정보 텍스트뷰
        ProgressBar loading; // 로딩 프로그레스바
        InfoViewHolder(View itemView) {
            super(itemView);
            prevInfo = itemView.findViewById(R.id.prevEpInfo);
            nextInfo = itemView.findViewById(R.id.nextEpInfo);
            loading = itemView.findViewById(R.id.infoLoading);
            itemView.setOnClickListener(this);
        }
        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick();
        }
    }

    // 클릭 리스너를 설정합니다.
    public void setClickListener(StripAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // 아이템 클릭 이벤트를 위한 인터페이스
    public interface ItemClickListener {
        void onItemClick();
    }

}



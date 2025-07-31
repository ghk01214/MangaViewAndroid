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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.mangaview.UpdatedManga;

import static ml.melun.mangaview.MainApplication.p;

// 업데이트된 만화 목록을 표시하는 RecyclerView 어댑터
public class UpdatedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    ArrayList<UpdatedManga> mData; // 표시할 데이터 리스트
    ArrayList<UpdatedManga> originalData; // 원본 데이터 (필터링 전)
    onclickListener olisten; // 클릭 리스너
    boolean save; // 데이터 절약 모드 여부
    boolean dark; // 다크 테마 여부
    private final LayoutInflater mInflater;
    Calendar selectedDate = null; // 선택된 날짜 필터 (Calendar 객체로 변경)

    public UpdatedAdapter(Context main) {
        super();
        context = main;
        mData = new ArrayList<>();
        originalData = new ArrayList<>();
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
        originalData.addAll(data);
        applyDateFilter(); // 필터 적용 후 표시
    }

    // 초기 데이터를 설정합니다.
    public void setData(ArrayList<UpdatedManga> data){
        originalData.clear();
        originalData.addAll(data);
        applyDateFilter();
    }

    public void setOnClickListener(onclickListener click){
        olisten = click;
    }

    // 특정 날짜(일 단위)로 필터링합니다.
    public void setDateFilterByDay(Calendar date) {
        this.selectedDate = date;
        applyDateFilter();
    }

    // 문자열 날짜로 직접 필터링합니다.
    public void setDateFilterByString(String dateStr) {
        this.selectedDate = null; // Calendar 필터 해제
        applyStringDateFilter(dateStr);
    }

    // 필터를 제거하고 전체 데이터를 표시합니다.
    public void clearDateFilter() {
        this.selectedDate = null;
        applyDateFilter();
    }

    // 현재 선택된 날짜를 반환합니다.
    public Calendar getSelectedDate() {
        return selectedDate;
    }

    // 데이터에서 사용 가능한 모든 날짜 목록을 반환합니다 (시간 제외, 일 기준).
    public ArrayList<String> getAvailableDates() {
        Set<String> dateSet = new HashSet<>(); // 중복 제거를 위해 Set 사용
        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        System.out.println("DEBUG: Getting available dates from " + originalData.size() + " manga entries");
        
        for (UpdatedManga manga : originalData) {
            String originalDate = manga.getDate();
            System.out.println("DEBUG: Found original date = '" + originalDate + "' for manga: " + manga.getName());
            
            if (originalDate != null) {
                // 날짜를 파싱하여 일 단위로 변환
                Calendar parsedDate = parseMangaDate(originalDate);
                if (parsedDate != null) {
                    // yyyy-MM-dd 형식으로 통일
                    String normalizedDate = displayFormat.format(parsedDate.getTime());
                    dateSet.add(normalizedDate);
                    System.out.println("DEBUG: Normalized date = '" + normalizedDate + "'");
                } else {
                    // 파싱에 실패한 경우 원본 날짜 사용
                    dateSet.add(originalDate);
                    System.out.println("DEBUG: Using original date (parse failed) = '" + originalDate + "'");
                }
            }
        }
        
        ArrayList<String> dates = new ArrayList<>(dateSet);
        Collections.sort(dates, Collections.reverseOrder()); // 최신 날짜 먼저
        
        System.out.println("DEBUG: Available normalized dates: " + dates.toString());
        return dates;
    }

    // 날짜 필터를 적용하여 데이터를 필터링합니다 (일 단위).
    private void applyDateFilter() {
        mData.clear();
        
        if (selectedDate == null) {
            // 필터가 없으면 모든 데이터 표시
            mData.addAll(originalData);
        } else {
            // 선택된 날짜(일)와 같은 날의 만화만 필터링
            for (UpdatedManga manga : originalData) {
                if (isSameDay(manga.getDate(), selectedDate)) {
                    mData.add(manga);
                }
            }
        }
        
        notifyDataSetChanged();
    }
    
    // 만화의 날짜 문자열과 선택된 날짜가 같은 일인지 확인합니다.
    private boolean isSameDay(String mangaDateStr, Calendar selectedDate) {
        if (mangaDateStr == null) return false;
        
        // 디버깅을 위한 로그 추가
        System.out.println("DEBUG: mangaDateStr = " + mangaDateStr);
        System.out.println("DEBUG: selectedDate = " + selectedDate.get(Calendar.YEAR) + "-" + 
                          (selectedDate.get(Calendar.MONTH) + 1) + "-" + 
                          selectedDate.get(Calendar.DAY_OF_MONTH));
        
        try {
            Calendar mangaDate = parseMangaDate(mangaDateStr);
            if (mangaDate == null) {
                System.out.println("DEBUG: parseMangaDate returned null for " + mangaDateStr);
                return false;
            }
            
            System.out.println("DEBUG: parsed mangaDate = " + mangaDate.get(Calendar.YEAR) + "-" + 
                              (mangaDate.get(Calendar.MONTH) + 1) + "-" + 
                              mangaDate.get(Calendar.DAY_OF_MONTH));
            
            // 년, 월, 일이 모두 같은지 확인
            boolean result = mangaDate.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                           mangaDate.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                           mangaDate.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH);
            
            System.out.println("DEBUG: isSameDay result = " + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 만화의 날짜 문자열을 Calendar 객체로 파싱합니다.
    private Calendar parseMangaDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        
        System.out.println("DEBUG: Trying to parse dateStr = '" + dateStr + "'");
        
        // 다양한 날짜 형식을 시도해봅니다
        String[] patterns = {
            "yyyy-MM-dd",
            "yyyy/MM/dd", 
            "yyyy.MM.dd",
            "MM-dd",
            "MM/dd",
            "MM.dd",
            "M-d",
            "M/d",
            "yyyy년 MM월 dd일",
            "MM월 dd일",
            "M월 d일",
            "yyyy년M월d일",
            "MMdd",
            "M.d",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd.MM.yyyy"
        };
        
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                sdf.setLenient(true); // 유연한 파싱 허용
                Date date = sdf.parse(dateStr);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                
                // MM-dd, M-d 등 년도가 없는 형식인 경우 현재 년도로 설정
                if (pattern.contains("MM-dd") || pattern.contains("MM/dd") || pattern.contains("MM.dd") ||
                    pattern.contains("M-d") || pattern.contains("M/d") || pattern.contains("M.d") ||
                    pattern.contains("MM월 dd일") || pattern.contains("M월 d일") || 
                    pattern.contains("MMdd")) {
                    calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
                }
                
                System.out.println("DEBUG: Successfully parsed with pattern '" + pattern + "' = " + 
                                 calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + 
                                 calendar.get(Calendar.DAY_OF_MONTH));
                return calendar;
            } catch (ParseException e) {
                // 다음 패턴 시도
                System.out.println("DEBUG: Pattern '" + pattern + "' failed for '" + dateStr + "'");
            }
        }
        
        System.out.println("DEBUG: All patterns failed for '" + dateStr + "'");
        return null;
    }
    
    // 문자열 날짜로 직접 필터링합니다 (일 단위 비교).
    private void applyStringDateFilter(String filterDate) {
        mData.clear();
        
        System.out.println("DEBUG: Applying string date filter = '" + filterDate + "'");
        
        // 필터 날짜를 Calendar로 파싱
        Calendar filterCalendar = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(filterDate);
            filterCalendar = Calendar.getInstance();
            filterCalendar.setTime(date);
        } catch (ParseException e) {
            System.out.println("DEBUG: Failed to parse filter date: " + filterDate);
        }
        
        for (UpdatedManga manga : originalData) {
            String mangaDateStr = manga.getDate();
            System.out.println("DEBUG: Comparing manga date '" + mangaDateStr + "' with filter '" + filterDate + "'");
            
            if (mangaDateStr != null) {
                // 만화 날짜를 파싱하여 일 단위로 비교
                Calendar mangaCalendar = parseMangaDate(mangaDateStr);
                
                if (mangaCalendar != null && filterCalendar != null) {
                    // Calendar 객체로 일 단위 비교
                    if (isSameDayCalendar(mangaCalendar, filterCalendar)) {
                        mData.add(manga);
                        System.out.println("DEBUG: Added manga (calendar match): " + manga.getName());
                    }
                } else {
                    // 파싱에 실패한 경우 문자열 직접 비교
                    if (mangaDateStr.equals(filterDate)) {
                        mData.add(manga);
                        System.out.println("DEBUG: Added manga (string match): " + manga.getName());
                    }
                }
            }
        }
        
        System.out.println("DEBUG: Filtered result count: " + mData.size());
        notifyDataSetChanged();
    }
    
    // 두 Calendar 객체가 같은 날인지 확인합니다.
    private boolean isSameDayCalendar(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
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
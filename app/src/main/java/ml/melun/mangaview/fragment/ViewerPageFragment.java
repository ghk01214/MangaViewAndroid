package ml.melun.mangaview.fragment;

import static ml.melun.mangaview.Utils.getGlideUrl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.fragment.app.Fragment;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import ml.melun.mangaview.R;
import ml.melun.mangaview.interfaces.PageInterface;
import ml.melun.mangaview.mangaview.Decoder;

// 뷰어에서 개별 페이지를 표시하는 프래그먼트
public class ViewerPageFragment extends Fragment {
    String image; // 표시할 이미지의 URL 또는 로컬 경로
    Decoder decoder; // 이미지 디코더 (스크램블 해제용)
    Context context;
    PageInterface i; // 페이지 클릭 이벤트를 처리할 인터페이스
    int width; // 화면 너비 (이미지 리사이징에 사용)

    public ViewerPageFragment(){

    }

    // 생성자
    public ViewerPageFragment(String image, Decoder decoder, int width, Context context, PageInterface i){
        this.image = image;
        this.decoder = decoder;
        this.width = width;
        this.context = context;
        this.i = i;
    }

    // 프래그먼트 인스턴스를 생성하는 정적 팩토리 메서드
    public static Fragment create(String image, Decoder decoder, int width, Context context, PageInterface i){
        return new ViewerPageFragment(image, decoder, width, context, i);
    }

    // Context를 업데이트하는 메서드 (프래그먼트 재사용 시 필요할 수 있음)
    public void updatePageFragment(Context context){
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_viewer, container, false);
        ImageView frame = rootView.findViewById(R.id.page); // 이미지를 표시할 ImageView
        ImageButton refresh = rootView.findViewById(R.id.refreshButton); // 새로고침 버튼

        // 초기 플레이스홀더 이미지 설정 및 새로고침 버튼 표시
        frame.setImageResource(R.drawable.placeholder);
        refresh.setVisibility(View.VISIBLE);

        // Context가 유효하면 이미지 로딩 시작
        if(context != null)
            loadImage(frame, refresh);

        // 새로고침 버튼 클릭 리스너
        refresh.setOnClickListener(v -> {
            if(context != null) {
                loadImage(frame, refresh);
            }
        });
        // 페이지 클릭 리스너
        rootView.setOnClickListener(v -> i.onPageClick());

        return rootView;
    }

    // Glide를 사용하여 이미지를 로드하고, 필요 시 디코딩하여 ImageView에 표시하는 메서드
    void loadImage(ImageView frame, ImageButton refresh){
        // URL인 경우 GlideUrl로, 로컬 경로인 경우 String으로 로드 대상을 설정
        Object target = image.startsWith("http") ? getGlideUrl(image) : image;
        Glide.with(frame)
                .asBitmap() // 비트맵으로 로드
                .load(target)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        // 로딩 성공 시
                        refresh.setVisibility(View.GONE); // 새로고침 버튼 숨김
                        bitmap = decoder.decode(bitmap,width); // 이미지 디코딩
                        frame.setImageBitmap(bitmap); // 디코딩된 이미지 표시
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        //
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // 로딩 실패 시
                        if(image.length()>0) {
                            frame.setImageResource(R.drawable.placeholder); // 플레이스홀더 표시
                            refresh.setVisibility(View.VISIBLE); // 새로고침 버튼 표시
                        }
                    }
                });
    }

    // 클릭 리스너를 설정하는 메서드
    public void setOnClick(PageInterface i){
        this.i = i;
    }
}

package ml.melun.mangaview.adapter;

import android.content.Context;
import android.os.Parcelable;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ml.melun.mangaview.fragment.ViewerPageFragment;
import ml.melun.mangaview.interfaces.PageInterface;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;

import static ml.melun.mangaview.MainApplication.p;

// 뷰어의 페이지를 ViewPager로 넘겨볼 수 있게 해주는 어댑터
public class ViewerPagerAdapter extends FragmentStatePagerAdapter
{
    List<Fragment> fragments; // 각 페이지를 담을 프래그먼트 리스트
    FragmentManager fm;
    int width; // 화면 너비
    Context context;
    PageInterface itf; // 페이지 클릭 인터페이스

    public ViewerPagerAdapter(FragmentManager fm, int width, Context context, PageInterface i) {
        super(fm);
        this.fm = fm;
        this.width = width;
        this.context = context;
        this.itf = i;
        fragments = new ArrayList<>();
    }

    // 표시할 만화(Manga) 객체를 설정하고, 페이지 프래그먼트를 생성합니다.
    public void setManga(Manga m){
        fragments.clear();
        List<String> imgs = m.getImgs(context);
        // 오른쪽에서 왼쪽으로 읽기(RTL) 설정이 켜져 있으면 이미지 순서를 뒤집습니다.
        if (p.getPageRtl()) Collections.reverse(imgs);
        for(int i = 0; i<imgs.size(); i++){
            String s = imgs.get(i);
            // 각 이미지 URL에 대해 ViewerPageFragment를 생성하여 리스트에 추가합니다.
            fragments.add(ViewerPageFragment.create(s, new Decoder(m.getSeed(), m.getId()), width, context, () -> itf.onPageClick()));
        }
        notifyDataSetChanged(); // 데이터 변경을 알려 뷰페이저를 갱신합니다.
    }

    // 아이템의 위치가 변경될 수 있음을 알려 항상 뷰를 다시 그리도록 합니다.
    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public Fragment getItem(int position)
    {
        return fragments.get(position);
    }

    @Override
    public int getCount()
    {
        return fragments.size();
    }

    // 상태를 저장하지 않아 뷰페이저가 항상 새로 그려지도록 합니다.
    @Override
    public Parcelable saveState()
    {
        return null;
    }

}

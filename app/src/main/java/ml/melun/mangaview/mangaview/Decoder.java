package ml.melun.mangaview.mangaview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static ml.melun.mangaview.Utils.getSample;


// 특정 사이트의 이미지 스크램블을 해제(디코딩)하는 클래스
public class Decoder {
    int __seed=0; // 디코딩에 사용되는 시드 값
    int id=0; // 만화 ID
    int view_cnt; // 조회수 (시드 값 계산에 사용)
    int cx=5, cy=5; // 이미지를 분할할 가로, 세로 개수

    public int getCnt(){
        return view_cnt;
    }

    // 생성자. 시드와 ID를 받아 디코딩에 필요한 값을 초기화합니다.
    public Decoder(int seed, int id){
        view_cnt = seed;
        __seed = seed/10;
        this.id = id;
        // 시드 값에 따라 분할 개수를 다르게 설정
        if(__seed>30000){
            cx = 1;
            cy = 6;
        }else if(__seed>20000){
            cx = 1;
        } else if (__seed>10000) {
            cy = 1;
        }
    }

    // 비트맵의 크기를 조정한 후 디코딩합니다.
    public Bitmap decode(Bitmap input, int width){
        input = getSample(input,width);
        return decode(input);
    }

    // 비트맵의 바이트 크기를 줄입니다. (다운샘플링)
    public Bitmap downSample(final Bitmap input, int maxBytes) {
        if(input.getByteCount() > maxBytes) {
            Float ratio = (maxBytes*1.0f/input.getByteCount());
            return downSize(input, ratio);
        }
        return input;
    }

    // 비트맵의 해상도를 비율에 맞게 줄입니다.
    public Bitmap downSize(final Bitmap input, Float ratio) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Bitmap bitmap = Bitmap.createScaledBitmap(input, ((Float)(input.getWidth()*ratio)).intValue(), ((Float)(input.getHeight()*ratio)).intValue(), true);
        return bitmap;
    }

    // 스크램블된 비트맵 이미지를 디코딩합니다.
    public Bitmap decode(Bitmap input){
        input = downSample(input, 100000000); // 이미지 크기 제한
        if(view_cnt==0) return input; // 조회수가 0이면 디코딩 불필요

        // 1. 이미지 조각의 순서를 결정합니다.
        int[][] order = new int[cx*cy][2];
        for (int i = 0; i < cx*cy; i++) {
            order[i][0] = i; // 원래 인덱스
            // 만화 ID에 따라 다른 랜덤 함수를 사용하여 순서 결정
            if (id < 554714) order[i][1] = _random(i);
            else order[i][1] = newRandom(i);
        }
        // 랜덤 값을 기준으로 순서를 정렬합니다.
        java.util.Arrays.sort(order, (a, b) -> {
            return a[1] != b[1] ? a[1] - b[1] : a[0] - b[0];
        });

        // 2. 정렬된 순서에 따라 이미지 조각을 다시 조합합니다.
        Bitmap output = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        int row_w = input.getWidth() / cx;
        int row_h = input.getHeight() / cy;
        for (int i = 0; i < cx*cy; i++) {
            int[] o = order[i];
            int ox = i % cx; // 원본 이미지에서 가져올 조각의 x 좌표
            int oy = i / cx; // 원본 이미지에서 가져올 조각의 y 좌표
            int tx = o[0] % cx; // 결과 이미지에 붙여넣을 조각의 x 좌표
            int ty = o[0] / cx; // 결과 이미지에 붙여넣을 조각의 y 좌표

            // 원본 이미지에서 조각을 잘라내어 결과 캔버스에 그립니다.
            Bitmap cropped = Bitmap.createBitmap(input, ox * row_w, oy * row_h, row_w, row_h);
            canvas.drawBitmap(cropped, tx * row_w, ty * row_h, null);
        }
        return output;
    }

    // 구버전 랜덤 함수
    private int _random(int index){
        double x = Math.sin(__seed+index) * 10000;
        return (int) Math.floor((x - Math.floor(x)) * 100000);
    }

    // 신버전 랜덤 함수
    private int newRandom(int index){
        index++;
        double t = 100 * Math.sin(10 * (__seed+index))
                , n = 1000 * Math.cos(13 * (__seed+index))
                , a = 10000 * Math.tan(14 * (__seed+index));
        t = Math.floor(100 * (t - Math.floor(t)));
        n = Math.floor(1000 * (n - Math.floor(n)));
        a = Math.floor(10000 * (a - Math.floor(a)));
        return (int)(t + n + a);
    }
}
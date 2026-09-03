package ntu.nguyenhoangphuc.personal_diary.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import ntu.nguyenhoangphuc.personal_diary.R;

/**
 * Custom View vẽ thanh ngang chia màu theo % của 5 loại tâm trạng - dùng ở
 * card "Tâm trạng" trong StatsFragment (id viewTyLeTamTrang trong XML).
 * XML chỉ khai id + kích thước, KHÔNG vẽ gì cả - toàn bộ nội dung vẽ ở đây
 * qua onDraw(Canvas), đúng như UI mentor ghi chú trong file gửi qua.
 *
 * Thứ tự 5 mood LUÔN cố định: Vui - Bình yên - Buồn - Giận - Trung tính,
 * khớp đúng thứ tự liệt kê ở layoutChuThichTamTrang bên dưới thanh này.
 */
public class MoodRatioBarView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // % của từng mood, theo đúng thứ tự cố định - mặc định toàn 0 (chưa có dữ liệu)
    private float phanTramHappy = 0f;
    private float phanTramCalm = 0f;
    private float phanTramSad = 0f;
    private float phanTramAngry = 0f;
    private float phanTramNeutral = 0f;

    public MoodRatioBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Gọi từ StatsFragment sau khi tính xong % của 5 mood - set xong PHẢI
    // gọi invalidate() để Android biết cần vẽ lại (onDraw không tự chạy lại
    // khi chỉ đổi biến thường)
    public void capNhatDuLieu(float happy, float calm, float sad, float angry, float neutral) {
        this.phanTramHappy = happy;
        this.phanTramCalm = calm;
        this.phanTramSad = sad;
        this.phanTramAngry = angry;
        this.phanTramNeutral = neutral;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float chieuRong = getWidth();
        float chieuCao = getHeight();

        // Vẽ nối tiếp từng đoạn từ trái sang phải - đoạn nào xong thì trả về
        // toạ độ x kết thúc để đoạn kế tiếp biết bắt đầu từ đâu
        float xBatDau = 0f;
        xBatDau = veMotDoan(canvas, xBatDau, chieuRong, chieuCao, phanTramHappy, R.color.mood_happy);
        xBatDau = veMotDoan(canvas, xBatDau, chieuRong, chieuCao, phanTramCalm, R.color.mood_calm);
        xBatDau = veMotDoan(canvas, xBatDau, chieuRong, chieuCao, phanTramSad, R.color.mood_sad);
        xBatDau = veMotDoan(canvas, xBatDau, chieuRong, chieuCao, phanTramAngry, R.color.mood_angry);
        veMotDoan(canvas, xBatDau, chieuRong, chieuCao, phanTramNeutral, R.color.mood_neutral);
    }

    // Vẽ 1 đoạn hình chữ nhật rộng theo đúng %, trả về vị trí x kết thúc
    private float veMotDoan(Canvas canvas, float xBatDau, float chieuRong, float chieuCao,
                            float phanTram, int maMauRes) {
        if (phanTram <= 0f) {
            return xBatDau;
        }
        float doRong = chieuRong * (phanTram / 100f);
        paint.setColor(ContextCompat.getColor(getContext(), maMauRes));
        canvas.drawRect(xBatDau, 0f, xBatDau + doRong, chieuCao, paint);
        return xBatDau + doRong;
    }
}
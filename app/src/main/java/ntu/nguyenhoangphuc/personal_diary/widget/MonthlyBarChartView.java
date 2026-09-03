package ntu.nguyenhoangphuc.personal_diary.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.model.ThongKeThang;

/**
 * Custom View vẽ biểu đồ cột số bài viết theo tháng - dùng ở card "6 tháng
 * gần đây" trong StatsFragment (id viewBieuDoThang trong XML). Giống hệt
 * MoodRatioBarView, toàn bộ vẽ nằm trong onDraw(Canvas), XML chỉ khai id +
 * kích thước.
 */
public class MonthlyBarChartView extends View {

    // Khoảng trống trên/dưới mỗi cột để vẽ chữ (số bài phía trên, tên tháng
    // phía dưới), đơn vị dp - đổi sang px lúc vẽ
    private static final int KHOANG_TRONG_TREN_DP = 20;
    private static final int KHOANG_TRONG_DUOI_DP = 20;

    // Khoảng cách 2 bên mỗi cột (dp) - để các cột không dính sát nhau
    private static final int KHOANG_CACH_COT_DP = 4;

    private final Paint paintCot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintChu = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<ThongKeThang> danhSachThang = new ArrayList<>();

    public MonthlyBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paintCot.setColor(ContextCompat.getColor(context, R.color.accent_terracotta));

        paintChu.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        paintChu.setTextAlign(Paint.Align.CENTER);
        paintChu.setTextSize(spSangPx(11));
    }

    // Gọi từ StatsFragment sau khi query xong 6 tháng gần đây
    public void capNhatDuLieu(List<ThongKeThang> danhSachThangMoi) {
        this.danhSachThang = danhSachThangMoi;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (danhSachThang.isEmpty()) {
            return; // chưa có dữ liệu thì không vẽ gì, tránh chia cho 0 phía dưới
        }

        float chieuRong = getWidth();
        float chieuCao = getHeight();

        float khoangTrenPx = dpSangPx(KHOANG_TRONG_TREN_DP);
        float khoangDuoiPx = dpSangPx(KHOANG_TRONG_DUOI_DP);
        float khoangCachCotPx = dpSangPx(KHOANG_CACH_COT_DP);

        float chieuCaoVungCot = chieuCao - khoangTrenPx - khoangDuoiPx;

        int soThang = danhSachThang.size();
        float chieuRongMoiCot = (chieuRong / soThang) - khoangCachCotPx;

        // Số bài cao nhất trong 6 tháng, ép tối thiểu = 1 để tránh chia cho 0
        // nếu cả 6 tháng đều chưa có bài nào
        int soBaiCaoNhat = 1;
        for (ThongKeThang thang : danhSachThang) {
            if (thang.getSoBai() > soBaiCaoNhat) {
                soBaiCaoNhat = thang.getSoBai();
            }
        }

        for (int i = 0; i < soThang; i++) {
            ThongKeThang thang = danhSachThang.get(i);

            float xGiuaCot = (chieuRong / soThang) * i + (chieuRong / soThang) / 2f;
            float xTrai = xGiuaCot - chieuRongMoiCot / 2f;
            float xPhai = xGiuaCot + chieuRongMoiCot / 2f;

            float tiLeChieuCao = thang.getSoBai() / (float) soBaiCaoNhat;
            float chieuCaoCotThat = chieuCaoVungCot * tiLeChieuCao;

            float yDay = khoangTrenPx + chieuCaoVungCot;
            float yDinh = yDay - chieuCaoCotThat;

            // Tháng 0 bài vẫn giữ 1 vạch mỏng sát đáy cho dễ nhìn vị trí,
            // không để trống trơn nhìn như thiếu dữ liệu
            if (thang.getSoBai() == 0) {
                yDinh = yDay - dpSangPx(2);
            }

            canvas.drawRect(xTrai, yDinh, xPhai, yDay, paintCot);

            canvas.drawText(String.valueOf(thang.getSoBai()), xGiuaCot,
                    yDinh - dpSangPx(4), paintChu);

            canvas.drawText(thang.getNhanThang(), xGiuaCot,
                    chieuCao - dpSangPx(4), paintChu);
        }
    }

    private float dpSangPx(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private float spSangPx(int sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                getResources().getDisplayMetrics());
    }
}
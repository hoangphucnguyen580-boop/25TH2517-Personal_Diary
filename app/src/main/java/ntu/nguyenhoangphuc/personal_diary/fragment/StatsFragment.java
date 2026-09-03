package ntu.nguyenhoangphuc.personal_diary.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Map;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.database.DiaryDatabaseHelper;
import ntu.nguyenhoangphuc.personal_diary.model.ThongKeThang;
import ntu.nguyenhoangphuc.personal_diary.model.ThongKeThe;
import ntu.nguyenhoangphuc.personal_diary.widget.MonthlyBarChartView;
import ntu.nguyenhoangphuc.personal_diary.widget.MoodRatioBarView;

/**
 * Tab "Thống kê" - tổng hợp số liệu ALL-TIME từ toàn bộ nhật ký (không lọc
 * theo tháng đang xem như CalendarFragment - đã chốt với Phúc): tổng quan
 * nhanh, tỷ lệ tâm trạng, top 5 thẻ hay dùng, biểu đồ số bài 6 tháng gần đây.
 */
public class StatsFragment extends Fragment {

    private ScrollView scrollThongKe;
    private LinearLayout layoutRongThongKe;

    private TextView tvTongSoBai, tvTongSoAnh, tvStreakDaiNhat;

    private MoodRatioBarView viewTyLeTamTrang;
    private TextView tvPhanTramHappy, tvPhanTramCalm, tvPhanTramSad, tvPhanTramAngry, tvPhanTramNeutral;

    private LinearLayout layoutTopThe;

    private MonthlyBarChartView viewBieuDoThang;

    private DiaryDatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        anhXaView(view);
        dbHelper = new DiaryDatabaseHelper(requireContext());

        taiDuLieu();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Quay lại tab này sau khi Thêm/Sửa/Xoá bài ở màn khác -> load lại số
        // liệu mới nhất, giống hệt pattern onResume() của 3 fragment kia
        if (dbHelper != null) {
            taiDuLieu();
        }
    }

    private void anhXaView(View view) {
        scrollThongKe = view.findViewById(R.id.scrollThongKe);
        layoutRongThongKe = view.findViewById(R.id.layoutRongThongKe);

        tvTongSoBai = view.findViewById(R.id.tvTongSoBai);
        tvTongSoAnh = view.findViewById(R.id.tvTongSoAnh);
        tvStreakDaiNhat = view.findViewById(R.id.tvStreakDaiNhat);

        viewTyLeTamTrang = view.findViewById(R.id.viewTyLeTamTrang);
        tvPhanTramHappy = view.findViewById(R.id.tvPhanTramHappy);
        tvPhanTramCalm = view.findViewById(R.id.tvPhanTramCalm);
        tvPhanTramSad = view.findViewById(R.id.tvPhanTramSad);
        tvPhanTramAngry = view.findViewById(R.id.tvPhanTramAngry);
        tvPhanTramNeutral = view.findViewById(R.id.tvPhanTramNeutral);

        layoutTopThe = view.findViewById(R.id.layoutTopThe);

        viewBieuDoThang = view.findViewById(R.id.viewBieuDoThang);
    }

    private void taiDuLieu() {
        int tongSoBai = dbHelper.demTongSoBai();

        if (tongSoBai == 0) {
            scrollThongKe.setVisibility(View.GONE);
            layoutRongThongKe.setVisibility(View.VISIBLE);
            return;
        }

        scrollThongKe.setVisibility(View.VISIBLE);
        layoutRongThongKe.setVisibility(View.GONE);

        capNhatTongQuan(tongSoBai);
        capNhatTyLeTamTrang();
        capNhatTopThe();
        capNhatBieuDoThang();
    }

    // ===================== TỔNG QUAN NHANH =====================

    private void capNhatTongQuan(int tongSoBai) {
        tvTongSoBai.setText(String.valueOf(tongSoBai));
        tvTongSoAnh.setText(String.valueOf(dbHelper.demTongSoAnh()));
        tvStreakDaiNhat.setText(String.valueOf(dbHelper.tinhStreakDaiNhat()));
    }

    // ===================== TỶ LỆ TÂM TRẠNG =====================

    private void capNhatTyLeTamTrang() {
        Map<String, Integer> soLuongTheoMood = dbHelper.demSoLuongTheoTamTrang();

        int tongSoBaiCoMood = 0;
        for (int soLuong : soLuongTheoMood.values()) {
            tongSoBaiCoMood += soLuong;
        }

        int phanTramHappy = tinhPhanTram(soLuongTheoMood.get("happy"), tongSoBaiCoMood);
        int phanTramCalm = tinhPhanTram(soLuongTheoMood.get("calm"), tongSoBaiCoMood);
        int phanTramSad = tinhPhanTram(soLuongTheoMood.get("sad"), tongSoBaiCoMood);
        int phanTramAngry = tinhPhanTram(soLuongTheoMood.get("angry"), tongSoBaiCoMood);
        int phanTramNeutral = tinhPhanTram(soLuongTheoMood.get("neutral"), tongSoBaiCoMood);

        tvPhanTramHappy.setText(phanTramHappy + "%");
        tvPhanTramCalm.setText(phanTramCalm + "%");
        tvPhanTramSad.setText(phanTramSad + "%");
        tvPhanTramAngry.setText(phanTramAngry + "%");
        tvPhanTramNeutral.setText(phanTramNeutral + "%");

        viewTyLeTamTrang.capNhatDuLieu(phanTramHappy, phanTramCalm, phanTramSad, phanTramAngry, phanTramNeutral);
    }

    // Chưa có bài nào gắn mood (mauSo = 0) thì trả về 0%, tránh chia cho 0
    private int tinhPhanTram(Integer soLuong, int mauSo) {
        if (soLuong == null || mauSo == 0) {
            return 0;
        }
        return Math.round(soLuong * 100f / mauSo);
    }

    // ===================== TOP 5 THẺ HAY DÙNG =====================

    private void capNhatTopThe() {
        layoutTopThe.removeAllViews(); // xoá hàng cũ trước, tránh cộng dồn mỗi lần onResume

        List<ThongKeThe> danhSachTop5 = dbHelper.layTop5TheDaSuDung();

        if (danhSachTop5.isEmpty()) {
            TextView tvChuaCoThe = new TextView(requireContext());
            tvChuaCoThe.setText(R.string.thong_ke_chua_co_the);
            tvChuaCoThe.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvChuaCoThe.setTextSize(12);
            layoutTopThe.addView(tvChuaCoThe);
            return;
        }

        // Thẻ đứng đầu (dùng nhiều nhất) làm mốc 100% cho thanh mini tỉ lệ -
        // các thẻ còn lại co giãn theo tỉ lệ so với thẻ này
        int soLanDungNhieuNhat = danhSachTop5.get(0).getSoLanDung();

        for (ThongKeThe the : danhSachTop5) {
            View hang = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_the_thong_ke, layoutTopThe, false);

            TextView tvTenThe = hang.findViewById(R.id.tvTenThe);
            TextView tvSoLanDung = hang.findViewById(R.id.tvSoLanDung);
            View viewTiLeThe = hang.findViewById(R.id.viewTiLeThe);

            tvTenThe.setText(the.getTenThe());
            tvSoLanDung.setText(String.valueOf(the.getSoLanDung()));

            int tiLePhanTram = Math.round(the.getSoLanDung() * 100f / soLanDungNhieuNhat);
            LinearLayout.LayoutParams thamSo = (LinearLayout.LayoutParams) viewTiLeThe.getLayoutParams();
            thamSo.weight = tiLePhanTram;
            viewTiLeThe.setLayoutParams(thamSo);

            layoutTopThe.addView(hang);
        }
    }

    // ===================== BIỂU ĐỒ 6 THÁNG GẦN ĐÂY =====================

    private void capNhatBieuDoThang() {
        List<ThongKeThang> danhSach6Thang = dbHelper.laySoBaiTheo6ThangGanDay();
        viewBieuDoThang.capNhatDuLieu(danhSach6Thang);
    }
}
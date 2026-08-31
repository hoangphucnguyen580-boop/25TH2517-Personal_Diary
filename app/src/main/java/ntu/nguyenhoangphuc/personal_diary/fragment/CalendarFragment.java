package ntu.nguyenhoangphuc.personal_diary.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.activity.AddEditDiaryActivity;
import ntu.nguyenhoangphuc.personal_diary.adapter.DayAdapter;
import ntu.nguyenhoangphuc.personal_diary.adapter.DiaryAdapter;
import ntu.nguyenhoangphuc.personal_diary.database.DiaryDatabaseHelper;
import ntu.nguyenhoangphuc.personal_diary.model.DiaryEntry;

/**
 * Tab "Lịch" (bản MVP) - Phương án A do UI mentor thiết kế: lưới lịch tháng
 * cố định phía trên, danh sách bài viết của ngày đang chọn cuộn riêng phía
 * dưới (tái dùng DiaryAdapter + item_diary_entry.xml đã có sẵn). Tuần bắt
 * đầu Thứ 2 (đã chốt với Phúc).
 */
public class CalendarFragment extends Fragment {

    private ImageButton btnThangTruoc, btnThangSau;
    private TextView tvThangNam, tvNgayDangChon;
    private RecyclerView rvLichThang, rvBaiVietTrongNgay;

    private DiaryDatabaseHelper dbHelper;
    private DayAdapter dayAdapter;
    private DiaryAdapter diaryAdapter;

    // Tháng đang hiển thị trên lưới - LUÔN chốt về ngày 1, giờ 0h (xem
    // ganVeNgayDauThang) để tính toán không bị lệch do giờ/phút thừa
    private final Calendar thangDangXem = Calendar.getInstance();

    // Ngày đang được chọn để xem bài viết, định dạng yyyy-MM-dd
    private String ngayDangChonYyyyMMdd;

    private final SimpleDateFormat dinhDangLuu = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat dinhDangHienThiNgay = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        anhXaView(view);
        dbHelper = new DiaryDatabaseHelper(requireContext());

        // Mở đúng tháng hiện tại, chốt về ngày 1 giờ 0h
        ganVeNgayDauThang(thangDangXem);

        // Mặc định chọn sẵn hôm nay - vì thangDangXem khởi tạo = tháng hiện
        // tại nên hôm nay LUÔN thuộc tháng đang mở, không cần kiểm tra thêm
        ngayDangChonYyyyMMdd = dinhDangLuu.format(Calendar.getInstance().getTime());

        khoiTaoRvLichThang();
        khoiTaoRvBaiVietTrongNgay();

        btnThangTruoc.setOnClickListener(v -> doiThang(-1));
        btnThangSau.setOnClickListener(v -> doiThang(1));

        capNhatLuoiThang();
        capNhatDanhSachBaiVietTheoNgay();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Quay lại tab này sau khi Thêm/Sửa bài ở màn khác -> load lại dữ
        // liệu mới nhất, giống hệt cách HomeFragment đang làm
        if (dbHelper != null) {
            capNhatLuoiThang();
            capNhatDanhSachBaiVietTheoNgay();
        }
    }

    private void anhXaView(View view) {
        btnThangTruoc = view.findViewById(R.id.btnThangTruoc);
        tvThangNam = view.findViewById(R.id.tvThangNam);
        btnThangSau = view.findViewById(R.id.btnThangSau);
        rvLichThang = view.findViewById(R.id.rvLichThang);
        tvNgayDangChon = view.findViewById(R.id.tvNgayDangChon);
        rvBaiVietTrongNgay = view.findViewById(R.id.rvBaiVietTrongNgay);
    }

    private void khoiTaoRvLichThang() {
        // XML đã khai app:layoutManager + app:spanCount="7" - set lại 1 lần
        // nữa bằng code cho chắc, đúng lưu ý UI mentor để lại trong comment XML
        rvLichThang.setLayoutManager(new GridLayoutManager(requireContext(), 7));

        dayAdapter = new DayAdapter(new ArrayList<>());
        dayAdapter.setOnNgayClickListener(ngay -> {
            ngayDangChonYyyyMMdd = ngay.ngayThangDayDu;
            capNhatLuoiThang(); // vẽ lại để tô nổi bật đúng ô vừa chọn
            capNhatDanhSachBaiVietTheoNgay();
        });
        rvLichThang.setAdapter(dayAdapter);
    }

    private void khoiTaoRvBaiVietTrongNgay() {
        rvBaiVietTrongNgay.setLayoutManager(new LinearLayoutManager(requireContext()));
        diaryAdapter = new DiaryAdapter(requireContext(), new ArrayList<>(), dbHelper);

        // Bấm vào 1 bài trong danh sách -> mở màn Sửa, giống hệt hành vi ở
        // HomeFragment. Bảng yêu cầu không nói rõ điểm này nhưng đây là hành
        // vi mặc định hợp lý nhất khi đã tái dùng đúng adapter đã hỗ trợ sẵn.
        diaryAdapter.setOnItemClickListener(entry -> {
            Intent intent = new Intent(requireContext(), AddEditDiaryActivity.class);
            intent.putExtra(AddEditDiaryActivity.EXTRA_DIARY_ID, entry.getId());
            startActivity(intent);
        });

        rvBaiVietTrongNgay.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBaiVietTrongNgay.setAdapter(diaryAdapter);
    }

    // ===================== ĐIỀU HƯỚNG THÁNG =====================

    private void doiThang(int soThangCong) {
        thangDangXem.add(Calendar.MONTH, soThangCong);
        capNhatLuoiThang();
        // KHÔNG tự đổi ngayDangChonYyyyMMdd khi lật tháng - giữ nguyên ngày
        // đang chọn. rvBaiVietTrongNgay không phụ thuộc thangDangXem nên vẫn
        // hiện đúng, chỉ là ô được tô nổi bật sẽ không thấy nếu ngày đó không
        // thuộc tháng đang xem - không sao, quay lại đúng tháng là thấy lại.
    }

    private void ganVeNgayDauThang(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    // ===================== VẼ LƯỚI LỊCH =====================

    private void capNhatLuoiThang() {
        tvThangNam.setText(getString(R.string.calendar_thang_nam,
                thangDangXem.get(Calendar.MONTH) + 1, thangDangXem.get(Calendar.YEAR)));

        String thangNamDangXem = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(thangDangXem.getTime());
        Set<String> danhSachNgayCoBai = dbHelper.layDanhSachNgayCoBaiTrongThang(thangNamDangXem);

        List<DayAdapter.NgayLich> danhSachO = new ArrayList<>();

        // Calendar.DAY_OF_WEEK trả về CHỦ NHẬT=1 ... THỨ 7=7 - phải quy đổi
        // sang hệ THỨ 2=0 ... CHỦ NHẬT=6 vì tuần bắt đầu Thứ 2 (đã chốt).
        // Công thức (v + 5) % 7: T2(2)->0, T3(3)->1,... T7(7)->5, CN(1)->6
        int thuCuaNgay1 = thangDangXem.get(Calendar.DAY_OF_WEEK);
        int soOTrongDauThang = (thuCuaNgay1 + 5) % 7;

        for (int i = 0; i < soOTrongDauThang; i++) {
            danhSachO.add(new DayAdapter.NgayLich(0, null, false, false));
        }

        int soNgayTrongThang = thangDangXem.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int ngay = 1; ngay <= soNgayTrongThang; ngay++) {
            // Clone riêng cho từng ngày - tránh sửa trực tiếp thangDangXem
            // (field dùng chung, sửa lung tung dễ gây lệch trạng thái tháng)
            Calendar ngayDangXet = (Calendar) thangDangXem.clone();
            ngayDangXet.set(Calendar.DAY_OF_MONTH, ngay);
            String ngayDayDu = dinhDangLuu.format(ngayDangXet.getTime());

            boolean coBai = danhSachNgayCoBai.contains(ngayDayDu);
            boolean dangNoiBat = ngayDayDu.equals(ngayDangChonYyyyMMdd);

            danhSachO.add(new DayAdapter.NgayLich(ngay, ngayDayDu, coBai, dangNoiBat));
        }

        dayAdapter.capNhatDanhSach(danhSachO);
    }

    // ===================== DANH SÁCH BÀI THEO NGÀY =====================

    private void capNhatDanhSachBaiVietTheoNgay() {
        if (ngayDangChonYyyyMMdd == null) {
            tvNgayDangChon.setText(R.string.calendar_chua_chon_ngay);
            diaryAdapter.capNhatDanhSach(new ArrayList<>());
            return;
        }

        try {
            String ngayHienThi = dinhDangHienThiNgay.format(dinhDangLuu.parse(ngayDangChonYyyyMMdd));
            tvNgayDangChon.setText(getString(R.string.calendar_bai_viet_ngay, ngayHienThi));
        } catch (ParseException e) {
            tvNgayDangChon.setText(getString(R.string.calendar_bai_viet_ngay, ngayDangChonYyyyMMdd));
        }

        List<DiaryEntry> danhSachBai = dbHelper.layDanhSachBaiTheoNgay(ngayDangChonYyyyMMdd);
        diaryAdapter.capNhatDanhSach(danhSachBai);
    }
}
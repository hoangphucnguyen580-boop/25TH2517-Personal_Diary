package ntu.nguyenhoangphuc.personal_diary.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.activity.AddEditDiaryActivity;
import ntu.nguyenhoangphuc.personal_diary.adapter.MemoriesAdapter;
import ntu.nguyenhoangphuc.personal_diary.database.DiaryDatabaseHelper;
import ntu.nguyenhoangphuc.personal_diary.model.AnhKyNiem;

/**
 * Tab "Kỷ niệm" - lưới ảnh gộp từ TẤT CẢ bài viết trong nhật ký, nhóm theo
 * tháng/năm (mới nhất trước), 3 cột. Bấm vào 1 ảnh -> mở bài viết gốc chứa
 * ảnh đó (đã chốt, không dựng màn xem phóng to xuyên-bài riêng ở bản này).
 * Layout do UI mentor thiết kế - Phương án PA3 (khối tiêu đề đứng yên, không
 * nằm trong RecyclerView, xem HUONG_DAN_KY_NIEM.md).
 */
public class MemoriesFragment extends Fragment {

    private static final int SO_COT_LUOI = 3;

    private TextView tvSoLuongAnh;
    private LinearLayout layoutRongKyNiem;
    private RecyclerView rvKyNiem;

    private DiaryDatabaseHelper dbHelper;
    private MemoriesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ky_niem, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSoLuongAnh = view.findViewById(R.id.tvSoLuongAnh);
        layoutRongKyNiem = view.findViewById(R.id.layoutRongKyNiem);
        rvKyNiem = view.findViewById(R.id.rvKyNiem);

        dbHelper = new DiaryDatabaseHelper(requireContext());

        khoiTaoRecyclerView();
        taiDuLieu();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Quay lại tab này sau khi thêm/xoá ảnh ở màn khác -> load lại, giống
        // hệt pattern onResume() của HomeFragment/CalendarFragment
        if (dbHelper != null) {
            taiDuLieu();
        }
    }

    private void khoiTaoRecyclerView() {
        adapter = new MemoriesAdapter(requireContext(), new ArrayList<>());
        adapter.setOnAnhClickListener(anh -> {
            Intent intent = new Intent(requireContext(), AddEditDiaryActivity.class);
            intent.putExtra(AddEditDiaryActivity.EXTRA_DIARY_ID, anh.getNhatKyId());
            startActivity(intent);
        });

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), SO_COT_LUOI);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Header tháng chiếm hết hàng (đủ SO_COT_LUOI cột), ảnh chỉ
                // chiếm 1 cột - đúng kỹ thuật UI mentor note trong
                // HUONG_DAN_KY_NIEM.md mục 4
                return adapter.laHeaderTaiViTri(position) ? SO_COT_LUOI : 1;
            }
        });

        rvKyNiem.setLayoutManager(layoutManager);
        rvKyNiem.setAdapter(adapter);
    }

    private void taiDuLieu() {
        List<AnhKyNiem> danhSachAnh = dbHelper.layTatCaAnhKemNgay();

        tvSoLuongAnh.setText(getString(R.string.so_luong_anh_ky_niem, danhSachAnh.size()));

        if (danhSachAnh.isEmpty()) {
            layoutRongKyNiem.setVisibility(View.VISIBLE);
            rvKyNiem.setVisibility(View.GONE);
            return;
        }

        layoutRongKyNiem.setVisibility(View.GONE);
        rvKyNiem.setVisibility(View.VISIBLE);

        List<MemoriesAdapter.MucHienThi> danhSachHienThi = gomNhomTheoThang(danhSachAnh);
        adapter.capNhatDanhSach(danhSachHienThi);
    }

    // Ảnh đã được SQL sắp mới->cũ sẵn (xem layTatCaAnhKemNgay) - ở đây chỉ cần
    // duyệt tuần tự, phát hiện lúc nào "yyyy-MM" đổi so với ảnh trước thì chèn
    // 1 header mới ngay trước đó. Cùng tinh thần CalendarFragment.capNhatLuoiThang()
    // đang chèn ô rỗng đầu tháng.
    private List<MemoriesAdapter.MucHienThi> gomNhomTheoThang(List<AnhKyNiem> danhSachAnh) {
        List<MemoriesAdapter.MucHienThi> ketQua = new ArrayList<>();
        String thangNamDangXet = null;

        for (AnhKyNiem anh : danhSachAnh) {
            String thangNamCuaAnh = anh.getNgayThang().substring(0, 7); // "yyyy-MM"

            if (!thangNamCuaAnh.equals(thangNamDangXet)) {
                ketQua.add(MemoriesAdapter.MucHienThi.taoHeader(tieuDeThangTuKhoa(thangNamCuaAnh)));
                thangNamDangXet = thangNamCuaAnh;
            }

            ketQua.add(MemoriesAdapter.MucHienThi.taoAnh(anh));
        }

        return ketQua;
    }

    // "2026-08" -> "Tháng 8, 2026" - tái dùng ĐÚNG string resource calendar_thang_nam
    // đã có sẵn từ CalendarFragment, không tạo string trùng ý nghĩa
    private String tieuDeThangTuKhoa(String thangNamKey) {
        int nam = Integer.parseInt(thangNamKey.substring(0, 4));
        int thang = Integer.parseInt(thangNamKey.substring(5, 7));
        return getString(R.string.calendar_thang_nam, thang, nam);
    }
}
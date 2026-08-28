package ntu.nguyenhoangphuc.personal_diary.activity;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.adapter.PhotoViewerAdapter;

/**
 * Dialog toàn màn hình (Phần 2.3) để xem 1 ảnh trong dải ảnh ở chế độ phóng
 * to - cho pinch-zoom + vuốt trái/phải qua các ảnh khác trong CÙNG bài đang
 * soạn. Đọc trực tiếp từ dải ảnh đang hiện trên màn hình (chưa cần Lưu xuống
 * DB), nên xem được cả ảnh vừa mới thêm.
 */
public class PhotoViewerDialog extends Dialog {

    private final List<String> danhSachDuongDan;
    private final List<String> danhSachChuThich;
    private final int viTriBanDau;

    private ViewPager2 viewPagerAnh;
    private TextView tvSoThuTu;
    private TextView tvChuThich;
    private ImageButton btnDong;

    public PhotoViewerDialog(Context context, List<String> danhSachDuongDan,
                             List<String> danhSachChuThich, int viTriBanDau) {
        // Theme riêng làm Dialog chiếm toàn màn hình, nền đen, không thanh
        // tiêu đề - khai ở res/values/styles.xml
        super(context, R.style.Theme_PersonalDiary_PhotoViewerDialog);
        this.danhSachDuongDan = danhSachDuongDan;
        this.danhSachChuThich = danhSachChuThich;
        this.viTriBanDau = viTriBanDau;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_xem_anh_phong_to);

        viewPagerAnh = findViewById(R.id.viewPagerAnh);
        tvSoThuTu = findViewById(R.id.tvSoThuTu);
        tvChuThich = findViewById(R.id.tvChuThich);
        btnDong = findViewById(R.id.btnDong);

        PhotoViewerAdapter adapter = new PhotoViewerAdapter(danhSachDuongDan);
        viewPagerAnh.setAdapter(adapter);
        // false = nhảy thẳng tới đúng ảnh mày vừa bấm, không trượt qua các ảnh ở giữa
        viewPagerAnh.setCurrentItem(viTriBanDau, false);

        capNhatThongTinTrang(viTriBanDau);

        viewPagerAnh.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                capNhatThongTinTrang(position);
            }
        });

        // Dialog gốc của Android đã tự đóng khi bấm nút Back hệ thống rồi,
        // nút X này chỉ để tiện bấm tay trên màn hình cảm ứng
        btnDong.setOnClickListener(v -> dismiss());
    }

    private void capNhatThongTinTrang(int viTri) {
        int tongSoAnh = danhSachDuongDan.size();
        tvSoThuTu.setText((viTri + 1) + "/" + tongSoAnh);

        String chuThich = danhSachChuThich.get(viTri);
        if (TextUtils.isEmpty(chuThich)) {
            tvChuThich.setVisibility(View.GONE);
        } else {
            tvChuThich.setVisibility(View.VISIBLE);
            tvChuThich.setText(chuThich);
        }
    }
}
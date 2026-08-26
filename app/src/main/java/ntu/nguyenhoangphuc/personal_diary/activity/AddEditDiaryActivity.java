package ntu.nguyenhoangphuc.personal_diary.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.database.DiaryDatabaseHelper;
import ntu.nguyenhoangphuc.personal_diary.model.DiaryEntry;

/**
 * Màn Thêm/Sửa 1 bài nhật ký - dùng CHUNG 1 Activity cho cả 2 chế độ,
 * phân biệt bằng có truyền EXTRA_DIARY_ID qua Intent hay không.
 *
 * PHẦN 1 (đang viết): nội dung, ngày, mood, gợi ý viết, tag, lưu/tải DB.
 * CHƯA LÀM (Phần 2, 3): ảnh, giọng nói, share, TTS - 4 nút đó đã gắn sẵn
 * OnClickListener rỗng (TODO) để không còn bị hiểu lầm "nút tĩnh".
 */
public class AddEditDiaryActivity extends AppCompatActivity {

    // Key truyền id bài nhật ký qua Intent khi mở màn này ở chế độ SỬA.
    // HomeFragment sẽ dùng đúng hằng số này khi gọi intent.putExtra(...)
    public static final String EXTRA_DIARY_ID = "extra_diary_id";

    // Intent KHÔNG có EXTRA_DIARY_ID -> hiểu là chế độ THÊM MỚI
    private static final int KHONG_CO_ID = -1;

    // ===== View =====
    private ImageButton btnQuayLai, btnLuu, btnChiaSe, btnDocTo, btnMic, btnDoiCauGoiY, btnThemAnh;
    private TextView tvTieuDe, tvNgayThang, tvGoiY;
    private EditText edtNoiDung, edtNhapTagMoi;
    private LinearLayout rowNgayThang;
    private ImageView ivMoodHappy, ivMoodCalm, ivMoodSad, ivMoodAngry, ivMoodNeutral;
    private ChipGroup cgTagGoiY;

    // ===== Dữ liệu / trạng thái =====
    private DiaryDatabaseHelper dbHelper;

    // -1 = đang Thêm mới, khác -1 = đang Sửa bài có id này
    private int diaryIdDangSua = KHONG_CO_ID;

    // Giữ lại ngay_tao gốc khi Sửa bài, để lưu xuống DB không mất thời điểm tạo bài ban đầu
    private String ngayTaoGocDaTai;

    // Ngày đang chọn, lưu sẵn ở định dạng yyyy-MM-dd (đúng định dạng cột ngay_thang trong DB)
    private String ngayDaChonYyyyMMdd;

    // Mã tâm trạng đang chọn ("happy"/"calm"/"sad"/"angry"/"neutral"), null nếu không chọn
    private String tamTrangDaChon;

    // ImageView mood nào đang setSelected(true) - dùng để bỏ chọn nó khi mày chọn mood khác
    private ImageView moodDangChon;

    private List<String> danhSachGoiY;
    private final Random ngauNhien = new Random();

    // dinhDangLuu: định dạng lưu DB. dinhDangHienThi: định dạng hiện lên màn hình cho dễ đọc
    // (giống hệt cách DiaryAdapter đang làm, để đồng bộ toàn app)
    private final SimpleDateFormat dinhDangLuu = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat dinhDangHienThi = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_diary);

        dbHelper = new DiaryDatabaseHelper(this);

        anhXaView();
        khoiTaoGoiY();
        ganSuKien();

        diaryIdDangSua = getIntent().getIntExtra(EXTRA_DIARY_ID, KHONG_CO_ID);

        if (diaryIdDangSua != KHONG_CO_ID) {
            // Chế độ SỬA - tải dữ liệu bài cũ lên màn hình
            tvTieuDe.setText(R.string.tieu_de_sua);
            taiDuLieuBaiCu();
        } else {
            // Chế độ THÊM MỚI - mặc định chọn sẵn ngày hôm nay cho tiện, mày vẫn đổi được
            tvTieuDe.setText(R.string.tieu_de_them);
            Calendar homNay = Calendar.getInstance();
            ngayDaChonYyyyMMdd = dinhDangLuu.format(homNay.getTime());
            tvNgayThang.setText(dinhDangHienThi.format(homNay.getTime()));
        }
    }

    private void anhXaView() {
        btnQuayLai = findViewById(R.id.btnQuayLai);
        tvTieuDe = findViewById(R.id.tvTieuDe);
        btnLuu = findViewById(R.id.btnLuu);
        btnChiaSe = findViewById(R.id.btnChiaSe);
        btnDocTo = findViewById(R.id.btnDocTo);
        btnMic = findViewById(R.id.btnMic);

        edtNoiDung = findViewById(R.id.edtNoiDung);

        rowNgayThang = findViewById(R.id.rowNgayThang);
        tvNgayThang = findViewById(R.id.tvNgayThang);

        ivMoodHappy = findViewById(R.id.ivMoodHappy);
        ivMoodCalm = findViewById(R.id.ivMoodCalm);
        ivMoodSad = findViewById(R.id.ivMoodSad);
        ivMoodAngry = findViewById(R.id.ivMoodAngry);
        ivMoodNeutral = findViewById(R.id.ivMoodNeutral);

        tvGoiY = findViewById(R.id.tvGoiY);
        btnDoiCauGoiY = findViewById(R.id.btnDoiCauGoiY);

        cgTagGoiY = findViewById(R.id.cgTagGoiY);
        edtNhapTagMoi = findViewById(R.id.edtNhapTagMoi);

        btnThemAnh = findViewById(R.id.btnThemAnh);
    }

    // Đọc string-array gợi ý viết trong strings.xml ra List cho dễ random
    private void khoiTaoGoiY() {
        String[] mang = getResources().getStringArray(R.array.danh_sach_goi_y_viet);
        danhSachGoiY = Arrays.asList(mang);
    }

    private void ganSuKien() {
        btnQuayLai.setOnClickListener(v -> finish());
        btnLuu.setOnClickListener(v -> luuNhatKy());

        // 4 nút này thuộc Phần 2/3 (ảnh, giọng nói, share, TTS) - CHƯA làm ở Phần 1,
        // gắn sẵn listener rỗng để không còn bị hiểu lầm "nút tĩnh" như mày report lúc nãy
        btnThemAnh.setOnClickListener(v -> {
            // TODO: làm ở Phần 2 - mở bottom sheet chọn Thư viện ảnh / Chụp ảnh mới
        });
        btnMic.setOnClickListener(v -> {
            // TODO: làm ở Phần 3 - RecognizerIntent, nối chữ nhận diện vào cuối edtNoiDung
        });
        btnChiaSe.setOnClickListener(v -> {
            // TODO: làm ở Phần 3 - Intent.ACTION_SEND chia sẻ nội dung bài viết
        });
        btnDocTo.setOnClickListener(v -> {
            // TODO: làm ở Phần 3 - TextToSpeech đọc nội dung edtNoiDung
        });

        rowNgayThang.setOnClickListener(v -> moChonNgay());

        ivMoodHappy.setOnClickListener(v -> xuLyBamMood(ivMoodHappy, "happy"));
        ivMoodCalm.setOnClickListener(v -> xuLyBamMood(ivMoodCalm, "calm"));
        ivMoodSad.setOnClickListener(v -> xuLyBamMood(ivMoodSad, "sad"));
        ivMoodAngry.setOnClickListener(v -> xuLyBamMood(ivMoodAngry, "angry"));
        ivMoodNeutral.setOnClickListener(v -> xuLyBamMood(ivMoodNeutral, "neutral"));

        btnDoiCauGoiY.setOnClickListener(v -> doiGoiYNgauNhien());

        // IME_ACTION_DONE = sự kiện bấm nút "Xong" trên bàn phím ảo, ứng với
        // imeOptions="actionDone" đã khai trong XML của edtNhapTagMoi
        edtNhapTagMoi.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String tenTagMoi = edtNhapTagMoi.getText().toString().trim();
                if (!tenTagMoi.isEmpty()) {
                    themTagMoi(tenTagMoi);
                    edtNhapTagMoi.setText("");
                }
                return true; // true = đã tự xử lý xong sự kiện này
            }
            return false;
        });
    }

    // ===================== NGÀY THÁNG =====================

    // DatePickerDialog = hộp thoại chọn ngày có sẵn của Android
    private void moChonNgay() {
        Calendar lich = Calendar.getInstance();
        if (ngayDaChonYyyyMMdd != null) {
            try {
                lich.setTime(dinhDangLuu.parse(ngayDaChonYyyyMMdd));
            } catch (ParseException e) {
                // Không parse được thì cứ giữ lịch hiện tại (hôm nay), không crash app
            }
        }

        int nam = lich.get(Calendar.YEAR);
        int thang = lich.get(Calendar.MONTH);
        int ngay = lich.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, namChon, thangChon, ngayChon) -> {
            Calendar ngayMoiChon = Calendar.getInstance();
            ngayMoiChon.set(namChon, thangChon, ngayChon);
            ngayDaChonYyyyMMdd = dinhDangLuu.format(ngayMoiChon.getTime());
            tvNgayThang.setText(dinhDangHienThi.format(ngayMoiChon.getTime()));
        }, nam, thang, ngay);

        dialog.show();
    }

    // ===================== MOOD =====================

    // Bấm vào 1 icon mood: đang chọn sẵn icon đó -> bấm lại để BỎ chọn (mood optional).
    // Chọn icon khác -> bỏ chọn icon cũ, chọn icon mới. setSelected(true/false) tự đổi
    // hình theo đúng selector_mood_*.xml (2 trạng thái: chọn/không chọn).
    private void xuLyBamMood(ImageView iconDuocBam, String maMood) {
        if (moodDangChon == iconDuocBam) {
            iconDuocBam.setSelected(false);
            moodDangChon = null;
            tamTrangDaChon = null;
            return;
        }

        if (moodDangChon != null) {
            moodDangChon.setSelected(false);
        }
        iconDuocBam.setSelected(true);
        moodDangChon = iconDuocBam;
        tamTrangDaChon = maMood;
    }

    // Dùng riêng khi TẢI dữ liệu bài cũ lên (chế độ Sửa) - set thẳng, không cần logic toggle
    private void chonMoodBanDau(String maMood) {
        ImageView iconTuongUng;
        switch (maMood) {
            case "happy":
                iconTuongUng = ivMoodHappy;
                break;
            case "calm":
                iconTuongUng = ivMoodCalm;
                break;
            case "sad":
                iconTuongUng = ivMoodSad;
                break;
            case "angry":
                iconTuongUng = ivMoodAngry;
                break;
            case "neutral":
            default:
                iconTuongUng = ivMoodNeutral;
                break;
        }
        iconTuongUng.setSelected(true);
        moodDangChon = iconTuongUng;
        tamTrangDaChon = maMood;
    }

    // ===================== GỢI Ý VIẾT =====================

    private void doiGoiYNgauNhien() {
        if (danhSachGoiY.isEmpty()) return;
        int viTri = ngauNhien.nextInt(danhSachGoiY.size());
        tvGoiY.setText(danhSachGoiY.get(viTri));
    }

    // ===================== TAG =====================

    // Đã có sẵn tag này (kể cả 5 tag dựng sẵn trong XML) -> chỉ tick chọn lên.
    // Chưa có -> tạo Chip mới có nút "x" để xoá (vì là tag mày tự gõ, không cố định)
    private void themTagMoi(String tenTag) {
        for (int i = 0; i < cgTagGoiY.getChildCount(); i++) {
            View con = cgTagGoiY.getChildAt(i);
            if (con instanceof Chip) {
                Chip chipDaCo = (Chip) con;
                if (chipDaCo.getText().toString().trim().equalsIgnoreCase(tenTag)) {
                    chipDaCo.setChecked(true);
                    return;
                }
            }
        }

        Chip chipMoi = new Chip(this);
        chipMoi.setText(tenTag);
        chipMoi.setCheckable(true);
        chipMoi.setChecked(true);
        chipMoi.setTextColor(ContextCompat.getColor(this, R.color.color_chu_chinh));
        chipMoi.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.chip_bg_selector));
        chipMoi.setChipStrokeColor(ContextCompat.getColorStateList(this, R.color.chip_stroke_selector));
        chipMoi.setChipStrokeWidth(dpSangPx(1.5f));
        chipMoi.setCheckedIconVisible(true);
        chipMoi.setCheckedIconTint(ContextCompat.getColorStateList(this, R.color.color_nhan));
        // closeIcon = icon "x" nhỏ để xoá riêng Chip này - chỉ Chip tự gõ mới có
        chipMoi.setCloseIconVisible(true);
        chipMoi.setOnCloseIconClickListener(v -> cgTagGoiY.removeView(chipMoi));

        cgTagGoiY.addView(chipMoi);
    }

    // Gom Chip đang tick chọn thành chuỗi phân cách dấu phẩy, đúng quy ước cột the_gan
    // (vd "Công việc,Gia đình"). Không Chip nào chọn -> null.
    private String layTagDangChon() {
        StringBuilder ketQua = new StringBuilder();
        for (int i = 0; i < cgTagGoiY.getChildCount(); i++) {
            View con = cgTagGoiY.getChildAt(i);
            if (con instanceof Chip) {
                Chip chip = (Chip) con;
                if (chip.isChecked()) {
                    if (ketQua.length() > 0) {
                        ketQua.append(",");
                    }
                    ketQua.append(chip.getText().toString().trim());
                }
            }
        }
        return ketQua.length() > 0 ? ketQua.toString() : null;
    }

    // ===================== LƯU / TẢI DB =====================

    private void taiDuLieuBaiCu() {
        DiaryEntry entry = dbHelper.getDiaryById(diaryIdDangSua);
        if (entry == null) {
            Toast.makeText(this, "Không tìm thấy bài nhật ký này", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ngayTaoGocDaTai = entry.getNgayTao();

        edtNoiDung.setText(entry.getNoiDung());

        ngayDaChonYyyyMMdd = entry.getNgayThang();
        try {
            tvNgayThang.setText(dinhDangHienThi.format(dinhDangLuu.parse(ngayDaChonYyyyMMdd)));
        } catch (ParseException e) {
            tvNgayThang.setText(ngayDaChonYyyyMMdd);
        }

        if (entry.getTamTrang() != null && !entry.getTamTrang().isEmpty()) {
            chonMoodBanDau(entry.getTamTrang());
        }

        if (entry.getTheGan() != null && !entry.getTheGan().trim().isEmpty()) {
            for (String tag : entry.getTheGan().split(",")) {
                String tagSach = tag.trim();
                if (!tagSach.isEmpty()) {
                    themTagMoi(tagSach);
                }
            }
        }
    }

    private void luuNhatKy() {
        String noiDung = edtNoiDung.getText().toString().trim();
        if (noiDung.isEmpty()) {
            Toast.makeText(this, "Bạn chưa viết nội dung gì cả", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ngayDaChonYyyyMMdd == null) {
            ngayDaChonYyyyMMdd = dinhDangLuu.format(Calendar.getInstance().getTime());
        }

        String theGan = layTagDangChon();

        if (diaryIdDangSua == KHONG_CO_ID) {
            // Chế độ THÊM MỚI - sinh timestamp lúc tạo bài ngay bây giờ
            String ngayTaoHienTai = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Calendar.getInstance().getTime());
            DiaryEntry baiMoi = new DiaryEntry(ngayDaChonYyyyMMdd, noiDung, tamTrangDaChon, theGan, ngayTaoHienTai);
            dbHelper.insertDiary(baiMoi);
        } else {
            // Chế độ SỬA - giữ nguyên ngay_tao gốc, không ghi đè thời điểm tạo bài
            DiaryEntry baiDaSua = new DiaryEntry(diaryIdDangSua, ngayDaChonYyyyMMdd, noiDung, tamTrangDaChon, theGan, ngayTaoGocDaTai);
            dbHelper.updateDiary(baiDaSua);
        }

        Toast.makeText(this, "Đã lưu nhật ký", Toast.LENGTH_SHORT).show();
        // setResult(RESULT_OK) báo cho màn gọi mình (HomeFragment) biết là đã lưu thành công
        setResult(RESULT_OK);
        finish();
    }

    // Đổi dp sang px - giống hệt hàm dpSangPx trong DiaryAdapter, cần vì set style Chip
    // bằng code (setChipStrokeWidth...) chỉ nhận đơn vị px, không tự hiểu "dp" như XML
    private int dpSangPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}

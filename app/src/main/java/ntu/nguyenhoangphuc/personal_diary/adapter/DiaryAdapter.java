package ntu.nguyenhoangphuc.personal_diary.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.database.DiaryDatabaseHelper;
import ntu.nguyenhoangphuc.personal_diary.model.DiaryEntry;
import ntu.nguyenhoangphuc.personal_diary.model.DiaryPhoto;

/**
 * Adapter cho RecyclerView (RecyclerView = danh sách cuộn được, tái sử dụng
 * View thay vì tạo mới liên tục để đỡ tốn bộ nhớ) ở HomeFragment.
 * Nhiệm vụ: biến từng DiaryEntry thành 1 item_diary_entry.xml hiển thị.
 *
 * ⚠️ 1 ĐIỂM CÒN CHỜ XÁC NHẬN: the_gan lưu nhiều tag cách nhau bằng dấu phẩy
 * (vd "Công việc,Gia đình") - đã chốt là "cả 2" (chọn sẵn + gõ thêm) ở
 * AddEditDiaryActivity, miễn lúc lưu vẫn ghép lại thành 1 chuỗi phẩy là khớp.
 * (Mood - 5 icon đã đủ, xong rồi, không còn TODO nữa.)
 */
public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

    // Interface để HomeFragment biết khi nào 1 item bị bấm (dùng mở màn sửa bài sau này)
    public interface OnItemClickListener {
        void onItemClick(DiaryEntry entry);
    }

    // MỚI - interface để Fragment biết khi nào 1 item bị GIỮ LÂU (long-press),
    // dùng để mở dialog xác nhận xóa - khác hẳn OnItemClickListener ở trên
    // (bấm thường -> mở màn Sửa)
    public interface OnItemLongClickListener {
        void onItemLongClick(DiaryEntry entry);
    }

    private final Context context;
    private final DiaryDatabaseHelper dbHelper;
    private List<DiaryEntry> danhSachNhatKy;
    private OnItemClickListener itemClickListener;
    private OnItemLongClickListener itemLongClickListener;

    // dinhDangLuu: định dạng ngày lưu trong DB (yyyy-MM-dd)
    // dinhDangHienThi: định dạng hiển thị ra màn hình cho dễ đọc (dd/MM/yyyy)
    private final SimpleDateFormat dinhDangLuu = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat dinhDangHienThi = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public DiaryAdapter(Context context, List<DiaryEntry> danhSachNhatKy, DiaryDatabaseHelper dbHelper) {
        this.context = context;
        this.danhSachNhatKy = danhSachNhatKy;
        this.dbHelper = dbHelper;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.itemLongClickListener = listener;
    }

    // Gọi hàm này mỗi khi có dữ liệu mới (sau khi thêm/sửa/xoá) để RecyclerView vẽ lại
    public void capNhatDanhSach(List<DiaryEntry> danhSachMoi) {
        this.danhSachNhatKy = danhSachMoi;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_diary_entry, parent, false);
        return new DiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaryViewHolder holder, int position) {
        DiaryEntry entry = danhSachNhatKy.get(position);

        // 1. Nội dung rút gọn - XML đã có maxLines="2" + ellipsize="end" nên chỉ cần setText
        holder.textExcerpt.setText(entry.getNoiDung());

        // 2. Ngày tháng - đổi từ yyyy-MM-dd (lưu DB) sang dd/MM/yyyy (dễ đọc)
        holder.textDate.setText(dinhDangNgay(entry.getNgayThang()));

        // 3. Tag/nhãn - tách chuỗi the_gan, addView từng pill vào tags_container
        ganTagVaoContainer(holder, entry.getTheGan());

        // 4. Mood - ẩn nếu không chọn, hiện icon tương ứng nếu có
        ganMoodVaoBadge(holder, entry.getTamTrang());

        // 5. Ảnh thumbnail + số lượng ảnh
        ganAnhThumbnail(holder, entry.getId());

        // 6. Bấm vào item -> báo ra ngoài cho HomeFragment xử lý (mở màn sửa bài)
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(entry);
            }
        });

        // MỚI - 7. Giữ lâu (long-press) vào item -> báo ra ngoài cho Fragment
        // xử lý (mở dialog xác nhận xóa). return true để báo đã "tiêu thụ"
        // xong sự kiện long-click này - chặn không cho nó lan tiếp thành 1
        // sự kiện click thường ngay sau khi nhả tay ra (nếu trả về false,
        // có thể vô tình mở nhầm màn Sửa ngay sau khi vừa long-press)
        holder.itemView.setOnLongClickListener(v -> {
            if (itemLongClickListener != null) {
                itemLongClickListener.onItemLongClick(entry);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return danhSachNhatKy.size();
    }

    // ===================== CÁC HÀM PHỤ =====================

    // Nếu ngày lưu DB bị sai định dạng (hiếm khi xảy ra) thì hiện nguyên chuỗi gốc, không crash app
    private String dinhDangNgay(String ngayLuuDB) {
        if (ngayLuuDB == null || ngayLuuDB.isEmpty()) {
            return "";
        }
        try {
            return dinhDangHienThi.format(dinhDangLuu.parse(ngayLuuDB));
        } catch (ParseException e) {
            return ngayLuuDB;
        }
    }

    // Tách chuỗi tag cách nhau bằng dấu phẩy, addView từng TextView pill vào tags_container
    private void ganTagVaoContainer(DiaryViewHolder holder, String theGan) {
        holder.tagsContainer.removeAllViews(); // xoá pill cũ trước (RecyclerView tái sử dụng view cũ)

        if (theGan == null || theGan.trim().isEmpty()) {
            holder.tagsContainer.setVisibility(View.GONE);
            return;
        }

        holder.tagsContainer.setVisibility(View.VISIBLE);
        String[] danhSachTag = theGan.split(",");

        for (String tag : danhSachTag) {
            String tagSach = tag.trim();
            if (tagSach.isEmpty()) continue;

            TextView pill = new TextView(context);
            pill.setText(tagSach);
            pill.setTextSize(10);
            pill.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            pill.setBackgroundResource(R.drawable.bg_pill_tag);

            int paddingNgang = dpSangPx(8);
            int paddingDoc = dpSangPx(2);
            pill.setPadding(paddingNgang, paddingDoc, paddingNgang, paddingDoc);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dpSangPx(4));
            pill.setLayoutParams(params);

            holder.tagsContainer.addView(pill);
        }
    }

    // Gán icon mood - đủ 5 nhánh rồi (trước chỉ có 2).
    // Tô màu icon theo đúng 5 màu mood_happy/calm/sad/angry/neutral có sẵn trong colors.xml.
    // Nếu mày KHÔNG muốn tô màu (giữ icon đen trắng thôi) thì xoá dòng setColorFilter là được.
    private void ganMoodVaoBadge(DiaryViewHolder holder, String tamTrang) {
        if (tamTrang == null || tamTrang.isEmpty()) {
            holder.badgeMood.setVisibility(View.GONE);
            return;
        }

        holder.badgeMood.setVisibility(View.VISIBLE);

        int drawableRes;
        int colorRes;
        switch (tamTrang) {
            case "happy":
                drawableRes = R.drawable.ic_mood_smile;
                colorRes = R.color.mood_happy;
                break;
            case "calm":
                drawableRes = R.drawable.ic_mood_calm;
                colorRes = R.color.mood_calm;
                break;
            case "sad":
                drawableRes = R.drawable.ic_mood_sad;
                colorRes = R.color.mood_sad;
                break;
            case "angry":
                drawableRes = R.drawable.ic_mood_angry;
                colorRes = R.color.mood_angry;
                break;
            case "neutral":
            default:
                drawableRes = R.drawable.ic_mood_neutral;
                colorRes = R.color.mood_neutral;
                break;
        }

        holder.badgeMood.setImageResource(drawableRes);
        holder.badgeMood.setColorFilter(ContextCompat.getColor(context, colorRes));
    }

    // Lấy ảnh đầu tiên (thu_tu = 0) làm thumbnail + hiện số lượng ảnh nếu > 1.
    // Query DB ngay trong onBindViewHolder là cách đơn giản nhất cho project này -
    // đủ nhanh vì số bài không quá nhiều. Có thể tối ưu sau bằng cách preload hết
    // ảnh 1 lần thay vì query lại mỗi lần cuộn (để dành làm sau, không cần vội).
    private void ganAnhThumbnail(DiaryViewHolder holder, int diaryId) {
        List<DiaryPhoto> danhSachAnh = dbHelper.getPhotosForDiary(diaryId);

        if (danhSachAnh.isEmpty()) {
            holder.thumb.setImageDrawable(null);
            holder.badgePhotoCount.setVisibility(View.GONE);
            return;
        }

        String duongDanAnhDau = danhSachAnh.get(0).getDuongDanAnh();
        int kichThuocPx = dpSangPx(42); // đúng bằng layout_width/height="42dp" của thumb trong XML
        Bitmap bitmap = giaiMaAnhGonNhe(duongDanAnhDau, kichThuocPx, kichThuocPx);
        holder.thumb.setImageBitmap(bitmap); // bitmap null cũng không sao, ImageView tự xoá ảnh cũ

        if (danhSachAnh.size() > 1) {
            holder.badgePhotoCount.setVisibility(View.VISIBLE);
            holder.badgePhotoCount.setText(String.valueOf(danhSachAnh.size()));
        } else {
            holder.badgePhotoCount.setVisibility(View.GONE);
        }
    }

    // Giải mã ảnh với kích thước thu nhỏ sẵn (tránh OOM - lỗi hết bộ nhớ hay gặp
    // khi decode ảnh gốc full-size chỉ để hiện thumbnail nhỏ xíu 42dp).
    // Theo đúng pattern chính hãng Android - xem link tham khảo cuối tin nhắn trước.
    private Bitmap giaiMaAnhGonNhe(String duongDan, int reqWidth, int reqHeight) {
        File file = new File(duongDan);
        if (!file.exists()) {
            return null; // file ảnh không còn tồn tại (vd người dùng xoá ảnh khỏi máy) - không crash
        }

        // Bước 1: chỉ đọc kích thước ảnh, CHƯA load ảnh thật vào bộ nhớ
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(duongDan, options);

        // Bước 2: tính tỉ lệ thu nhỏ (inSampleSize) dựa trên kích thước thật vs kích thước cần
        options.inSampleSize = tinhTiLeThuNho(options, reqWidth, reqHeight);

        // Bước 3: decode thật với tỉ lệ đã tính - ảnh trong bộ nhớ giờ nhỏ hơn nhiều so với ảnh gốc
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(duongDan, options);
    }

    private int tinhTiLeThuNho(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int chieuCaoGoc = options.outHeight;
        int chieuRongGoc = options.outWidth;
        int tiLe = 1;

        if (chieuCaoGoc > reqHeight || chieuRongGoc > reqWidth) {
            int nuaChieuCao = chieuCaoGoc / 2;
            int nuaChieuRong = chieuRongGoc / 2;

            while ((nuaChieuCao / tiLe) >= reqHeight && (nuaChieuRong / tiLe) >= reqWidth) {
                tiLe *= 2;
            }
        }
        return tiLe;
    }

    // Đổi dp sang px - cần vì layout XML dùng "dp" nhưng code Java chỉ hiểu "px"
    private int dpSangPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    // ===================== VIEWHOLDER =====================
    // ViewHolder = nơi "giữ" sẵn tham chiếu tới các View trong item_diary_entry.xml,
    // để không phải gọi findViewById lặp lại mỗi lần cuộn (findViewById khá tốn công)

    static class DiaryViewHolder extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView badgePhotoCount;
        ImageView badgeMood;
        TextView textExcerpt;
        LinearLayout tagsContainer;
        TextView textDate;

        DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);
            badgePhotoCount = itemView.findViewById(R.id.badge_photo_count);
            badgeMood = itemView.findViewById(R.id.badge_mood);
            textExcerpt = itemView.findViewById(R.id.text_excerpt);
            tagsContainer = itemView.findViewById(R.id.tags_container);
            textDate = itemView.findViewById(R.id.text_date);
        }
    }
}
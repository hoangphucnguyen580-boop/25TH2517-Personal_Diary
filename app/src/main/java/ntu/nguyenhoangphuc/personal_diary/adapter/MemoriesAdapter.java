package ntu.nguyenhoangphuc.personal_diary.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.util.List;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.model.AnhKyNiem;

/**
 * Adapter cho rvKyNiem (MemoriesFragment) - lưới ảnh TRỘN 2 loại item: header
 * tháng/năm (chiếm hết hàng qua GridLayoutManager.SpanSizeLookup, set bên
 * Fragment) và ô ảnh (chiếm 1 cột). Xem HUONG_DAN_KY_NIEM.md mục 4 để rõ lý do
 * kỹ thuật của từng lựa chọn XML.
 */
public class MemoriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int LOAI_HEADER = 0;
    private static final int LOAI_ANH = 1;

    // Kích thước decode ảnh - ước lượng đủ nét cho 1 ô lưới 3 cột, không cần
    // chính xác tuyệt đối vì hàm giải mã bên dưới chỉ làm tròn theo luỹ thừa 2
    private static final int KICH_THUOC_THUMB_DP = 150;

    // Đại diện cho ĐÚNG 1 trong 2 loại item hiển thị trong rvKyNiem - hoặc là
    // header tháng, hoặc là 1 ảnh, KHÔNG BAO GIỜ cả 2 field cùng khác null 1
    // lúc. Cùng tinh thần với DayAdapter.NgayLich đang phân biệt ô rỗng/ô có
    // ngày trong CalendarFragment.
    public static class MucHienThi {
        private final String tieuDeThang; // null nếu đây là item ảnh
        private final AnhKyNiem anhKyNiem; // null nếu đây là item header

        public static MucHienThi taoHeader(String tieuDeThang) {
            return new MucHienThi(tieuDeThang, null);
        }

        public static MucHienThi taoAnh(AnhKyNiem anhKyNiem) {
            return new MucHienThi(null, anhKyNiem);
        }

        private MucHienThi(String tieuDeThang, AnhKyNiem anhKyNiem) {
            this.tieuDeThang = tieuDeThang;
            this.anhKyNiem = anhKyNiem;
        }

        public boolean laHeader() {
            return tieuDeThang != null;
        }
    }

    // Interface để MemoriesFragment biết khi nào 1 ảnh bị bấm (mở bài viết gốc)
    public interface OnAnhClickListener {
        void onAnhClick(AnhKyNiem anhKyNiem);
    }

    private final Context context;
    private List<MucHienThi> danhSachHienThi;
    private OnAnhClickListener anhClickListener;

    public MemoriesAdapter(Context context, List<MucHienThi> danhSachHienThi) {
        this.context = context;
        this.danhSachHienThi = danhSachHienThi;
    }

    public void setOnAnhClickListener(OnAnhClickListener listener) {
        this.anhClickListener = listener;
    }

    // Gọi mỗi khi có dữ liệu mới (sau khi thêm/xoá ảnh ở màn khác) để vẽ lại
    public void capNhatDanhSach(List<MucHienThi> danhSachMoi) {
        this.danhSachHienThi = danhSachMoi;
        notifyDataSetChanged();
    }

    // Dùng cho GridLayoutManager.SpanSizeLookup bên MemoriesFragment - true nếu
    // vị trí này là header (cần chiếm hết hàng, không phải 1 ô ảnh bình thường)
    public boolean laHeaderTaiViTri(int position) {
        return danhSachHienThi.get(position).laHeader();
    }

    @Override
    public int getItemViewType(int position) {
        return danhSachHienThi.get(position).laHeader() ? LOAI_HEADER : LOAI_ANH;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == LOAI_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_tieu_de_thang, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_anh_ky_niem, parent, false);
        return new AnhViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MucHienThi muc = danhSachHienThi.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvTieuDeThang.setText(muc.tieuDeThang);
            return;
        }

        AnhViewHolder anhHolder = (AnhViewHolder) holder;
        AnhKyNiem anh = muc.anhKyNiem;

        int kichThuocPx = dpSangPx(KICH_THUOC_THUMB_DP);
        Bitmap bitmap = giaiMaAnhGonNhe(anh.getDuongDanAnh(), kichThuocPx, kichThuocPx);
        anhHolder.ivAnhKyNiem.setImageBitmap(bitmap); // null cũng không sao, tự xoá ảnh cũ

        anhHolder.itemView.setOnClickListener(v -> {
            if (anhClickListener != null) {
                anhClickListener.onAnhClick(anh);
            }
        });
    }

    @Override
    public int getItemCount() {
        return danhSachHienThi.size();
    }

    // ===================== CÁC HÀM PHỤ =====================

    // Giải mã ảnh với kích thước thu nhỏ sẵn - GIỐNG HỆT pattern
    // DiaryAdapter.giaiMaAnhGonNhe() đang dùng, tránh OOM khi decode ảnh gốc
    // full-size chỉ để hiện thumbnail nhỏ trong ô lưới
    private Bitmap giaiMaAnhGonNhe(String duongDan, int reqWidth, int reqHeight) {
        File file = new File(duongDan);
        if (!file.exists()) {
            return null; // file ảnh không còn tồn tại - không crash
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(duongDan, options);

        options.inSampleSize = tinhTiLeThuNho(options, reqWidth, reqHeight);

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

    private int dpSangPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    // ===================== VIEWHOLDER =====================

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTieuDeThang;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            // item_tieu_de_thang.xml chỉ có 1 TextView làm root duy nhất, nên ép
            // kiểu thẳng - giống hệt cách PhotoViewerAdapter.PhotoViewHolder đang
            // làm với ZoomableImageView
            tvTieuDeThang = (TextView) itemView;
        }
    }

    static class AnhViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAnhKyNiem;

        AnhViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAnhKyNiem = itemView.findViewById(R.id.ivAnhKyNiem);
        }
    }
}
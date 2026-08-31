package ntu.nguyenhoangphuc.personal_diary.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ntu.nguyenhoangphuc.personal_diary.R;

/**
 * Adapter cho lưới lịch tháng (rvLichThang trong CalendarFragment) - mỗi ô là
 * 1 item_o_ngay_lich.xml. Data class NgayLich giữ đủ thông tin để bind: số
 * ngày hiện lên, có phải ô rỗng (đệm đầu tháng cho đúng cột thứ) không, có
 * bài viết không, có đang được tô nổi bật không (hôm nay/đang chọn - GỘP
 * CHUNG 1 kiểu tô ở bản MVP, đúng như UI mentor đã chốt trong XML).
 */
public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    public static class NgayLich {
        public final int soNgay;            // 0 = ô rỗng (đệm đầu tháng)
        public final String ngayThangDayDu; // yyyy-MM-dd, null nếu ô rỗng
        public final boolean coBai;
        public final boolean dangNoiBat;    // hôm nay HOẶC đang được chọn

        public NgayLich(int soNgay, String ngayThangDayDu, boolean coBai, boolean dangNoiBat) {
            this.soNgay = soNgay;
            this.ngayThangDayDu = ngayThangDayDu;
            this.coBai = coBai;
            this.dangNoiBat = dangNoiBat;
        }

        public boolean laOTrong() {
            return ngayThangDayDu == null;
        }
    }

    public interface OnNgayClickListener {
        void onNgayClick(NgayLich ngay);
    }

    private List<NgayLich> danhSachNgay;
    private OnNgayClickListener clickListener;

    public DayAdapter(List<NgayLich> danhSachNgay) {
        this.danhSachNgay = danhSachNgay;
    }

    public void setOnNgayClickListener(OnNgayClickListener listener) {
        this.clickListener = listener;
    }

    // Gọi mỗi khi đổi tháng HOẶC đổi ngày đang chọn - vẽ lại toàn bộ lưới
    public void capNhatDanhSach(List<NgayLich> danhSachMoi) {
        this.danhSachNgay = danhSachMoi;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_o_ngay_lich, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        NgayLich ngay = danhSachNgay.get(position);

        if (ngay.laOTrong()) {
            // Ô đệm đầu tháng - ẩn hết, không cho bấm. PHẢI xoá listener cũ
            // (nếu RecyclerView tái sử dụng đúng View này từ 1 ô CÓ ngày trước
            // đó), không thì bấm vào ô rỗng vẫn kích hoạt callback của ngày cũ.
            holder.tvSoNgay.setText("");
            holder.nenTrangThaiNgay.setVisibility(View.GONE);
            holder.chamCoBai.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
            return;
        }

        holder.tvSoNgay.setText(String.valueOf(ngay.soNgay));

        if (ngay.dangNoiBat) {
            holder.nenTrangThaiNgay.setVisibility(View.VISIBLE);
            // Nền terracotta đặc -> chữ phải sáng lên mới đủ tương phản, đúng
            // lưu ý UI mentor để lại trong comment XML (dùng on_accent - đúng
            // token ngữ nghĩa "chữ trên nền màu nhấn", không dùng bg_main dù
            // 2 màu trùng giá trị)
            holder.tvSoNgay.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.on_accent));
        } else {
            // BẮT BUỘC reset về màu mặc định - tránh trường hợp View bị tái sử
            // dụng từ 1 ô ĐANG nổi bật trước đó, giữ nhầm màu chữ sáng khiến số
            // ngày biến mất trên nền trắng
            holder.nenTrangThaiNgay.setVisibility(View.GONE);
            holder.tvSoNgay.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.text_primary));
        }

        holder.chamCoBai.setVisibility(ngay.coBai ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onNgayClick(ngay);
            }
        });
    }

    @Override
    public int getItemCount() {
        return danhSachNgay.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        View nenTrangThaiNgay;
        TextView tvSoNgay;
        View chamCoBai;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            nenTrangThaiNgay = itemView.findViewById(R.id.nenTrangThaiNgay);
            tvSoNgay = itemView.findViewById(R.id.tvSoNgay);
            chamCoBai = itemView.findViewById(R.id.chamCoBai);
        }
    }
}

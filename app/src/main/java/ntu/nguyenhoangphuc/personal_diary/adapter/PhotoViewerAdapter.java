package ntu.nguyenhoangphuc.personal_diary.adapter;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.widget.ZoomableImageView;

/**
 * Adapter cho ViewPager2 trong PhotoViewerDialog (Phần 2.3) - mỗi trang là 1
 * ảnh phóng to, dùng ZoomableImageView (widget tự viết) để hỗ trợ pinch-zoom.
 */
public class PhotoViewerAdapter extends RecyclerView.Adapter<PhotoViewerAdapter.PhotoViewHolder> {

    private final List<String> danhSachDuongDan;

    public PhotoViewerAdapter(List<String> danhSachDuongDan) {
        this.danhSachDuongDan = danhSachDuongDan;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trang_xem_anh, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String duongDan = danhSachDuongDan.get(position);

        // Ảnh đã được resize + nén sẵn từ lúc thêm vào dải ảnh (tối đa cạnh dài
        // 1280px, JPEG 85%) nên decode trực tiếp full-size là đủ nhanh, không
        // cần downsample thêm như lúc hiện thumbnail nhỏ 42dp/96dp
        holder.zoomImageView.setImageBitmap(BitmapFactory.decodeFile(duongDan));

        // Reset lại trạng thái zoom mỗi lần bind - tránh trường hợp RecyclerView
        // tái sử dụng View đã bị zoom to từ ảnh trước, khiến ảnh mới hiện ra
        // cũng bị zoom sẵn luôn
        holder.zoomImageView.resetZoom();
    }

    @Override
    public int getItemCount() {
        return danhSachDuongDan.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ZoomableImageView zoomImageView;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            // Layout item_trang_xem_anh.xml có ZoomableImageView là root duy
            // nhất, nên ép kiểu thẳng thay vì findViewById cho dễ hiểu
            zoomImageView = (ZoomableImageView) itemView;
        }
    }
}
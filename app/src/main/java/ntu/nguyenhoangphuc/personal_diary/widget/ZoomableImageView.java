package ntu.nguyenhoangphuc.personal_diary.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewParent;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * ImageView tự viết, dùng trong PhotoViewerDialog (Phần 2.3 - xem ảnh phóng to).
 * Hỗ trợ:
 *  - Pinch 2 ngón để phóng to/thu nhỏ (ScaleGestureDetector)
 *  - Kéo (pan) ảnh khi đang phóng to (GestureDetector.onScroll)
 *  - Double-tap để phóng to nhanh / thu về lại cỡ gốc
 *
 * Vì đặt trong ViewPager2 (cần vuốt trái/phải đổi ảnh), View này phải TỰ báo
 * cho ViewPager2 biết khi nào được phép "cướp" sự kiện chạm:
 *  - Đang ở cỡ gốc (chưa zoom) -> để ViewPager2 xử lý vuốt đổi ảnh bình thường.
 *  - Đang phóng to -> chặn ViewPager2, để View này tự kéo (pan) ảnh, không thì
 *    vừa kéo ảnh vừa bị lật qua ảnh khác cùng lúc, rất khó dùng.
 */
public class ZoomableImageView extends AppCompatImageView {

    private static final float MUC_ZOOM_TOI_THIEU = 1f;
    private static final float MUC_ZOOM_TOI_DA = 4f;
    private static final float MUC_ZOOM_DOUBLE_TAP = 2.5f;

    private final Matrix matrix = new Matrix();
    private float mucZoomHienTai = 1f;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        khoiTao(context);
    }

    private void khoiTao(Context context) {
        // Bắt buộc dùng MATRIX thì mới tự lo được việc dịch chuyển/phóng to ảnh
        // bằng tay qua biến "matrix" - các ScaleType khác (FIT_CENTER,...) không
        // cho mình can thiệp vào toạ độ vẽ ảnh
        setScaleType(ScaleType.MATRIX);

        scaleGestureDetector = new ScaleGestureDetector(context, new LangNgheThuPhong());
        gestureDetector = new GestureDetector(context, new LangNgheKeoVaDoubleTap());

        // Đợi View có kích thước thật (sau layout) rồi mới canh giữa ảnh lần đầu -
        // gọi ngay trong khoiTao() sẽ bị getWidth()/getHeight() = 0 vì View chưa
        // được đo đạc xong
        addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                resetZoom());
    }

    // Đưa ảnh về đúng giữa View, tỉ lệ vừa khít (như ScaleType.FIT_CENTER), chưa
    // zoom - gọi mỗi khi PhotoViewerAdapter gán ảnh MỚI vào trang này (đảm bảo
    // ảnh nào cũng bắt đầu ở trạng thái chưa zoom, không bị "thừa kế" trạng thái
    // zoom của ảnh trước đó do RecyclerView tái sử dụng View)
    public void resetZoom() {
        if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        float chieuRongAnh = getDrawable().getIntrinsicWidth();
        float chieuCaoAnh = getDrawable().getIntrinsicHeight();
        float chieuRongView = getWidth();
        float chieuCaoView = getHeight();

        float tiLe = Math.min(chieuRongView / chieuRongAnh, chieuCaoView / chieuCaoAnh);
        float dxCanGiua = (chieuRongView - chieuRongAnh * tiLe) / 2f;
        float dyCanGiua = (chieuCaoView - chieuCaoAnh * tiLe) / 2f;

        matrix.reset();
        matrix.postScale(tiLe, tiLe);
        matrix.postTranslate(dxCanGiua, dyCanGiua);
        setImageMatrix(matrix);

        mucZoomHienTai = 1f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    // ===================== PINCH ZOOM (2 ngón) =====================

    private class LangNgheThuPhong extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float tiLeMoi = mucZoomHienTai * detector.getScaleFactor();
            tiLeMoi = Math.max(MUC_ZOOM_TOI_THIEU, Math.min(tiLeMoi, MUC_ZOOM_TOI_DA));

            float heSoDoiThucTe = tiLeMoi / mucZoomHienTai;
            mucZoomHienTai = tiLeMoi;

            matrix.postScale(heSoDoiThucTe, heSoDoiThucTe, detector.getFocusX(), detector.getFocusY());
            gioiHanTrongKhungHinh();
            setImageMatrix(matrix);
            capNhatQuyenChamChoCha();
            return true;
        }
    }

    // ===================== KÉO (PAN) + DOUBLE-TAP =====================

    private class LangNgheKeoVaDoubleTap extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            // Gọi ngay lúc vừa chạm xuống (trước khi biết mày sẽ kéo hay không) -
            // để nếu ảnh ĐANG zoom sẵn từ trước, ViewPager2 bị khoá lại NGAY LẬP
            // TỨC, không đợi tới lúc onScroll() mới khoá (lúc đó có thể ViewPager2
            // đã lỡ cướp mất sự kiện chạm rồi)
            capNhatQuyenChamChoCha();
            return true; // BẮT BUỘC true - không thì onScroll/onDoubleTap phía dưới sẽ không được gọi
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float khoangCachX, float khoangCachY) {
            // Chỉ tự kéo ảnh khi đang zoom to hơn cỡ gốc - lúc chưa zoom, trả về
            // false để nhường sự kiện kéo ngang cho ViewPager2 (vuốt đổi ảnh)
            if (mucZoomHienTai <= MUC_ZOOM_TOI_THIEU) {
                return false;
            }
            matrix.postTranslate(-khoangCachX, -khoangCachY);
            gioiHanTrongKhungHinh();
            setImageMatrix(matrix);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mucZoomHienTai > MUC_ZOOM_TOI_THIEU) {
                // Đang zoom -> double-tap để thu về lại cỡ gốc, canh giữa lại
                resetZoom();
            } else {
                // Đang cỡ gốc -> double-tap để zoom nhanh, tâm zoom là đúng vị trí
                // ngón tay vừa chạm (e.getX/getY), không phải giữa màn hình
                float heSoDoiThucTe = MUC_ZOOM_DOUBLE_TAP / mucZoomHienTai;
                mucZoomHienTai = MUC_ZOOM_DOUBLE_TAP;
                matrix.postScale(heSoDoiThucTe, heSoDoiThucTe, e.getX(), e.getY());
                gioiHanTrongKhungHinh();
                setImageMatrix(matrix);
            }
            capNhatQuyenChamChoCha();
            return true;
        }
    }

    // Giữ ảnh không bị kéo/zoom lệch hẳn ra ngoài View - nếu ảnh nhỏ hơn View
    // theo 1 chiều nào đó thì canh giữa chiều đó luôn, không cho kéo lệch
    private void gioiHanTrongKhungHinh() {
        if (getDrawable() == null) {
            return;
        }

        RectF rectAnh = new RectF(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
        matrix.mapRect(rectAnh);

        float chieuRongView = getWidth();
        float chieuCaoView = getHeight();
        float dx = 0f;
        float dy = 0f;

        if (rectAnh.width() <= chieuRongView) {
            dx = (chieuRongView - rectAnh.width()) / 2f - rectAnh.left;
        } else if (rectAnh.left > 0) {
            dx = -rectAnh.left;
        } else if (rectAnh.right < chieuRongView) {
            dx = chieuRongView - rectAnh.right;
        }

        if (rectAnh.height() <= chieuCaoView) {
            dy = (chieuCaoView - rectAnh.height()) / 2f - rectAnh.top;
        } else if (rectAnh.top > 0) {
            dy = -rectAnh.top;
        } else if (rectAnh.bottom < chieuCaoView) {
            dy = chieuCaoView - rectAnh.bottom;
        }

        matrix.postTranslate(dx, dy);
    }

    // Báo cho ViewPager2 (view cha) biết có nên "cướp" sự kiện chạm để vuốt
    // trang hay không - xem giải thích đầy đủ ở javadoc đầu class
    private void capNhatQuyenChamChoCha() {
        ViewParent viewCha = getParent();
        if (viewCha != null) {
            viewCha.requestDisallowInterceptTouchEvent(mucZoomHienTai > MUC_ZOOM_TOI_THIEU);
        }
    }
}
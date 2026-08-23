package ntu.nguyenhoangphuc.personal_diary.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// Fragment rỗng tạm thời cho tab "Kỷ niệm" - thiết kế kỹ sau
public class MemoriesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        TextView placeholder = new TextView(context);
        placeholder.setText("Màn Kỷ niệm - đang xây dựng");
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setTextColor(Color.parseColor("#3D2B24"));
        return placeholder;
    }
}

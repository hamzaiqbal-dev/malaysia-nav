package app.organicmaps.my.report;

import android.content.Context;
import android.location.Location;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.widget.TextViewCompat;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.sdk.util.log.Logger;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.IOException;

/**
 * "Lapor" sheet on the navigation screen. Reports are only saved on the device for now.
 */
public final class ReportSheet
{
  private static final String TAG = ReportSheet.class.getSimpleName();
  private static final String FILE_NAME = "my_reports.csv";

  private ReportSheet() {}

  @NonNull
  public static ReportStore store(@NonNull Context context)
  {
    return new ReportStore(new File(context.getFilesDir(), FILE_NAME));
  }

  @StringRes
  static int label(@NonNull ReportType type)
  {
    return switch (type)
    {
      case JAM -> R.string.report_jam;
      case ACCIDENT -> R.string.report_accident;
      case POLICE -> R.string.report_police;
      case AES_CAMERA -> R.string.report_aes_camera;
      case POTHOLE -> R.string.report_pothole;
      case FLOOD -> R.string.report_flood;
      case ROAD_CLOSED -> R.string.report_road_closed;
    };
  }

  public static void show(@NonNull Context context)
  {
    final BottomSheetDialog dialog = new BottomSheetDialog(context);
    final int pad = context.getResources().getDimensionPixelSize(R.dimen.margin_base);

    final LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(pad, pad, pad, pad);

    final TextView title = new TextView(context);
    title.setText(R.string.report_sheet_title);
    TextViewCompat.setTextAppearance(title, R.style.TextAppearance_Sampai_Label);
    title.setTextSize(18);
    title.setGravity(Gravity.CENTER_HORIZONTAL);
    title.setPadding(0, 0, 0, pad);
    root.addView(title);

    final GridLayout grid = new GridLayout(context);
    grid.setColumnCount(2);
    for (final ReportType type : ReportType.values())
    {
      final MaterialButton b = new MaterialButton(context);
      b.setText(label(type));
      b.setAllCaps(false);
      b.setOnClickListener(v -> {
        save(context, type);
        dialog.dismiss();
      });
      final GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f),
                                                                     GridLayout.spec(GridLayout.UNDEFINED, 1f));
      lp.width = 0;
      lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
      grid.addView(b, lp);
    }
    root.addView(grid);

    dialog.setContentView(root);
    dialog.show();
  }

  private static void save(@NonNull Context context, @NonNull ReportType type)
  {
    final Location loc = MwmApplication.from(context).getLocationHelper().getSavedLocation();
    if (loc == null || !Report.isFixFresh(SystemClock.elapsedRealtime() - loc.getElapsedRealtimeNanos() / 1_000_000))
    {
      Toast.makeText(context, R.string.current_location_unknown_error_title, Toast.LENGTH_SHORT).show();
      return;
    }
    try
    {
      store(context).add(new Report(System.currentTimeMillis(), type, loc.getLatitude(), loc.getLongitude()));
      Toast.makeText(context, R.string.report_saved, Toast.LENGTH_SHORT).show();
    }
    catch (IOException e)
    {
      Logger.e(TAG, "Cannot save report", e);
    }
  }
}

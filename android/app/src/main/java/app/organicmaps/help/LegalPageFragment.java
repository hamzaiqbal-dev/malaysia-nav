package app.organicmaps.help;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import app.organicmaps.R;
import app.organicmaps.WebContainerDelegate;
import app.organicmaps.base.BaseMwmFragment;
import app.organicmaps.util.WindowInsetUtils;

/**
 * Shows a bundled HTML page (privacy policy, terms) from the app assets, so it works offline.
 */
public class LegalPageFragment extends BaseMwmFragment
{
  public static final String ARG_URL = "url";

  private WebContainerDelegate mDelegate;

  @NonNull
  public static Bundle makeArgs(@NonNull String url)
  {
    final Bundle args = new Bundle();
    args.putString(ARG_URL, url);
    return args;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
  {
    final View root = inflater.inflate(R.layout.fragment_web_view_with_progress, container, false);

    ViewCompat.setOnApplyWindowInsetsListener(root, WindowInsetUtils.PaddingInsetsListener.excludeTop());

    final String url = requireArguments().getString(ARG_URL);
    mDelegate = new WebContainerDelegate(root, url) {
      @Override
      protected void doStartActivity(Intent intent)
      {
        startActivity(intent);
      }
    };

    return root;
  }

  @Override
  public boolean onBackPressed()
  {
    if (!mDelegate.onBackPressed())
      ((HelpActivity) requireActivity()).stackFragment(HelpFragment.class, getString(R.string.help), null);

    return true;
  }
}

package net.nend.sample.java.fullboard;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.nend.android.NendAdFullBoard;
import net.nend.android.NendAdFullBoardLoader;
import net.nend.android.NendAdFullBoardView;
import net.nend.sample.java.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class FullBoardPagerActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<NendAdFullBoard>> {

    private static final String TAG = "FullBoardAd";
    private static final int AD_COUNT = 2;

    private ViewPager mPager;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_board_pager);
        mPager = (ViewPager) findViewById(R.id.pager);
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Update the ad orientation.
                mPager.getAdapter().notifyDataSetChanged();
            }
        });
    }

    @Override
    public Loader<List<NendAdFullBoard>> onCreateLoader(int id, Bundle args) {
        return new AdLoader(getApplicationContext());
    }

    @Override
    public void onLoadFinished(Loader<List<NendAdFullBoard>> loader, List<NendAdFullBoard> data) {
        List<Page> pages = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            pages.add(new ContentPage());
        }
        if (AD_COUNT == data.size()) {
            pages.add(2, new AdPage(data.get(0)));
            pages.add(4, new AdPage(data.get(1)));
        } else {
            Log.d(TAG, "Couldn't obtain two ads.");
        }
        mPager.setAdapter(new Adapter(getSupportFragmentManager(), pages));
    }

    @Override
    public void onLoaderReset(Loader<List<NendAdFullBoard>> loader) {
    }

    interface Page {
        Fragment getFragment();
    }

    public static class ContentFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            TextView textView = new TextView(getActivity());
            textView.setText("Content");
            textView.setGravity(Gravity.CENTER);
            return textView;
        }
    }

    public static class AdFragment extends Fragment implements NendAdFullBoardView.FullBoardAdClickListener {

        private NendAdFullBoard mAd;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            if (mAd != null) {
                return NendAdFullBoardView.Builder.with(getActivity(), mAd)
                        .adClickListener(this)
                        .build();
            } else {
                return null;
            }
        }

        void setAd(NendAdFullBoard ad) {
            mAd = ad;
        }

        @Override
        public void onClickAd(NendAdFullBoard ad) {
            Log.d(TAG, "ClickAd");
        }
    }

    private static class AdLoader extends AsyncTaskLoader<List<NendAdFullBoard>> {

        private List<NendAdFullBoard> mAds;
        private NendAdFullBoardLoader mLoader;

        AdLoader(Context context) {
            super(context);
            mLoader = new NendAdFullBoardLoader(context, 485520, "a88c0bcaa2646c4ef8b2b656fd38d6785762f2ff");
        }

        @Override
        public List<NendAdFullBoard> loadInBackground() {
            final CountDownLatch latch = new CountDownLatch(AD_COUNT);
            final List<NendAdFullBoard> ads = new ArrayList<>();
            for (int i = 0; i < AD_COUNT; i++) {
                mLoader.loadAd(new NendAdFullBoardLoader.Callback() {
                    @Override
                    public void onSuccess(NendAdFullBoard ad) {
                        ads.add(ad);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(NendAdFullBoardLoader.FullBoardAdError error) {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await();
            } catch (InterruptedException ignore) {

            }
            return ads;
        }

        @Override
        public void deliverResult(List<NendAdFullBoard> data) {
            mAds = data;
            if (isStarted()) {
                super.deliverResult(data);
            }
        }

        @Override
        protected void onStartLoading() {
            if (mAds != null) {
                deliverResult(mAds);
            } else {
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();
            onStopLoading();
            mAds = null;
        }
    }

    private class ContentPage implements Page {

        @Override
        public Fragment getFragment() {
            return new ContentFragment();
        }
    }

    private class AdPage implements Page {

        private final NendAdFullBoard mAd;

        private AdPage(NendAdFullBoard ad) {
            mAd = ad;
        }

        @Override
        public Fragment getFragment() {
            AdFragment f = new AdFragment();
            f.setAd(mAd);
            return f;
        }
    }

    private class Adapter extends FragmentPagerAdapter {

        private final List<Page> mPages;

        Adapter(FragmentManager fm, List<Page> pages) {
            super(fm);
            mPages = pages;
        }

        @Override
        public int getItemPosition(Object object) {
            if (object instanceof AdFragment) {
                return POSITION_NONE;
            } else {
                return super.getItemPosition(object);
            }
        }

        @Override
        public Fragment getItem(int position) {
            return mPages.get(position).getFragment();
        }

        @Override
        public int getCount() {
            return mPages.size();
        }
    }
}

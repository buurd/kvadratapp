package se.danielkonsult.www.kvadratab.activities;

import android.graphics.Bitmap;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import se.danielkonsult.www.kvadratab.AppCtrl;
import se.danielkonsult.www.kvadratab.R;
import se.danielkonsult.www.kvadratab.adapters.ConsultantListAdapter;
import se.danielkonsult.www.kvadratab.entities.ConsultantData;
import se.danielkonsult.www.kvadratab.fragments.ConsultantFilterFragment;
import se.danielkonsult.www.kvadratab.services.data.ConsultantFilter;
import se.danielkonsult.www.kvadratab.services.data.DataServiceListener;
import se.danielkonsult.www.kvadratab.services.data.DataServiceListeners;

public class ConsultantListActivity extends AppCompatActivity implements ConsultantFilterFragment.Listener, DataServiceListener {

    // Private variables

    private static final String TAG_FILTER = "CONSULTANT_FILTER";
    private static final long FRAGMENT_FILTER_FADE_DURATION = 200;

    private final Handler handler = new Handler();

    private ListView _lvMain;
    private FloatingActionButton _fabFilter;
    private ConsultantFilterFragment _fragmentConsultantFilter;

    // Private methods

    /**
     * Hides or displays the consultant filter.
     */
    private void toggleFilterView() {
        int currentVisibility = _fragmentConsultantFilter.getView().getVisibility();
        int targetVisibility = currentVisibility == View.GONE ? View.VISIBLE : View.GONE;

        _fragmentConsultantFilter.getView().setVisibility(targetVisibility);

        _fabFilter.setVisibility(currentVisibility);
    }

    // Public methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultant_list);

        _lvMain = (ListView) findViewById(R.id.lvMain);
        _fabFilter = (FloatingActionButton) findViewById(R.id.fabFilter);
        _fabFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFilterView();
            }
        });

        _fragmentConsultantFilter = (ConsultantFilterFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentFilter);
        _fragmentConsultantFilter.setListener(this);

        // Perform an initial update of the consultants list
        onConsultantsUpdated();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register as data service listener
        AppCtrl.getDataService().registerListener(this);
    }

    @Override
    protected void onPause() {
        AppCtrl.getDataService().unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // If the filter is visible, hide it
        if (_fragmentConsultantFilter.getView().getVisibility() != View.VISIBLE)
            super.onBackPressed();
        else {
            // Update the filter
            ConsultantFilter newFilter = _fragmentConsultantFilter.getFilter();
            AppCtrl.getDataService().setFilter(newFilter);
            toggleFilterView();
        }
    }

    @Override
    protected void onDestroy() {
        AppCtrl.getDataService().unregisterListener(this);
        super.onDestroy();
    }

    // Methods (ConsultantFilterFragment.Listener)

    @Override
    public void onClose() { }
    public void onInitialLoadStarted() { }
    public void onInitialLoadProgress(int progressCount, int totalCount) { }
    public void onConsultantAdded(ConsultantData consultant, Bitmap bitmap) { }
    public void onError(String tag, String errorMessage) { }

    @Override
    public void onConsultantsUpdated() {
        _lvMain.setVisibility(View.INVISIBLE);
        try {
            ConsultantData[] consultantDatas = AppCtrl.getDataService().getFilteredConsultants();
            _lvMain.setAdapter(new ConsultantListAdapter(ConsultantListActivity.this, consultantDatas));
        } finally {
            _lvMain.setVisibility(View.VISIBLE);
        }
    }
}

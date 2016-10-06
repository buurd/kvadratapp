package se.danielkonsult.www.kvadratab.services.data;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import se.danielkonsult.www.kvadratab.AppCtrl;
import se.danielkonsult.www.kvadratab.entities.ConsultantData;
import se.danielkonsult.www.kvadratab.entities.OfficeData;
import se.danielkonsult.www.kvadratab.helpers.Utils;
import se.danielkonsult.www.kvadratab.helpers.db.KvadratDb;

/**
 * Created by Daniel on 2016-09-14.
 */
public class DefaultDataService implements DataService {

    // Refresh the database once a week
    private static final int REFRESH_INTERVAL_HOURS = 7 * 24;

    // Private variables

    private static ConsultantData[] _allConsultants;
    private static ConsultantData[] _filteredConsultants;
    private static ConsultantData[] _triedFilterConsultants;

    private ConsultantFilter _filter;
    private ConsultantFilter _triedFilter;

    private static OfficeData[] _offices;
    private DataServiceListener _listener;

    // Private methods

    private void setFilteredConsultants(ConsultantData[] consultantDatas) {
        _filteredConsultants = consultantDatas;
        if (_listener != null)
            _listener.onFilteredConsultantsUpdated();
    }

    /**
     * Tries a filter on all consultants and returns the ones that match.
     */
    private ConsultantData[] applyFilter(ConsultantFilter filter) {
        List<ConsultantData> result = new ArrayList<>();

        // Is the filter empty? Then copy all consultants and exit the function
        if (Utils.isStringNullOrEmpty(filter.getName().trim()) &&
                filter.getOfficeIds().size() == 0) {
            for (ConsultantData cd: getAllConsultants())
                result.add(cd);
            return result.toArray(new ConsultantData[result.size()]);
        }

        String namePiece1 = "";
        String namePiece2 = "";
        boolean shouldFilterByName = false;
        // Is there any name filter? Then split it into first and last name
        if (!Utils.isStringNullOrEmpty(filter.getName().trim())){
            shouldFilterByName = true;
            int spaceIndex = filter.getName().indexOf(" ");
            if (spaceIndex < 0)
                namePiece1 = filter.getName().toLowerCase().trim();
            else {
                namePiece1 = filter.getName().toLowerCase().substring(0,spaceIndex).trim();
                namePiece2 = filter.getName().toLowerCase().substring(spaceIndex).trim();
            }
        }

        for (ConsultantData cd : getAllConsultants()) {
            boolean isSelected = true;

            // Are there any specific offices in the filter?
            if (filter.getOfficeIds().size() > 0){
                if (!filter.getOfficeIds().contains(cd.OfficeId))
                    isSelected = false;
            }
            // Should we filter by name?
            if (isSelected && shouldFilterByName) {
                isSelected = false;

                // Is there only one name piece?
                if (Utils.isStringNullOrEmpty(namePiece2) &&
                        (cd.FirstName.toLowerCase().startsWith(namePiece1) || cd.LastName.toLowerCase().startsWith(namePiece1))){
                    isSelected = true;
                }
                // Filter by both first and last name
                else if (!Utils.isStringNullOrEmpty(namePiece2) &&
                        cd.FirstName.toLowerCase().startsWith(namePiece1) && cd.LastName.toLowerCase().startsWith(namePiece2)) {
                    isSelected = true;
                }
            }

            // Is the consultant still selected?
            if (isSelected)
                result.add(cd);
        }

        return result.toArray(new ConsultantData[result.size()]);
    }

    // Constructor
    public DefaultDataService() {
        _filter = new ConsultantFilter();
    }

    @Override
    public void setListener(DataServiceListener listener) {
        _listener = listener;
    }

//    @Override
//    public void start() {
//        // Start on a separate thread to not lock up GUI
//        Runnable startRunnable = new Runnable() {
//            @Override
//            public void run() {
//                // Do we have any consultants in the database yet? And how long is it since the last refresh?
//                KvadratDb db = AppCtrl.getDb();
//                int consultantCount = db.getConsultantCount();
//
//                boolean isDataOld = false;
//                if (consultantCount > 0){
//                    // There are consultants, but how long since they were refreshed?
//                    int hoursSinceLastRefresh = AppCtrl.getPrefsService().getHoursSinceLastRefresh();
//                    if (hoursSinceLastRefresh > REFRESH_INTERVAL_HOURS){
//                        // Drop the database and then get a new handle to it
//                        AppCtrl.dropDatabase();
//                        db = AppCtrl.getDb();
//
//                        // Delete all consultant images
//                        AppCtrl.getImageService().deleteAllConsultantImages();
//
//                        isDataOld = true;
//                    }
//                }
//
//                if ((consultantCount == 0) || isDataOld) {
//                    InitialLoader loader = new InitialLoader(db, _listeners);
//                    loader.run();
//                }
//                else {
//                    // Prep by loading the consultants before saying we're finished
//                    setOffices(db.getAllOffices());
//                    setAllConsultants(db.getAllConsultants(true));
//
//                    // Signal that the data already is available, but also
//                    // check if it's time for a refresh of the data.
//                    _listeners.onFilteredConsultantsUpdated();
//
////                    Refresher refresher = new Refresher(db, _listeners);
////                    refresher.run();
//                }
//            }
//        };
//        AsyncTask.execute(startRunnable);
//    }

    @Override
    public OfficeData[] getOffices() {
        if (_offices == null)
            _offices = AppCtrl.getDb().getOfficeDataRepository().getAll();
        return  _offices;
    }

    @Override
    public ConsultantData[] getAllConsultants() {
        if (_allConsultants == null)
            _allConsultants = AppCtrl.getDb().getConsultantDataRepository().getAll(true);
        return _allConsultants;
    }

    @Override
    public ConsultantData[] getFilteredConsultants() {
        if (_filteredConsultants == null)
            _filteredConsultants = applyFilter(_filter);

        return _filteredConsultants;
    }

    @Override
    public int tryFilter(ConsultantFilter filter) {
        _triedFilter = filter;
        _triedFilterConsultants = applyFilter(filter);
        return _triedFilterConsultants.length;
    }

    @Override
    public void useTriedFilter() {
        if (_triedFilterConsultants != null){
            _filter = _triedFilter;
            setFilteredConsultants(_triedFilterConsultants);

            _triedFilterConsultants = null;
        }
    }

    @Override
    public void setFilter(ConsultantFilter filter) {
        _filter = filter;
        applyFilter(_filter);
    }

    @Override
    public ConsultantFilter getFilter() {
        return _filter;
    }
}

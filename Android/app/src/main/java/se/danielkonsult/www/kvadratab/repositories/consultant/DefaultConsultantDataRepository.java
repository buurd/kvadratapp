package se.danielkonsult.www.kvadratab.repositories.consultant;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import se.danielkonsult.www.kvadratab.entities.ConsultantData;
import se.danielkonsult.www.kvadratab.entities.ConsultantDetails;
import se.danielkonsult.www.kvadratab.entities.OfficeData;
import se.danielkonsult.www.kvadratab.helpers.Utils;
import se.danielkonsult.www.kvadratab.helpers.db.DbSpec;
import se.danielkonsult.www.kvadratab.helpers.db.KvadratDb;
import se.danielkonsult.www.kvadratab.repositories.office.DefaultOfficeDataRepository;
import se.danielkonsult.www.kvadratab.repositories.office.OfficeDataRepository;

/**
 * Handles database reading and writing of consultant data.
 */
public class DefaultConsultantDataRepository implements ConsultantDataRepository {

    // Private variables

    private final String[] queryProjection = {
            DbSpec.ConsultantEntry.COLUMN_NAME_ID,
            DbSpec.ConsultantEntry.COLUMN_NAME_FIRSTNAME,
            DbSpec.ConsultantEntry.COLUMN_NAME_LASTNAME,
            DbSpec.ConsultantEntry.COLUMN_NAME_JOBROLE,
            DbSpec.ConsultantEntry.COLUMN_NAME_DESCRIPTION,
            DbSpec.ConsultantEntry.COLUMN_NAME_OFFICEID,
            DbSpec.ConsultantEntry.COLUMN_NAME_OVERVIEW,
            DbSpec.ConsultantEntry.COLUMN_NAME_DETAILSTIMESTAMP
    };
    private final String orderBy = DbSpec.ConsultantEntry.COLUMN_NAME_LASTNAME + "," + DbSpec.ConsultantEntry.COLUMN_NAME_FIRSTNAME;

    private KvadratDb _db;

    // Private methods

    /**
     * Reads a ConsultantData object from a db cursor.
     */
    private ConsultantData getFromCursor(Cursor c) {
        ConsultantData consultantData = new ConsultantData();
        consultantData.Id = c.getInt(c.getColumnIndex(DbSpec.ConsultantEntry.COLUMN_NAME_ID));
        consultantData.FirstName = c.getString(c.getColumnIndex(DbSpec.ConsultantEntry.COLUMN_NAME_FIRSTNAME));
        consultantData.LastName = c.getString(c.getColumnIndex(DbSpec.ConsultantEntry.COLUMN_NAME_LASTNAME));
        consultantData.JobRole = c.getString(c.getColumnIndex(DbSpec.ConsultantEntry.COLUMN_NAME_JOBROLE));
        consultantData.Description = c.getString(c.getColumnIndex(DbSpec.ConsultantEntry.COLUMN_NAME_DESCRIPTION));
        consultantData.OfficeId = c.getInt(c.getColumnIndex(DbSpec.ConsultantEntry.COLUMN_NAME_OFFICEID));
        consultantData.Overview = c.getString(c.getColumnIndex(DbSpec.ConsultantEntry.COLUMN_NAME_OVERVIEW));
        consultantData.DetailsTimstamp = c.getLong(c.getColumnIndex(DbSpec.ConsultantEntry.COLUMN_NAME_DETAILSTIMESTAMP));

        return consultantData;
    }

    /**
     * Loads offices and links them to the consultants
     */
    private void performOfficeJoin(List<ConsultantData> consultantDatas) {
        // Load offices and create a hashmap for quick lookup
        OfficeDataRepository officeDataRepository = new DefaultOfficeDataRepository(_db);
        OfficeData[] officeDatas = officeDataRepository.getAll();
        HashMap<Integer, OfficeData> officeDataHash = new HashMap<>();
        for (OfficeData officeData : officeDatas)
            officeDataHash.put(officeData.Id, officeData);

        for (ConsultantData cd : consultantDatas){
            if (cd.OfficeId > 0)
                cd.Office = officeDataHash.get(cd.OfficeId);
        }
    }

    // Constructor

    public DefaultConsultantDataRepository(KvadratDb _db) {
        this._db = _db;
    }

    @Override
    public ConsultantData getById(int id, boolean joinOffice) {
        String selection = DbSpec.ConsultantEntry.COLUMN_NAME_ID + " = ?";
        String[] selectionArgs = {
                Integer.toString(id)
        };

        // Put the single hit in a result list if we need to join the office
        List<ConsultantData> result = new ArrayList<>();
        SQLiteDatabase db = _db.getReadableDatabase();
        Cursor c = db.query(DbSpec.ConsultantEntry.TABLE_NAME, queryProjection, selection, selectionArgs, null, null, null, null);
        if (c.moveToFirst()){
            ConsultantData cd = getFromCursor(c);
            cd.CompetenceAreas = _db.getConsultantCompetenceRepository().getById(cd.Id);
            result.add(cd);
        }

        if (joinOffice){
            performOfficeJoin(result);
        }

        if (result.size() > 0)
            return result.get(0);

        return null;
    }

    @Override
    public ConsultantData[] getAll(boolean joinOffices) {
        List<ConsultantData> result = new ArrayList<>();

        SQLiteDatabase db = _db.getReadableDatabase();
        Cursor c = db.query(DbSpec.ConsultantEntry.TABLE_NAME, queryProjection, null, null, null, null, orderBy);
        while (c.moveToNext()){
            result.add(getFromCursor(c));
        }

        if (joinOffices){
            performOfficeJoin(result);
        }

        return result.toArray(new ConsultantData[result.size()]);
    }

    @Override
    public int getCount() {
        SQLiteDatabase db = _db.getReadableDatabase();
        Cursor c = db.rawQuery(DbSpec.ConsultantEntry.SQL_COUNT_ALL, null);
        if (c.moveToFirst()){
            return c.getInt(0);
        }
        return -1;
    }

    @Override
    public void insert(ConsultantData consultant) {
        SQLiteDatabase db = _db.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbSpec.ConsultantEntry.COLUMN_NAME_ID, consultant.Id);
        values.put(DbSpec.ConsultantEntry.COLUMN_NAME_FIRSTNAME, consultant.FirstName);
        values.put(DbSpec.ConsultantEntry.COLUMN_NAME_LASTNAME, consultant.LastName);
        if (!Utils.isStringNullOrEmpty(consultant.JobRole))
            values.put(DbSpec.ConsultantEntry.COLUMN_NAME_JOBROLE, consultant.JobRole);
        if (!Utils.isStringNullOrEmpty(consultant.Description))
            values.put(DbSpec.ConsultantEntry.COLUMN_NAME_DESCRIPTION, consultant.Description);

        // Insert office data
        if (consultant.OfficeId != 0)
            values.put(DbSpec.ConsultantEntry.COLUMN_NAME_OFFICEID, consultant.OfficeId);
        else if ((consultant.Office != null) && (consultant.Office.Id != 0))
            values.put(DbSpec.ConsultantEntry.COLUMN_NAME_OFFICEID, consultant.Office.Id);

        db.insertOrThrow(DbSpec.ConsultantEntry.TABLE_NAME, null, values);

        // Does the consultant have competence data as well+
        if ((consultant.CompetenceAreas != null) && (consultant.CompetenceAreas.length > 0)) {
            _db.getConsultantCompetenceRepository().update(consultant.Id, consultant.CompetenceAreas);
        }
    }

    @Override
    public void updateOffice(int consultantId, int officeId) {
        SQLiteDatabase db = _db.getWritableDatabase();

        ContentValues updatedValues = new ContentValues();
        updatedValues.put(DbSpec.ConsultantEntry.COLUMN_NAME_OFFICEID, officeId);

        String filter = String.format("%s = %d", DbSpec.ConsultantEntry.COLUMN_NAME_ID, consultantId);

        db.update(DbSpec.ConsultantEntry.TABLE_NAME, updatedValues, filter, null);
    }

    @Override
    public void updateName(int consultantId, String firstName, String lastName) {
        SQLiteDatabase db = _db.getWritableDatabase();

        ContentValues updatedValues = new ContentValues();
        updatedValues.put(DbSpec.ConsultantEntry.COLUMN_NAME_FIRSTNAME, firstName);
        updatedValues.put(DbSpec.ConsultantEntry.COLUMN_NAME_LASTNAME, lastName);

        String filter = String.format("%s = %d", DbSpec.ConsultantEntry.COLUMN_NAME_ID, consultantId);

        db.update(DbSpec.ConsultantEntry.TABLE_NAME, updatedValues, filter, null);
    }

    @Override
    public void updateDetails(int consultantId, ConsultantDetails details) {
        SQLiteDatabase db = _db.getWritableDatabase();

        _db.getConsultantCompetenceRepository().update(consultantId, details.CompetenceAreas);

        long currentTimestamp = System.currentTimeMillis();

        ContentValues updatedValues = new ContentValues();
        updatedValues.put(DbSpec.ConsultantEntry.COLUMN_NAME_DESCRIPTION, details.Description);
        updatedValues.put(DbSpec.ConsultantEntry.COLUMN_NAME_OVERVIEW, details.Overview);
        updatedValues.put(DbSpec.ConsultantEntry.COLUMN_NAME_DETAILSTIMESTAMP, currentTimestamp);

        String filter = String.format("%s = %d", DbSpec.ConsultantEntry.COLUMN_NAME_ID, consultantId);
        db.update(DbSpec.ConsultantEntry.TABLE_NAME, updatedValues, filter, null);
    }

    @Override
    public void delete(int id) {
        SQLiteDatabase db = _db.getWritableDatabase();

        _db.getConsultantCompetenceRepository().delete(id);

        String whereClause = DbSpec.ConsultantEntry.COLUMN_NAME_ID + "=?";
        String[] whereArgs = new String[] { Integer.toString(id) };

        db.delete(DbSpec.ConsultantEntry.TABLE_NAME, whereClause, whereArgs);
    }
}

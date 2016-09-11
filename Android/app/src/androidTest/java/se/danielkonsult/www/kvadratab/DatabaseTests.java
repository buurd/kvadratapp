package se.danielkonsult.www.kvadratab;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import se.danielkonsult.www.kvadratab.entities.OfficeData;
import se.danielkonsult.www.kvadratab.helpers.db.DbOperationListener;
import se.danielkonsult.www.kvadratab.helpers.db.KvadratDb;
import se.danielkonsult.www.kvadratab.helpers.db.OfficeDataArrayListener;

/**
 * Tests aimed at the KvadratDb class.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseTests {

    private static final String TAG = "DatabaseTests";

    private boolean operationSuccessful;
    private OfficeData[] assertOffices;

    /**
     * Test that an office can be written and read in the Kvadrat database.
     */
    @Test
    public void shouldStoreOffice() throws InterruptedException {

        OfficeData oData = new OfficeData();
        oData.Id = 17;
        oData.Name = "Jönköping";

        final CountDownLatch signal = new CountDownLatch(1);

        // Insert the office
        operationSuccessful = false;
        KvadratDb db = new KvadratDb(InstrumentationRegistry.getTargetContext());
        db.insertOffice(oData, new DbOperationListener() {
            @Override
            public void onResult(long id) {
                operationSuccessful = true;
                signal.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, errorMessage);
                signal.countDown();
            }
        });

        signal.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(operationSuccessful);

        final CountDownLatch signal2 = new CountDownLatch(1);

        // Read it back
        assertOffices = null;
        db = new KvadratDb(InstrumentationRegistry.getTargetContext());
        db.getAllOffices(new OfficeDataArrayListener() {
            @Override
            public void onResult(OfficeData[] offices) {
                assertOffices = offices;
                signal2.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                signal2.countDown();
            }
        });

        signal2.await(10, TimeUnit.SECONDS);
        Assert.assertNotNull(assertOffices);
        boolean foundOffice = false;

        for (OfficeData office : assertOffices) {
            if ((office.Id == oData.Id) && (office.Name == oData.Name))
                foundOffice = true;
        }
        Assert.assertTrue(foundOffice);
    }
}
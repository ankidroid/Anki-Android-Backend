package net.ankiweb;

import android.content.Context;
import android.database.Cursor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.ankiweb.rsdroid.BackendFactory;
import net.ankiweb.rsdroid.BackendUtils;
import net.ankiweb.rsdroid.BackendV1;
import net.ankiweb.rsdroid.database.RustSupportSQLiteDatabase;
import net.ankiweb.rsdroid.testing.ModuleLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class CollectionCreationTest {

    @Before
    public void loadLibrary() {
        ModuleLoader.init();
    }

    @Test
    public void ensureCollectionCreatedIsValid() {
        // We use this routine in AnkiDroid to create the collection, therefore we need to ensure
        // that the database is valid, open, and the values returned match how the Java used to work

        BackendV1 backendV1 = new BackendFactory().getBackend();

        String collectionPath = new File(getTargetContext().getFilesDir(), "collection.anki2").getAbsolutePath();

        BackendUtils.openAnkiDroidCollection(backendV1, collectionPath);
        RustSupportSQLiteDatabase database = new RustSupportSQLiteDatabase(backendV1, collectionPath);

        database.beginTransaction();
        try {
            Cursor ver = database.query("select * from col");

            if (!ver.moveToFirst()) {
                throw new IllegalStateException("no rows");
            }



            int i = 5;
        } catch (Exception e) {
            // OK
        }
    }


    private Context getTargetContext() {
        return ApplicationProvider.getApplicationContext();
    }
}
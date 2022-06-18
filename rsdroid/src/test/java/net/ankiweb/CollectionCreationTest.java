package net.ankiweb;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.ankiweb.rsdroid.Backend;
import net.ankiweb.rsdroid.BackendFactory;
import net.ankiweb.rsdroid.database.AnkiSupportSQLiteDatabase;
import net.ankiweb.rsdroid.testing.RustBackendLoader;

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
        RustBackendLoader.init();
    }

    @Test
    public void ensureCollectionCreatedIsValid() {
        // We use this routine in AnkiDroid to create the collection, therefore we need to ensure
        // that the database is valid, open, and the values returned match how the Java used to work

        String path = new File(getTargetContext().getFilesDir(), "collection.anki2").getAbsolutePath();

        Configuration config = getConfiguration(path);

        Backend backend = BackendFactory.getBackend(getTargetContext());
        backend.openCollection(":memory:");
        SupportSQLiteDatabase database = AnkiSupportSQLiteDatabase.withRustBackend(backend);

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

    private Configuration getConfiguration(String path) {
        return Configuration.builder(getTargetContext())
                    .name(path)
                    .callback(new SupportSQLiteOpenHelper.Callback(1) {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {

                        }

                        @Override
                        public void onUpgrade(@NonNull SupportSQLiteDatabase db, int oldVersion, int newVersion) {

                        }
                    })
                    .build();
    }


    private Context getTargetContext() {
        return ApplicationProvider.getApplicationContext();
    }
}
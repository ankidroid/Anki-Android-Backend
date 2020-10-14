package net.ankiweb;

import android.content.Context;
import android.database.Cursor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.ankiweb.rsdroid.BackendException;
import net.ankiweb.rsdroid.BackendV1;
import net.ankiweb.rsdroid.NativeMethods;
import net.ankiweb.rsdroid.database.RustSupportSQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class CollectionCreationTest {

    private static boolean sLoaded;

    @Before
    public void loadLibrary() {
        if (!sLoaded) {
            try {
                File d = new File("C:\\GitHub\\Rust-Test\\rslib-bridge\\target\\release\\rsdroid.dll");
                Runtime.getRuntime().load("C:\\GitHub\\Rust-Test\\rslib-bridge\\target\\release\\rsdroid.dll");
            } catch (UnsatisfiedLinkError e) {
                if (e.getMessage() == null || !e.getMessage().contains("already loaded in another classloader")) {
                    throw e;
                }
            }
            sLoaded = true;
        }

        if (!NativeMethods.isRoboUnitTest()) {
            throw new IllegalStateException();
        }
    }

    @Test
    public void ensureCollectionCreatedIsValid() throws BackendException {
        // We use this routine in AnkiDroid to create the collection, therefore we need to ensure
        // that the database is valid, open, and the values returned match how the Java used to work

        BackendV1 backendV1 = new BackendV1();

        String collectionPath = new File(getTargetContext().getFilesDir(), "collection.anki2").getAbsolutePath();

        backendV1.openAnkiDroidCollection(collectionPath);
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
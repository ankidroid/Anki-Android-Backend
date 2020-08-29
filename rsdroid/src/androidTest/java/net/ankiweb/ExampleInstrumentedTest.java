package net.ankiweb;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.InvalidProtocolBufferException;

import net.ankiweb.rsdroid.BackendV1;

import org.junit.Test;
import org.junit.runner.RunWith;

import BackendProto.Backend;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws InvalidProtocolBufferException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        Backend.SchedTimingTodayOut ret = new BackendV1().schedTimingToday();

        int elpased = ret.getDaysElapsed();
        long nextDayAt = ret.getNextDayAt();

        assertEquals("net.ankiweb.rsdroid.test", appContext.getPackageName());
    }
}
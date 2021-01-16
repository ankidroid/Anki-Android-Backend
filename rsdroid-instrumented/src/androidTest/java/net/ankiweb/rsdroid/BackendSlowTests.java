/*
 * Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.ankiweb.rsdroid;

import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;

import net.ankiweb.rsdroid.ankiutil.InstrumentedTest;
import net.ankiweb.rsdroid.database.RustSupportSQLiteOpenHelper;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BackendSlowTests extends InstrumentedTest {

    @Test
    // @Ignore("Run manually - 38 seconds (and fails)")
    public void ensureSQLIsStreamed() throws IOException {
        // Issue #6 - commentary there.
        // We need to ensure that the SQL is streamed so we don't OOM the Java.
        // Presently the Rust loads all the data at once - from the exceptions below,
        // we also need to work around this.



        // Testing was done on an API 21 emulator - 1536 RAM, 256MB VM Heap

        int numberOfElements = 5000;
        int numberOfAppends = 10;

        // This raised two stack traces. One was upsetting (

        /*
         *
         I/DEBUG: *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***
         I/DEBUG: Build fingerprint: 'generic_x86/sdk_google_phone_x86/generic_x86:5.0.2/LSY66K/6695550:eng/test-keys'
         I/DEBUG: Revision: '0'
         I/DEBUG: ABI: 'x86'
         I/DEBUG: pid: 27721, tid: 27737, name: roidJUnitRunner  >>> net.ankiweb.rsdroid.instrumented <<<
         I/DEBUG: signal 6 (SIGABRT), code -6 (SI_TKILL), fault addr --------
         I/DEBUG:     eax 00000000  ebx 00006c49  ecx 00006c59  edx 00000006
         I/DEBUG:     esi af3ffdb8  edi 0000000c
         I/DEBUG:     xcs 00000073  xds 0000007b  xes 0000007b  xfs 0000005f  xss 0000007b
         I/DEBUG:     eip b76f1ea6  ebp 00006c59  esp af3feba0  flags 00200282
         I/DEBUG: backtrace:
         I/DEBUG:     #00 pc 00073ea6  /system/lib/libc.so (tgkill+22)
         I/DEBUG:     #01 pc 00021b1b  /system/lib/libc.so (pthread_kill+155)
         I/DEBUG:     #02 pc 00023394  /system/lib/libc.so (raise+36)
         I/DEBUG:     #03 pc 0001b874  /system/lib/libc.so (abort+84)
         I/DEBUG:     #04 pc 00ad1d34  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so
         I/DEBUG:     #05 pc 00abd7e4  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so (std::process::abort::h065e619536580e75+20)
         I/DEBUG:     #06 pc 00ac34b5  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so (rust_oom+53)
         I/DEBUG:     #07 pc 00aec02f  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so (alloc::alloc::handle_alloc_error::hd089a51289db2e22+31)
         I/DEBUG:     #08 pc 002d22cd  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so
         I/DEBUG:     #09 pc 002a718e  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so
         I/DEBUG:     #10 pc 0028320c  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so
         I/DEBUG:     #11 pc 002b8c9c  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so
         I/DEBUG:     #12 pc 00278e48  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so
         I/DEBUG:     #13 pc 00321c41  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so (anki::backend::Backend::db_command::hb9cda1c640b22898+209)
         I/DEBUG:     #14 pc 00321d0d  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so (anki::backend::Backend::run_db_command_bytes::hbd3014586c1b3ec5+45)
         I/DEBUG:     #15 pc 001b0f29  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so
         I/DEBUG:     #16 pc 001b038e  /data/app/net.ankiweb.rsdroid.instrumented.test-2/lib/x86/librsdroid.so (Java_net_ankiweb_rsdroid_NativeMethods_fullDatabaseCommand+62)
         I/DEBUG:     #17 pc 0037bd1e  /data/dalvik-cache/x86/data@app@net.ankiweb.rsdroid.instrumented.test-2@base.apk@classes.dex
         I/DEBUG: Tombstone written to: /data/tombstones/tombstone_02
         I/Zygote: Process 27721 exited due to signal (6)
         W/ActivityManager: Error in app net.ankiweb.rsdroid.instrumented running instrumentation ComponentInfo{net.ankiweb.rsdroid.instrumented.test/androidx.test.runner.AndroidJUnitRunner}:
         W/ActivityManager:   Native crash
         W/ActivityManager:   Native crash: Aborted
         D/AndroidRuntime: Shutting down VM
         I/art: Debugger is no longer active
         I/ActivityManager: Force stopping net.ankiweb.rsdroid.instrumented appid=10066 user=0: finished inst
         I/ActivityManager: Killing 27721:net.ankiweb.rsdroid.instrumented/u0a66 (adj 0): stop net.ankiweb.rsdroid.instrumented
         W/libprocessgroup: failed to open /acct/uid_10066/pid_27721/cgroup.procs: No such file or directory
         W/ActivityManager: Spurious death for ProcessRecord{f2bc55 27721:net.ankiweb.rsdroid.instrumented/u0a66}, curProc for 27721: null
         2072-2087/com.google.android.gms.persistent I/art: Background sticky concurrent mark sweep GC freed 19248(909KB) AllocSpace objects, 2(32KB) LOS objects, 4% free, 20MB/21MB, paused 25.469ms total 401.130ms
         D/AndroidRuntime: >>>>>> AndroidRuntime START com.android.internal.os.RuntimeInit <<<<<<
         */

        /*
        int numberOfElements = 5000;
        int numberOfAppends = 10;

        java.lang.OutOfMemoryError: Failed to allocate a 553047794 byte allocation with 4194304 free bytes and 118MB until OOM
        at java.lang.String.<init>(String.java:233)
        at java.lang.String.<init>(String.java:149)
        at java.lang.String.<init>(String.java:119)
        at net.ankiweb.rsdroid.BackendV1Impl.fullQueryInternal(BackendV1Impl.java:162)
        at net.ankiweb.rsdroid.BackendV1Impl.fullQuery(BackendV1Impl.java:142)
        at net.ankiweb.rsdroid.BackendMutex.fullQuery(BackendMutex.java:83)
        at net.ankiweb.rsdroid.database.Session.fullQuery(Session.java:127)
        at net.ankiweb.rsdroid.database.MemoryHeavySQLiteCursor.<init>(MemoryHeavySQLiteCursor.java:34)
        at net.ankiweb.rsdroid.database.RustSupportSQLiteDatabase.query(RustSupportSQLiteDatabase.java:127)
        at net.ankiweb.rsdroid.database.RustSupportSQLiteDatabase.query(RustSupportSQLiteDatabase.java:122)
        at net.ankiweb.rsdroid.BackendSlowTests.memoryIsBounded(BackendSlowTests.java:103)
        at java.lang.reflect.Method.invoke(Native Method)
        at java.lang.reflect.Method.invoke(Method.java:372)
        at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
        at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
        at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
        at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
        at androidx.test.internal.runner.junit4.statement.RunBefores.evaluate(RunBefores.java:80)
        at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
        at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
        at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
        at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
        at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
        at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
        at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
        at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
        at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
        at org.junit.runners.Suite.runChild(Suite.java:128)
        at org.junit.runners.Suite.runChild(Suite.java:27)
        at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
        at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
        at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
        at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
        at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
        at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
        at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
        at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
        at androidx.test.internal.runner.TestExecutor.execute(TestExecutor.java:56)
        at androidx.test.runner.AndroidJUnitRunner.onStart(AndroidJUnitRunner.java:395)
        at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1837)
         */

        try (BackendV1 backend = super.getBackend("initial_version_2_12_1.anki2")) {
            SupportSQLiteDatabase db = new RustSupportSQLiteOpenHelper(backend).getWritableDatabase();

            db.query("create table tmp (id varchar)");

            StringBuilder longString = new StringBuilder("VeryLongStringWhich Will MaybeCauseAnOOM IfWeDoItWrong");
            for (int i = 0; i < numberOfAppends; i++) {
                longString.append(longString);
            }

            for (int i = 0; i < numberOfElements; i++) {
                // add a suffix so the string can't be interned
                db.query("insert into tmp (id) values (?)", new Object[] { longString.toString() + i });
            }

            int count = 0;

            try (Cursor c = db.query("select * from tmp")) {
                while (c.moveToNext()) {
                    c.getString(0);
                    count += 1;
                }
            }

            assertThat(count, is(numberOfElements));
        }
    }
}

// Copyright: Ankitects Pty Ltd and contributors
// License: GNU AGPL, version 3 or later; http://www.gnu.org/licenses/agpl.html

package net.ankiweb.rsdroid;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.ankiweb.rsdroid.ankiutil.InstrumentedTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class BackendTranslationsTest extends InstrumentedTest {
    
    private String withoutIsolation(String s) {
        return s.replace("\u2068", "").replace("\u2069", "");
    }

    @Test
    public void ensureI18nWorks() {
        Backend b = new Backend(Arrays.asList("en"));
        assertThat(withoutIsolation(b.getTr().mediaCheckTrashCount(5, 10)), equalTo("Trash folder: 5 files, 10MB"));
        assertThat(withoutIsolation(b.getTr().mediaCheckTrashCount(5, 10.0)), equalTo("Trash folder: 5 files, 10MB"));
        assertThat(withoutIsolation(b.getTr().mediaCheckTrashCount(5, "foo")), equalTo("Trash folder: 5 files, fooMB"));
        b = new Backend(Arrays.asList("fr"));
        assertThat(withoutIsolation(b.getTr().mediaCheckTrashCount(5, 10)), equalTo("Corbeille : 5 fichiers, 10 Mo"));
    }
}

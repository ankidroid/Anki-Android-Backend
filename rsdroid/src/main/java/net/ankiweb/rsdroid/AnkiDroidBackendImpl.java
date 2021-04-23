/*
 * Copyright (c) 2021 David Allison <davidallisongithub@gmail.com>
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

public class AnkiDroidBackendImpl extends net.ankiweb.rsdroid.AdbackendImpl {

    private final PointerGen mPointerGen;

    public AnkiDroidBackendImpl(PointerGen pointerGen) {
        this.mPointerGen = pointerGen;
    }

    @Override
    public Pointer ensureBackend() {
        return mPointerGen.generatePointer();
    }

    @Override
    protected byte[] executeCommand(long backendPointer, int command, byte[] args) {
        return NativeMethods.executeAnkiDroidCommand(backendPointer, command, args);
    }

    public void downgradeBackend(String collectionPath) {
        String ret = NativeMethods.downgradeDatabase(collectionPath);

        if (ret != null && ret.length() != 0) {
            throw new BackendException(ret);
        }

        // otherwise, return
    }

    public interface PointerGen {
        Pointer generatePointer();
    }
}

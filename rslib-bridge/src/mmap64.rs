/*
Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>

This program is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation; either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program.  If not, see <http://www.gnu.org/licenses/>.

This file incorporates work covered by the following copyright and
permission notice:

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at http://mozilla.org/MPL/2.0/.

https://searchfox.org/mozilla-central/rev/38bcf897f1fa19c1eba441a611cf309482e0d6e5/nsprpub/pr/src/md/unix/unix.c#2716-2737
 */

#[cfg(not(feature = "no-android"))]
use libc::{size_t, c_int, loff_t, c_void, syscall, SYS_mmap2, MAP_FAILED};


/* NDK non-unified headers for API < 21 don't have mmap64. However,
 * NDK unified headers do provide mmap64 for all API versions when building
 * with clang.
 *
 * Mozilla uses the following logic:
 *
 * We should provide mmap64 here for API < 21 if we're not using clang or if we're using
 * non-unified headers. We check for non-unified headers by the lack of __ANDROID_API_L__ macro.
 *
 * But we'll just use mmap2 which works until one hits 2^44 bytes of RAM (17TB).
 * This is to avoid performing an app bundle split based on the Android SDK_INT.
 * I expect we'll be past API 21 before this becomes a problem.
 *
 * "Nobody will ever need more than 17TB of memory"
 */

#[no_mangle]
#[cfg(not(feature = "no-android"))]
pub unsafe extern "C" fn mmap64(addr : *mut c_void, len: size_t, prot: c_int, flags: c_int, fd: c_int, offset: loff_t) -> *mut c_void {
    let android_page_size = 4096 as loff_t;
    if offset & (android_page_size - 1) != 0 {
        // TODO: libc::set_errno(libc::EINVAL);
        return MAP_FAILED;
    }

    return syscall(SYS_mmap2, addr, len, prot, flags, fd, offset / android_page_size) as *mut c_void
}
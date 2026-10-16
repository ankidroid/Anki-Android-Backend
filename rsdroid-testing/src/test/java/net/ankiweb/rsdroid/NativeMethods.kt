// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid

/** The production JNI signature, without depending on the Android/protobuf backend module. */
object NativeMethods {
    external fun openBackend(data: ByteArray): Array<ByteArray?>?
}

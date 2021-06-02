/*
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2019 BeDataDriven Groep B.V. and contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.gnu.org/licenses/gpl-2.0.txt
 *
 * https://github.com/mobanisto/gcc-bridge/blob/3bd5df076d7d3b6d7b0a66653c2d114e4bfaf381/runtime/src/main/java/org/renjin/gcc/runtime/Stdlib.java
 */

package net.ankiweb.rsdroid.utils;

public class StringToLong {

    public static long strtol(String s) {
        int radix = 0;

        // Find the start of the number
        int start = 0;

        // Skip beginning whitespace
        while (start < s.length() && Character.isWhitespace(s.charAt(start))) {
            start++;
        }

        int pos = start;

        // Check for +/- prefix
        if (pos < s.length() && (s.charAt(pos) == '-' || s.charAt(pos) == '+')) {
            pos++;
        }

        // Check for hex prefix 0x/0X if the radix is 16 or unspecified
        else if(pos + 1 < s.length() && s.charAt(pos) == '0' && (s.charAt(pos + 1) == 'x' || s.charAt(pos + 1) == 'X')) {
            start += 2;
            pos = start;
            radix = 16;

        }

        // If radix is not specified, then check for octal prefix
        else if(pos < s.length() && s.charAt(pos) == '0') {
            radix = 8;
        }

        // Otherwise if radix is not specified, and there is no prefix,
        // assume decimal
        if (radix == 0) {
            radix = 10;
        }

        // Advance until we run out of digits
        while(pos < s.length() && Character.digit(s.charAt(pos), radix) != -1) {
            pos++;
        }

        // If empty, return 0 and exit
        if (start == pos) {
            return 0;
        }

        s = s.substring(start, pos);

        try {
            return Long.parseLong(s, radix);
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }
}

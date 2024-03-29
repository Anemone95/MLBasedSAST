/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package top.anemone.mlsast.gui2;

/**
 * For debugging purposes only... Make sure DEBUG is set to false before you
 * release a new version.
 *
 * @author Dan
 *
 */
public class Debug {
    public static void println(Object s) {
        if (MainFrame.GUI2_DEBUG) {
            System.out.println(s);
        }
    }

    public static void printf(String format, Object... args) {
        if (MainFrame.GUI2_DEBUG) {
            System.out.printf(format, args);
        }
    }

    public static void println(Exception e) {
        if (MainFrame.GUI2_DEBUG) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

    }
}

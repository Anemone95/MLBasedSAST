/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package top.anemone.mlsast.gui2;

import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.Matcher;
import top.anemone.mlsast.gui2.FilterActivity.FilterActivityNotifier;

/**
 * Updates filters in the current running FindBugs.
 *
 * @author Graham Allan
 */
public class ApplyNewFilter {

    private final Filter suppressionFilter;
    private final PreferencesFrame preferencesFrame;
    private final FilterActivityNotifier filterActivityNotifier;

    public ApplyNewFilter(Filter suppressionFilter, PreferencesFrame preferencesFrame, FilterActivityNotifier filterActivityNotifier) {
        this.suppressionFilter = suppressionFilter;
        this.preferencesFrame = preferencesFrame;
        this.filterActivityNotifier = filterActivityNotifier;
    }

    public void fromMatcher(Matcher matcher) {
        if (matcher != null) {
            suppressionFilter.addChild(matcher);

            preferencesFrame.updateFilterPanel();
            filterActivityNotifier.notifyListeners(FilterListener.Action.FILTERING, null);
        }
    }

}

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package top.anemone.mlsast.gui2;

import top.anemone.mlsast.core.exception.PredictorRunnerException;
import top.anemone.mlsast.core.exception.SlicerException;
import top.anemone.mlsast.core.predict.exception.PredictorException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.util.List;

/**
 * @author pugh
 */
public class GuiUtil {

    public static void addOkAndCancelButtons(JPanel panel, JButton ok, JButton cancel) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        if (MainFrame.MAC_OS_X) {
            panel.add(Box.createHorizontalGlue());
            panel.add(cancel);
            panel.add(Box.createHorizontalStrut(5));
            panel.add(ok);
            panel.add(Box.createHorizontalStrut(20));
        } else {
            panel.add(Box.createHorizontalGlue());
            panel.add(ok);
            panel.add(Box.createHorizontalStrut(5));
            panel.add(cancel);
            panel.add(Box.createHorizontalStrut(5));
        }
    }


    public static void addExceptions(Exception exception, List<Exception> exceptions){
        if (exception instanceof PredictorRunnerException) {
            for (Exception e : ((PredictorRunnerException) exception).getExceptions()) {
                addExceptions(e, exceptions);
            }
        } else if (exception instanceof PredictorException) {
            if (!exception.getMessage().equals(LabelPredictor.EXCEPTION_MESSAGE)){
                exceptions.add(((PredictorException) exception).getRawException());
            }
        } else if (exception instanceof SlicerException) {
            exceptions.add(((SlicerException) exception).getRawException());
        } else {
            exceptions.add(exception);
        }

    }
}

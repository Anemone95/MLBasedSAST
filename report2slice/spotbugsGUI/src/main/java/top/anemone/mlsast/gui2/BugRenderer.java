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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Priorities;
import top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParser;

@SuppressWarnings("serial")
/**
 *  Sets colors for JTree nodes
 *  @author Dan
 */
public class BugRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object node, boolean selected, boolean expanded, boolean leaf,
                                                  int row, boolean hasFocus) {
        Component toReturn = super.getTreeCellRendererComponent(tree, node, selected, expanded, leaf, row, hasFocus);

        if (!(node instanceof BugLeafNode)) {
            return toReturn;
        } else {
            BugInstance bug = ((BugLeafNode) node).getBug();
            final Color c;
            Color c1;
            switch (bug.getPriority()) {
                case Priorities.LOW_PRIORITY:
                    c1 = new Color(0.4f, 0.4f, 0.6f);
                    break;
                case Priorities.NORMAL_PRIORITY:
                    if (bug.isDead()) {
                        c1 = new Color(0.2f, 0.2f, 0.2f);
                    } else {
                        c1 = new Color(255, 204, 0);
                    }
                    break;
                case Priorities.HIGH_PRIORITY:
                    if (bug.isDead()) {
                        c1 = new Color(.65f, 0.2f, 0.2f);
                    } else {
                        c1 = new Color(.85f, 0, 0);
                    }
                    break;
                case Priorities.EXP_PRIORITY:
                case Priorities.IGNORE_PRIORITY:
                default:
                    c1 = Color.BLUE;
                    break;
            }
            if (AiProject.getInstance().bugInstanceIsLabeled.contains(bug) && AiProject.getInstance().getLabeledIsSafe(bug) != null) {
                if (AiProject.getInstance().getLabeledIsSafe(bug)) {
                    c1 = Color.GRAY;
                }
            } else {
                if (AiProject.getInstance().getBugInstanceIsSafe(bug) != null &&
                        (AiProject.getInstance().getBugInstanceIsSafe(bug))) {
                    c1 = Color.GRAY;
                }
            }
            c = c1;
            if (leaf) {
                Icon icon = new Icon() {
                    @Override
                    public void paintIcon(Component comp, Graphics g, int x, int y) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(c);
                        g2.fillOval(2, 2, 12, 12);
                        g2.setColor(Color.BLACK);
                        g2.drawOval(2, 2, 12, 12);
                    }

                    @Override
                    public int getIconWidth() {
                        return 16;
                    }

                    @Override
                    public int getIconHeight() {
                        return 16;
                    }
                };
                ((BugRenderer) toReturn).setIcon(icon);
                if (SpotbugXMLReportParser.caredVulns.contains(bug.getType())) {
                    // show prediction
                    String prediction = null;
                    String rawText = ((BugRenderer) toReturn).getText();
                    if (AiProject.getInstance().getPredictProject() != null && AiProject.getInstance().getPredictProject().getExceptions(bug) != null) {
                        prediction = "(P:ERR)";
                    } else {
                        Boolean bugIsSafe = AiProject.getInstance().getBugInstanceIsSafe(bug);
                        if (bugIsSafe == null) {
                            prediction = "(P:UNK)";
                        } else if (!bugIsSafe) {
                            prediction = "(P:TP)";
                        } else {
                            prediction = "(P:FP)";
                        }
                    }
                    String label;
                    if (AiProject.getInstance().getLabelProject() != null && (!AiProject.getInstance().bugInstanceIsLabeled.contains(bug))) {
                        label = "[L:ULB]";
                    } else {
                        Boolean isSafe = AiProject.getInstance().getLabeledIsSafe(bug);
                        if (isSafe == null) {
                            label = "[L:ULB]";
                        } else if (isSafe) {
                            label = "[L:FP]";
                        } else {
                            label = "[L:TP]";
                        }
                    }

//                    ((BugRenderer) toReturn).setTextNonSelectionColor(new Color(88, 88, 88));
                    ((BugRenderer) toReturn).setText(prediction + label + rawText);
                }
            }
            return toReturn;
        }
    }
}

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import edu.umd.cs.findbugs.L10N;

/**
 * @author pugh
 */
public class SplitLayout implements FindBugsLayoutManager {

    final MainFrame frame;

    JLabel sourceTitle;

    JPanel topLeftSPane;

    JSplitPane subPane1;

    JSplitPane subPane2;

    JSplitPane outerPane;

    JButton viewSource = new JButton("View in browser");

    /**
     * @param frame
     */
    public SplitLayout(MainFrame frame) {
        this.frame = frame;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#createWindowMenu()
     */
    @Override
    public JMenu createWindowMenu() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#initialize()
     */
    @Override
    public void initialize() {

        Font buttonFont = viewSource.getFont();
        viewSource.setFont(buttonFont.deriveFont(buttonFont.getSize() / 2));
        viewSource.setPreferredSize(new Dimension(150, 15));
        viewSource.setEnabled(false);

        topLeftSPane = frame.mainFrameTree.bugListPanel();

        JPanel sourceTitlePanel = new JPanel();
        sourceTitlePanel.setLayout(new BorderLayout());

        JPanel outerSourcePanel = new JPanel();
        outerSourcePanel.setLayout(new BorderLayout());
        outerSourcePanel.setBorder(new EmptyBorder(3, 3, 1, 3));
        JPanel sourcePanel = new JPanel();
        BorderLayout sourcePanelLayout = new BorderLayout();
        sourcePanelLayout.setHgap(3);
        sourcePanelLayout.setVgap(3);
        sourcePanel.setLayout(sourcePanelLayout);
        sourceTitle = new JLabel();
        sourceTitle.setText(L10N.getLocalString("txt.source_listing", ""));

        sourceTitlePanel.setBorder(new EmptyBorder(3, 3, 3, 3));
        sourceTitlePanel.add(viewSource, BorderLayout.EAST);
        sourceTitlePanel.add(sourceTitle, BorderLayout.CENTER);

        sourcePanel.setBorder(new SourceBorder(Color.GRAY,10));
        sourcePanel.add(sourceTitlePanel, BorderLayout.NORTH);
        sourcePanel.add(frame.createSourceCodePanel(), BorderLayout.CENTER);
        sourcePanel.add(frame.createSourceSearchPanel(), BorderLayout.SOUTH);
        outerSourcePanel.add(sourcePanel);

        JScrollPane[] summaryTabs = frame.summaryTab();

        JScrollPane vulnDetailPane= summaryTabs[0];
        JScrollPane vulnTypeDescPane= summaryTabs[1];

        subPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topLeftSPane, vulnDetailPane);
        subPane1.setOneTouchExpandable(true);
        subPane1.setContinuousLayout(true);
        subPane1.setDividerLocation(GUISaveState.getInstance().getSplitRight());
        removeSplitPaneBorders(subPane1,0);

        subPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outerSourcePanel, vulnTypeDescPane);
        subPane2.setOneTouchExpandable(true);
        subPane2.setContinuousLayout(true);
        subPane2.setDividerLocation(GUISaveState.getInstance().getSplitRight());
        removeSplitPaneBorders(subPane2,0);

        outerPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, subPane1, subPane2);
        outerPane.setOneTouchExpandable(true);
        outerPane.setContinuousLayout(true);
        outerPane.setDividerLocation(GUISaveState.getInstance().getSplitMain());
        removeSplitPaneBorders(outerPane,0);

        frame.setLayout(new BorderLayout());
        frame.add(outerPane, BorderLayout.CENTER);
        frame.add(frame.statusBar(), BorderLayout.SOUTH);

    }

    private void removeSplitPaneBorders(JSplitPane pane, int rm) {
        pane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void setBorder(Border b) {
                    }
                };
            }
        });
        pane.setBorder(new EmptyBorder(rm, rm, rm, rm));
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#makeSourceVisible()
     */
    @Override
    public void makeSourceVisible() {

    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#saveState()
     */
    @Override
    public void saveState() {
        GUISaveState.getInstance().setSplitLeft(subPane1.getDividerLocation());
        GUISaveState.getInstance().setSplitRight(subPane2.getDividerLocation());
        GUISaveState.getInstance().setSplitMain(outerPane.getDividerLocation());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#setSourceTitle(java.lang
     * .String)
     */
    @Override
    public void setSourceTitle(String title) {
        sourceTitle.setText(title);

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.gui2.FindBugsLayoutManager#getSourceTitleComponent()
     */
    @Override
    public JComponent getSourceViewComponent() {
        return viewSource;
    }

}

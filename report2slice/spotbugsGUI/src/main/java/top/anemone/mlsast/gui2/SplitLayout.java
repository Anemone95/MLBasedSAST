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
import javax.swing.border.LineBorder;
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

    JSplitPane rightPane;

    JSplitPane subRightPane;

    JSplitPane mainSPane;

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

        sourcePanel.setBorder(new LineBorder(Color.GRAY));
        sourcePanel.add(sourceTitlePanel, BorderLayout.NORTH);
        sourcePanel.add(frame.createSourceCodePanel(), BorderLayout.CENTER);
        sourcePanel.add(frame.createSourceSearchPanel(), BorderLayout.SOUTH);

        JScrollPane[] summaryTabs = frame.summaryTab();

        JScrollPane vulnDetailPane= summaryTabs[0];
        JScrollPane vulnTypeDescPane= summaryTabs[1];

        subRightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sourcePanel, vulnTypeDescPane);
        subRightPane.setOneTouchExpandable(true);
        subRightPane.setContinuousLayout(true);
        subRightPane.setDividerLocation(GUISaveState.getInstance().getSplitRight());
        removeSplitPaneBorders(subRightPane,0);

        rightPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, vulnDetailPane, subRightPane);
        rightPane.setOneTouchExpandable(true);
        rightPane.setContinuousLayout(true);
        rightPane.setDividerLocation(GUISaveState.getInstance().getSplitRight());
        removeSplitPaneBorders(rightPane,0);


        mainSPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, topLeftSPane, rightPane);
        mainSPane.setOneTouchExpandable(true);
        mainSPane.setContinuousLayout(true);
        mainSPane.setDividerLocation(GUISaveState.getInstance().getSplitMain());
        removeSplitPaneBorders(mainSPane,0);

        frame.setLayout(new BorderLayout());
        frame.add(mainSPane, BorderLayout.CENTER);
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
        GUISaveState.getInstance().setSplitRight(rightPane.getDividerLocation());
        GUISaveState.getInstance().setSplitSummary(subRightPane.getDividerLocation());
        GUISaveState.getInstance().setSplitMain(mainSPane.getDividerLocation());
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

package top.anemone.mlsast.gui2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.MethodDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.h3xstream.findsecbugs.injection.taintdata.MethodNodeAnnotation;
import com.h3xstream.findsecbugs.injection.taintdata.MethodVertex;
import edu.umd.cs.findbugs.*;
import top.anemone.mlsast.core.data.Func;
import top.anemone.mlsast.core.data.taintTree.TaintFlow;

public class MainFrameComponentFactory implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(MainFrameComponentFactory.class.getName());

    private final MainFrame mainFrame;

    private URL sourceLink;

    private boolean listenerAdded = false;


    public MainFrameComponentFactory(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    JPanel statusBar() {
        JPanel statusBar = new JPanel();
        // statusBar.setBackground(Color.WHITE);

        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridy = 0;
        constraints.weightx = 1;
        statusBar.add(mainFrame.getStatusBarLabel(), constraints.clone());

        return statusBar;
    }

    JScrollPane[] summaryTab() {
        JPanel pan=new JPanel();
        pan.setOpaque(false);
        mainFrame.setSummaryTopPanel(pan);
        mainFrame.getSummaryTopPanel().setLayout(new GridLayout(0, 1));
        mainFrame.getSummaryTopPanel().setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        //        mainFrame.getSummaryTopPanel().setMinimumSize(new Dimension(fontSize * 50, fontSize * 5));

        JPanel summaryTopOuter = new JPanel(new BorderLayout());
//        summaryTopOuter.setOpaque(true);
//        summaryTopOuter.setBackground(Color.WHITE);
        summaryTopOuter.add(mainFrame.getSummaryTopPanel(), BorderLayout.NORTH);

        mainFrame.getSummaryHtmlArea().setContentType("text/html");
        mainFrame.getSummaryHtmlArea().setEditable(false);
        mainFrame.getSummaryHtmlArea().addHyperlinkListener(evt -> AboutDialog.editorPaneHyperlinkUpdate(evt));
        setStyleSheets();
        // JPanel temp = new JPanel(new BorderLayout());
        // temp.add(summaryTopPanel, BorderLayout.CENTER);
        JScrollPane summaryScrollPane = new JScrollPane(summaryTopOuter);
        summaryScrollPane.getVerticalScrollBar().setUnitIncrement((int) Driver.getFontSize());

        JScrollPane[] components=new JScrollPane[2];
        components[0]=summaryScrollPane;
        components[1]=mainFrame.getSummaryHtmlScrollPane();
//        JSplitPane splitP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, summaryScrollPane,
//                mainFrame.getSummaryHtmlScrollPane());
//        splitP.setContinuousLayout(true);
//        splitP.setDividerLocation(GUISaveState.getInstance().getSplitSummary());
//        splitP.setOneTouchExpandable(true);
//        splitP.setUI(new BasicSplitPaneUI() {
//            @Override
//            public BasicSplitPaneDivider createDefaultDivider() {
//                return new BasicSplitPaneDivider(this) {
//                    @Override
//                    public void setBorder(Border b) {
//                    }
//                };
//            }
//        });
//        splitP.setBorder(null);
        return components;
    }

    private void setStyleSheets() {
        StyleSheet styleSheet = new StyleSheet();
        styleSheet.addRule("body {font-size: " + Driver.getFontSize() + "pt}");
        styleSheet.addRule("H1 {color: red;  font-size: 120%; font-weight: bold;}");
        styleSheet.addRule("code {font-family: courier; font-size: " + Driver.getFontSize() + "pt}");
        styleSheet.addRule(" a:link { color: #0000FF; } ");
        styleSheet.addRule(" a:visited { color: #800080; } ");
        styleSheet.addRule(" a:active { color: #FF0000; text-decoration: underline; } ");
        HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
        htmlEditorKit.setStyleSheet(styleSheet);
        mainFrame.summaryHtmlArea.setEditorKit(htmlEditorKit);
    }

    /**
     * Creates the source code panel, but does not put anything in it.
     */
    JPanel createSourceCodePanel() {
        Font sourceFont = new Font("Monospaced", Font.PLAIN, (int) Driver.getFontSize());
        mainFrame.getSourceCodeTextPane().setFont(sourceFont);
        mainFrame.getSourceCodeTextPane().setEditable(false);
        mainFrame.getSourceCodeTextPane().getCaret().setSelectionVisible(true);
        mainFrame.getSourceCodeTextPane().setDocument(SourceCodeDisplay.SOURCE_NOT_RELEVANT);
        JScrollPane sourceCodeScrollPane = new JScrollPane(mainFrame.getSourceCodeTextPane());
        sourceCodeScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sourceCodeScrollPane, BorderLayout.CENTER);

        panel.revalidate();
        if (MainFrame.GUI2_DEBUG) {
            System.out.println("Created source code panel");
        }
        return panel;
    }

    JPanel createSourceSearchPanel() {
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        JPanel thePanel = new JPanel();
        thePanel.setLayout(gridbag);
        mainFrame.getFindButton().setToolTipText("Find first occurrence");
        mainFrame.getFindNextButton().setToolTipText("Find next occurrence");
        mainFrame.getFindPreviousButton().setToolTipText("Find previous occurrence");
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.insets = new Insets(0, 5, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(mainFrame.getSourceSearchTextField(), c);
        thePanel.add(mainFrame.getSourceSearchTextField());
        // add the buttons
        mainFrame.getFindButton().addActionListener(evt -> mainFrame.searchSource(0));
        c.gridx = 1;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        gridbag.setConstraints(mainFrame.getFindButton(), c);
        thePanel.add(mainFrame.getFindButton());
        mainFrame.getFindNextButton().addActionListener(evt -> mainFrame.searchSource(1));
        c.gridx = 2;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        gridbag.setConstraints(mainFrame.getFindNextButton(), c);
        thePanel.add(mainFrame.getFindNextButton());
        mainFrame.getFindPreviousButton().addActionListener(evt -> mainFrame.searchSource(2));
        c.gridx = 3;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        gridbag.setConstraints(mainFrame.getFindPreviousButton(), c);
        thePanel.add(mainFrame.getFindPreviousButton());
        return thePanel;
    }

    /**
     * Sets the title of the source tabs for either docking or non-docking
     * versions.
     */
    void setSourceTab(String title, @CheckForNull BugInstance bug) {
        JComponent label = mainFrame.getGuiLayout().getSourceViewComponent();
        if (label != null) {
            removeLink(label);
        }
        mainFrame.getGuiLayout().setSourceTitle(title);
    }

    private void removeLink(JComponent component) {
        this.sourceLink = null;
        component.setEnabled(false);
        component.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        component.setToolTipText("");
    }

    void initializeGUI() {
        SwingUtilities.invokeLater(new InitializeGUI(mainFrame));
    }

    Component bugSummaryComponent(BugAnnotation value, BugInstance bug, int predictIsClean, int labelIsClean) {
        JLabel label = new JLabel();
        label.setFont(label.getFont().deriveFont(Driver.getFontSize()));
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        label.setForeground(Color.BLACK);
        ClassAnnotation primaryClass = bug.getPrimaryClass();

        String sourceCodeLabel = L10N.getLocalString("summary.source_code", "source code.");
        String summaryLine = L10N.getLocalString("summary.line", "Line");
        String summaryLines = L10N.getLocalString("summary.lines", "Lines");
        String clickToGoToText = L10N.getLocalString("tooltip.click_to_go_to", "Click to go to");
        if (value instanceof SourceLineAnnotation) {
            final SourceLineAnnotation link = (SourceLineAnnotation) value;
            if (sourceCodeExists(link)) {
                String srcStr = "";
                int start = link.getStartLine();
                int end = link.getEndLine();
                if (start < 0 && end < 0) {
                    srcStr = sourceCodeLabel;
                } else if (start == end) {
                    srcStr = " [" + summaryLine + " " + start + "]";
                } else if (start < end) {
                    srcStr = " [" + summaryLines + " " + start + " - " + end + "]";
                }

                label.setToolTipText(clickToGoToText + " " + srcStr);

                label.addMouseListener(new BugSummaryMouseListener(bug, label, link));
            }

            label.setText(link.toString());
        } else if (value instanceof BugAnnotationWithSourceLines) {
            BugAnnotationWithSourceLines note = (BugAnnotationWithSourceLines) value;
            final SourceLineAnnotation link = note.getSourceLines();
            String srcStr = "";
            if (link != null && sourceCodeExists(link)) {
                int start = link.getStartLine();
                int end = link.getEndLine();
                if (start < 0 && end < 0) {
                    srcStr = sourceCodeLabel;
                } else if (start == end) {
                    srcStr = " [" + summaryLine + " " + start + "]";
                } else if (start < end) {
                    srcStr = " [" + summaryLines + " " + start + " - " + end + "]";
                }

                if (!"".equals(srcStr)) {
                    label.setToolTipText(clickToGoToText + " " + srcStr);
                    label.addMouseListener(new BugSummaryMouseListener(bug, label, link));
                }
            }
            String noteText;
            if (note == bug.getPrimaryMethod() || note == bug.getPrimaryField()) {
                noteText = note.toString();
            } else {
                noteText = note.toString(primaryClass);
            }
            if (labelIsClean>=0){
                noteText=addCleanTag(noteText, true);
            }
            if (predictIsClean>=0){
                noteText=addCleanTag(noteText, false);
            }
            if (!srcStr.equals(sourceCodeLabel)) {
                label.setText(noteText + srcStr);
            } else {
                label.setText(noteText);
            }
        } else {
            label.setText(value.toString(primaryClass));
        }

        AiProject project = AiProject.getInstance();
        if (project.getPredictProject() != null && project.getPredictProject().getSliceProject() != null) {
            if (value instanceof StringAnnotation && value.getDescription().equals("Taintflow tree")){
                label.addMouseListener(new LabelVulnTreeListener(bug, label, Integer.parseInt(((StringAnnotation) value).getValue())));
            } else if (value instanceof MethodNodeAnnotation && ((MethodNodeAnnotation) value).firstId!=null){
                MethodNodeAnnotation methodNodeAnnotation=(MethodNodeAnnotation) value;
                Func func=new Func(methodNodeAnnotation.getClassName(), methodNodeAnnotation.getMethodName(), methodNodeAnnotation.getMethodSignature());
                label.addMouseListener(new LabelSafeFlowListener(bug, label, func));
            }

        }
        return label;
    }

    public String addCleanTag(String noteText, boolean isLabel) {
        String pattern;
        if (isLabel){
            pattern="[✨]";
        } else {
            pattern="(✨)";
        }
        if (noteText.contains("←")) {
            noteText = noteText.replace("←", "←"+pattern);
        } else if (noteText.contains("→")) {
            noteText = noteText.replace("→", "→"+pattern);
        } else {
            noteText = noteText.replace(" ", pattern);
        }
        return noteText;
    }



    /**
     * Creates bug summary component. If obj is a string will create a JLabel
     * with that string as it's text and return it. If obj is an annotation will
     * return a JLabel with the annotation's toString(). If that annotation is a
     * SourceLineAnnotation or has a SourceLineAnnotation connected to it and
     * the source file is available will attach a listener to the label.
     */
    public Component bugSummaryComponent(String str, BugInstance bug) {
        JLabel label = new JLabel();
        label.setFont(label.getFont().deriveFont(Driver.getFontSize()));
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        label.setForeground(Color.BLACK);

        label.setText(str);

        SourceLineAnnotation link = bug.getPrimarySourceLineAnnotation();
        if (link != null) {
            label.addMouseListener(new BugSummaryMouseListener(bug, label, link));
        }

        return label;
    }

    private boolean sourceCodeExists(@Nonnull SourceLineAnnotation note) {
        try {
            mainFrame.getProject().getSourceFinder().findSourceFile(note);
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static class InitializeGUI implements Runnable {
        private final MainFrame mainFrame;

        public InitializeGUI(final MainFrame mainFrame) {
            this.mainFrame = mainFrame;
        }

        @Override
        public void run() {
            mainFrame.setTitle("MSCA");
            // noinspection ConstantConditions
            if (MainFrame.USE_WINDOWS_LAF && System.getProperty("os.name").toLowerCase().contains("windows")) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Could not load Windows Look&Feel", e);
                }
            }

            try {
                mainFrame.getGuiLayout().initialize();
            } catch (Exception e) {
                // If an exception was encountered while initializing, this may
                // be because of a bug in the particular look-and-feel selected
                // (as in sourceforge bug 1899648). In an attempt to recover
                // gracefully, this code reverts to the cross-platform look-
                // and-feel and attempts again to initialize the layout.
                if (!"Metal".equals(UIManager.getLookAndFeel().getName())) {
                    System.err.println("Exception caught initializing GUI; reverting to CrossPlatformLookAndFeel");
                    try {
                        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    } catch (Exception e2) {
                        System.err.println("Exception while setting CrossPlatformLookAndFeel: " + e2);
                        throw new Error(e2);
                    }
                    mainFrame.getGuiLayout().initialize();
                } else {
                    throw new Error(e);
                }
            }
            mainFrame.mainFrameTree.setBugPopupMenu(mainFrame.mainFrameTree.createBugPopupMenu());
            mainFrame.mainFrameTree.setBranchPopupMenu(mainFrame.mainFrameTree.createBranchPopUpMenu());
            mainFrame.updateStatusBar();
            Rectangle bounds = GUISaveState.getInstance().getFrameBounds();
            if (bounds != null) {
                mainFrame.setBounds(bounds);
            }

            mainFrame.setExtendedState(GUISaveState.getInstance().getExtendedWindowState());
            Toolkit.getDefaultToolkit().setDynamicLayout(true);
            mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            mainFrame.setJMenuBar(mainFrame.mainFrameMenu.createMainMenuBar());
            mainFrame.setVisible(true);

            mainFrame.getMainFrameLoadSaveHelper().initialize();

            // Sets the size of the tooltip to match the rest of the GUI. -
            // Kristin
            JToolTip tempToolTip = mainFrame.mainFrameTree.getTableheader().createToolTip();
            UIManager.put("ToolTip.font", new FontUIResource(tempToolTip.getFont().deriveFont(Driver.getFontSize())));

            setupOSX();

            String loadFromURL = SystemProperties.getOSDependentProperty("findbugs.loadBugsFromURL");

            if (loadFromURL != null) {
                try {
                    loadFromURL = SystemProperties.rewriteURLAccordingToProperties(loadFromURL);
                    URL url = new URL(loadFromURL);
                    mainFrame.getMainFrameLoadSaveHelper().loadAnalysis(url);
                } catch (MalformedURLException e1) {
                    JOptionPane.showMessageDialog(mainFrame, "Error loading " + loadFromURL);
                }
            }

            mainFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    mainFrame.callOnClose();
                }
            });

            Driver.removeSplashScreen();
            mainFrame.waitForMainFrameInitialized();
        }

        private void setupOSX() {
            if (MainFrame.MAC_OS_X) {
                try {
                    mainFrame.mainFrameMenu.initOSX();
                    mainFrame.mainFrameMenu.enablePreferencesMenuItem(true);
                } catch (NoClassDefFoundError e) {
                    // This will be thrown first if the OSXAdapter is loaded on
                    // a system without the EAWT
                    // because OSXAdapter extends ApplicationAdapter in its def
                    System.err
                            .println("This version of Mac OS X does not support the Apple EAWT. Application Menu handling has been disabled ("
                                    + e + ")");
                } catch (ClassNotFoundException e) {
                    // This shouldn't be reached; if there's a problem with the
                    // OSXAdapter we should get the
                    // above NoClassDefFoundError first.
                    System.err
                            .println("This version of Mac OS X does not support the Apple EAWT. Application Menu handling has been disabled ("
                                    + e + ")");
                } catch (Exception e) {
                    System.err.println("Exception while loading the OSXAdapter: " + e);
                    e.printStackTrace();
                    if (MainFrame.GUI2_DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Listens for when cursor is over the label and when it is clicked. When
     * the cursor is over the label will make the label text blue and the cursor
     * the hand cursor. When clicked will take the user to the source code tab
     * and to the lines of code connected to the SourceLineAnnotation.
     *
     * @author Kristin Stephens
     */
    private class BugSummaryMouseListener extends MouseAdapter {
        private final BugInstance bugInstance;

        private final JLabel label;

        private final SourceLineAnnotation note;

        BugSummaryMouseListener(@Nonnull BugInstance bugInstance, @Nonnull JLabel label, @Nonnull SourceLineAnnotation link) {
            this.bugInstance = bugInstance;
            this.label = label;
            this.note = link;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            mainFrame.getSourceCodeDisplayer().displaySource(bugInstance, note);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            label.setForeground(Color.blue);
            mainFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            label.setForeground(Color.black);
            mainFrame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private class LabelVulnTreeListener extends MouseAdapter {
        private final BugInstance bugInstance;

        private final JLabel label;
        private final int id;


        LabelVulnTreeListener(@Nonnull BugInstance bugInstance, @Nonnull JLabel label, int id) {
            this.bugInstance = bugInstance;
            this.label = label;
            this.id = id;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton()== MouseEvent.BUTTON3){
                createLabelMenu(true, bugInstance, id, null).show(label,e.getX(), e.getY());
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            label.setForeground(Color.blue);
            mainFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            label.setForeground(Color.black);
            mainFrame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private class LabelSafeFlowListener extends MouseAdapter {
        private final BugInstance bugInstance;

        private final JLabel label;
        private final Func method;


        LabelSafeFlowListener(@Nonnull BugInstance bugInstance, @Nonnull JLabel label, Func methodDescriptor) {
            this.bugInstance = bugInstance;
            this.label = label;
            this.method=methodDescriptor;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton()== MouseEvent.BUTTON3){
                createLabelMenu(false, bugInstance,-1, method).show(label,e.getX(), e.getY());
                System.out.println(e.getButton());
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            label.setForeground(Color.blue);
            mainFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            label.setForeground(Color.black);
            mainFrame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    JPopupMenu createLabelMenu(boolean isVulnTree, BugInstance bug, int treeId, Func methodDescriptor) {
        JPopupMenu popupMenu = new JPopupMenu();
        // @Anemone, if caredVulns, pop label function
        AiProject project = AiProject.getInstance();
        if (isVulnTree){
            JMenuItem labeledAsTP = MainFrameHelper.newJMenuItem("menu.labelExploitFlow", "Label Exploitable Taint Tree");
            labeledAsTP.addActionListener(evt -> {
                AiProject.getInstance().bugInstanceIsLabeled.add(bug);
                AiTPLabelDialog dialog = new AiTPLabelDialog(MainFrame.getInstance(), null, true, bug, AiProject.getInstance().getSliceProject().getTaintTrees(bug),treeId);
                dialog.setLocationRelativeTo(MainFrame.getInstance());
                dialog.setVisible(true);
                MainFrame.getInstance().syncBugInformation();
            });
            popupMenu.add(labeledAsTP);

        } else {
            JMenuItem labeledAsFP = MainFrameHelper.newJMenuItem("menu.labelSafeFlow", "Label Safe Taint Flow");
            labeledAsFP.addActionListener(evt -> {
                AiProject.getInstance().bugInstanceIsLabeled.add(bug);
                List<TaintFlow> flows= AiProject.getInstance().getSliceProject().getTaintFlows(bug).stream().filter(e->e.getEntry().equals(methodDescriptor)).collect(Collectors.toList());
                AiFPLabelDialog dialog = new AiFPLabelDialog(MainFrame.getInstance(), null, true, new HashSet<>(flows));
                dialog.setLocationRelativeTo(MainFrame.getInstance());
                dialog.setVisible(true);
                MainFrame.getInstance().syncBugInformation();
            });
            popupMenu.add(labeledAsFP);

        }

        return popupMenu;
    }
}

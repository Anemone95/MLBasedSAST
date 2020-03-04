package top.anemone.mlsast.gui2;


import com.h3xstream.findsecbugs.injection.taintdata.LocationNodeAnnotation;
import com.h3xstream.findsecbugs.injection.taintdata.MethodNodeAnnotation;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.log.Logger;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.data.VO.Label;
import top.anemone.mlsast.core.data.taintTree.TaintFlow;
import top.anemone.mlsast.core.data.taintTree.TaintTreeNode;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.exception.PredictorRunnerException;
import top.anemone.mlsast.core.exception.SliceRunnerException;
import top.anemone.mlsast.core.predict.PredictRunner;
import top.anemone.mlsast.core.predict.exception.PredictorException;
import top.anemone.mlsast.core.utils.ExceptionUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;

public class AiTPLabelDialog extends JDialog {
    private void closeDialog(WindowEvent event) {
        closeDialog();
    }

    private void closeDialog() {
        setVisible(false);
        dispose();
    }

    public static String getTreeStringById(BugInstance bug, int id){
        String sId= String.valueOf(id);
        boolean isThisTree=false;
        StringBuilder stringBuilder=new StringBuilder();
        for (BugAnnotation annotation: bug.getAnnotations()) {
            if (annotation instanceof StringAnnotation && annotation.getDescription().equals("Taintflow tree")) {
                if (((StringAnnotation) annotation).getValue().equals(sId)){
                    isThisTree=true;
                    continue;
                }
            }
            if (isThisTree){
                if (annotation instanceof MethodNodeAnnotation || annotation instanceof LocationNodeAnnotation){
                    stringBuilder.append(annotation.toString());
                    stringBuilder.append("\n");
                } else {
                    break;
                }
            }
        }
        return stringBuilder.toString();
    }

    public AiTPLabelDialog(JFrame parent, Logger l, boolean modal, BugInstance bug, java.util.List<TaintTreeNode> trees, int id) {
        super(parent, modal);
        this.setSize(700, 400);
        setTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.label_dialog", "Labeling"));
        setLayout(new BorderLayout());

        JPanel gridPanel = new JPanel(new BorderLayout());
        gridPanel.setBorder(new EmptyBorder(3, 6, 3, 6));
        gridPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel labelText = new JLabel("<html>Choose one taint tree where the bug can be exploited.</html>");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.6;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 3, 5, 3);
        gridPanel.add(labelText, gbc);

        String[] choices=new String[trees.size()];
        for (int i = 0; i < trees.size(); i++) {
            choices[i]="Taint tree "+(i+1);
        }
        JComboBox<String> jcombo = new JComboBox<>(choices);
        jcombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 3, 5, 3);
        gridPanel.add(jcombo, gbc);

        JTextArea sliceText = new JTextArea();
        sliceText.setEditable(false);
        JScrollPane sliceScroll=new JScrollPane(sliceText);
        jcombo.setSelectedItem("Taint tree "+id);
        String tree=getTreeStringById(bug, jcombo.getSelectedIndex()+1);
        sliceText.setText(tree);
        sliceText.setCaretPosition(0);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        gridPanel.add(sliceScroll, gbc);

        JPanel bottomPanel = new JPanel();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gridPanel.add(bottomPanel, gbc);


        jcombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedTree = (String) jcombo.getSelectedItem();
                int selectId=Integer.parseInt(selectedTree.split(" ")[2]);
                String tree1 =getTreeStringById(bug, selectId);
                sliceText.setText(tree1);
                sliceText.setCaretPosition(0);
            }

        });

        bottomPanel.add(new JButton(new AbstractAction("Submit") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                String selectedTree = (String) jcombo.getSelectedItem();
                int selectId=Integer.parseInt(selectedTree.split(" ")[2]);

                for (TaintFlow edge : AiProject.getInstance().getSliceProject().getTaintFlows(trees.get(selectId-1))) {
                    Label label = new Label(MainFrame.getInstance().getProject().toString(), AiProject.getInstance().getSliceProject().getSliceHash(edge), false);
                    label.setTaintFlow(edge);
                    try {
                        AiProject.getInstance().labelPredictor.label(label);
                    } catch (PredictorException e) {
                        e.printStackTrace();
                    }
                    try {
                        AiProject.getInstance().getRemotePredictor().label(label);
                    } catch (PredictorException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(
                                MainFrame.getInstance(),
                                ExceptionUtil.getStackTrace(e).substring(0, 3000),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                try {
                    AiProject.getInstance().setLabelProject(
                            new PredictRunner<>(AiProject.getInstance().getSliceProject())
                                    .setPredictor(AiProject.getInstance().labelPredictor)
                                    .run(new Monitor() {
                                        @Override
                                        public void init(String stageName, int totalWork) {
                                            ;
                                        }

                                        @Override
                                        public void process(int idx, int totalWork, Object input, Object output, Exception exception) {
                                        }
                                    })
                    );
                } catch (ParserException | PredictorRunnerException | NotFoundException | SliceRunnerException e) {
                    e.printStackTrace();
                }
                closeDialog();
            }
        }));

        getContentPane().add(gridPanel);
    }
}

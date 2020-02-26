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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

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
        setTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.label_dialog", "Labeling"));
        JPanel contentPanel = new JPanel(new BorderLayout());
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel,BoxLayout.Y_AXIS));
        JLabel label = new JLabel(
                "<html>Choose one taint tree where the bug can be exploited.</html>"
                );
        label.setSize(700, 20);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelPanel.add(label);

        String[] choices=new String[trees.size()];
        for (int i = 0; i < trees.size(); i++) {
            choices[i]="Taint tree "+(i+1);
        }

        JComboBox<String> jcombo = new JComboBox<>(choices);
        jcombo.setSize(700, 20);
        jcombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelPanel.add(jcombo);


        JLabel nonce = new JLabel("<html><br/></html>");
        nonce.setSize(700, 5);
        nonce.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelPanel.add(nonce);

        JTextArea sliceText = new JTextArea();
        sliceText.setEditable(false);
        sliceText.setSize(700,400);
        JScrollPane sliceScroll=new JScrollPane(sliceText);
        labelPanel.add(sliceScroll);
        jcombo.setSelectedItem("Taint tree "+id);
        String tree=getTreeStringById(bug, jcombo.getSelectedIndex()+1);
        sliceText.setText(tree);

        jcombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedTree = (String) jcombo.getSelectedItem();
                    int selectId=Integer.parseInt(selectedTree.split(" ")[2]);
                    String tree=getTreeStringById(bug, selectId);
                    sliceText.setText(tree);
                }

            }

        });

        JPanel bottomPanel = new JPanel();
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

        contentPanel.add(labelPanel);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        getContentPane().add(contentPanel);
        this.setSize(500, 350);
    }

    private void addField(JPanel p, GridBagConstraints c, int y, String lbl, JComponent field) {
        c.gridy = y;
        JLabel l = new JLabel(lbl, SwingConstants.TRAILING);
        l.setLabelFor(field);
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 0;
        p.add(l, c);
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 1;
        p.add(field, c);
    }
}

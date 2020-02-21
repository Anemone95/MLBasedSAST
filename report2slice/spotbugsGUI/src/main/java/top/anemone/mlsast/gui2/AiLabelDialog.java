package top.anemone.mlsast.gui2;


import edu.umd.cs.findbugs.log.Logger;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.data.VO.Label;
import top.anemone.mlsast.core.data.taintTree.TaintEdge;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.exception.PredictorRunnerException;
import top.anemone.mlsast.core.exception.SliceRunnerException;
import top.anemone.mlsast.core.predict.PredictRunner;
import top.anemone.mlsast.core.predict.exception.PredictorException;
import top.anemone.mlsast.core.utils.ExceptionUtil;
import top.anemone.mlsast.core.utils.SHA1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.util.Set;

public class AiLabelDialog extends JDialog {
    private void closeDialog(WindowEvent event) {
        closeDialog();
    }

    private void closeDialog() {
        setVisible(false);
        dispose();
    }

    public AiLabelDialog(JFrame parent, Logger l, boolean modal, Set<TaintEdge> flowEdges) {
        super(parent, modal);
        setTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.label_dialog", "Labeling"));
        JPanel contentPanel = new JPanel(new BorderLayout());
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel,BoxLayout.Y_AXIS));
        JLabel label = new JLabel(
                "<html>Choose the shortest subflow where the taint is cleaned:<br/>(Program will automatically judge if there is a FP)<br/></html>"
                );
        label.setSize(700, 20);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelPanel.add(label);

        JComboBox<TaintEdge> jcombo = new JComboBox<TaintEdge>(flowEdges.toArray(new TaintEdge[]{}));
        jcombo.setSize(700, 40);
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
        TaintEdge edge = (TaintEdge) jcombo.getSelectedItem();
        sliceText.setText(
                "Hash: \n" + AiProject.getInstance().getSliceProject().getSliceHash(edge) +"\n\n"+
                "Slice: \n" + AiProject.getInstance().getSliceProject().getSlice(edge));

        jcombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    TaintEdge edge = (TaintEdge) jcombo.getSelectedItem();

                    ;
                    sliceText.setText(
                            "Hash: \n" + AiProject.getInstance().getSliceProject().getSliceHash(edge) +"\n\n"+
                            "Slice: \n" + AiProject.getInstance().getSliceProject().getSlice(edge)
                    );
                }

            }

        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JButton(new AbstractAction("Submit") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                TaintEdge edge = (TaintEdge) jcombo.getSelectedItem();
                Label label = new Label(MainFrame.getInstance().getProject().toString(), AiProject.getInstance().getSliceProject().getSliceHash(edge), true);
                label.setTaintEdge(edge);
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
        this.setSize(500, 450);
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

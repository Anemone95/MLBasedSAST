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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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
        JPanel labelPanel = new JPanel(new FlowLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = c.ipady = 5;

        JLabel label = new JLabel(
                "<html>Choose the shortest subflow where the taint is cleaned.<br/>Program will automatically judge if there is a FP)</html>",
                SwingConstants.TRAILING);
        label.setSize(500, 0);
        JComboBox<TaintEdge> jcombo = new JComboBox<TaintEdge>(flowEdges.toArray(new TaintEdge[]{}));
        jcombo.setSize(500, 0);
        labelPanel.add(label);
        labelPanel.add(jcombo);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JButton(new AbstractAction("Submit") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                TaintEdge edge = (TaintEdge) jcombo.getSelectedItem();
                Label label = new Label(MainFrame.getInstance().getProject().toString(), edge.sha1(), true);
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
                                        public void process(int idx, int totalWork, Object input, Object output, Exception exception) { }
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
        this.setSize(500, 150);
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

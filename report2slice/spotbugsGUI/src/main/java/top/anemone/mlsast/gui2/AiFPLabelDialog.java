package top.anemone.mlsast.gui2;


import edu.umd.cs.findbugs.log.Logger;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.data.VO.Label;
import top.anemone.mlsast.core.data.taintTree.TaintFlow;
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
import java.util.Set;

public class AiFPLabelDialog extends JDialog {
    private void closeDialog(WindowEvent event) {
        closeDialog();
    }

    private void closeDialog() {
        setVisible(false);
        dispose();
    }


    public AiFPLabelDialog(JFrame parent, Logger l, boolean modal, Set<TaintFlow> flowEdges) {
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

        JPanel choosePannel=new JPanel();
        choosePannel.setSize(700, 20);
        JComboBox<TaintFlow> jcombo = new JComboBox<TaintFlow>(flowEdges.toArray(new TaintFlow[]{}));
//        jcombo.setSize(1700, 20);
        jcombo.setPreferredSize(new Dimension(480,20));
        jcombo.setMaximumSize(new Dimension(480,20));
        jcombo.setMinimumSize(new Dimension(480,20));
        jcombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        choosePannel.add(jcombo);
        labelPanel.add(choosePannel);


        JLabel nonce = new JLabel("<html><br/></html>");
        nonce.setSize(700, 5);
        nonce.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelPanel.add(nonce);

        JTextArea sliceText = new JTextArea();
        sliceText.setEditable(false);
        sliceText.setSize(700,800);
        JScrollPane sliceScroll=new JScrollPane(sliceText);
        labelPanel.add(sliceScroll);
        TaintFlow edge = (TaintFlow) jcombo.getSelectedItem();
        sliceText.setText(
                "Hash: \n" + AiProject.getInstance().getSliceProject().getSliceHash(edge) +"\n\n"+
                "Slice: \n" + AiProject.getInstance().getSliceProject().getSlice(edge));

        jcombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    TaintFlow edge = (TaintFlow) jcombo.getSelectedItem();

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
                TaintFlow edge = (TaintFlow) jcombo.getSelectedItem();
                Label label = new Label(MainFrame.getInstance().getProject().toString(), AiProject.getInstance().getSliceProject().getSliceHash(edge), true);
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

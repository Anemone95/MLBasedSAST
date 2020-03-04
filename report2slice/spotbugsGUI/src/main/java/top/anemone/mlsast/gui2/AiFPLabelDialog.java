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
import javax.swing.border.EmptyBorder;
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
        this.setSize(700, 400);
        setTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.label_dialog", "Labeling"));
        setLayout(new BorderLayout());

        JPanel gridPanel = new JPanel(new BorderLayout());
        gridPanel.setBorder(new EmptyBorder(3, 6, 3, 6));
        gridPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();


        JLabel labelText = new JLabel(
                "<html>Choose the shortest subflow where the taint is cleaned:<br/>(Program will automatically judge if there is a FP)<br/></html>"
        );
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.6;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 3, 5, 3);
        gridPanel.add(labelText, gbc);

        JComboBox<TaintFlow> jcombo = new JComboBox<TaintFlow>(flowEdges.toArray(new TaintFlow[]{}));
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

        TaintFlow edge = (TaintFlow) jcombo.getSelectedItem();
        sliceText.setText(
                "Hash: \n" + AiProject.getInstance().getSliceProject().getSliceHash(edge) +"\n\n"+
                "Slice: \n" + AiProject.getInstance().getSliceProject().getSlice(edge));
        sliceText.setCaretPosition(0);

        jcombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                TaintFlow edge1 = (TaintFlow) jcombo.getSelectedItem();
                sliceText.setText(
                        "Hash: \n" + AiProject.getInstance().getSliceProject().getSliceHash(edge1) +"\n\n"+
                        "Slice: \n" + AiProject.getInstance().getSliceProject().getSlice(edge1)
                );
                sliceText.setCaretPosition(0);
            }

        });

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


        getContentPane().add(gridPanel);
    }

}

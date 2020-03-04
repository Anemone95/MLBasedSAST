package top.anemone.mlsast.gui2;

import edu.umd.cs.findbugs.L10N;
import edu.umd.cs.findbugs.log.Logger;
import top.anemone.mlsast.core.predict.impl.BLSTMRemotePredictor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class AiSetServerDialog extends javax.swing.JDialog {

    private void closeDialog(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_closeDialog
        closeDialog();
    }// GEN-LAST:event_closeDialog

    private void closeDialog() {
        setVisible(false);
        dispose();
    }
    public AiSetServerDialog(JFrame parent, Logger l, boolean modal) {
        super(parent, modal);
        // this.parent = parent;

        setTitle(L10N.getLocalString("dlg.set_server_dialog", "Set Server"));
        JPanel contentPanel = new JPanel(new FlowLayout());
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gridConstraints = new GridBagConstraints();
//        gridConstraints.ipadx = gridConstraints.ipady = 5;


        float currFS = Driver.getFontSize();

        JTextField tabTextField = new JTextField(AiProject.getInstance().getRemotePredictor().getRemoteServer());
        tabTextField.setPreferredSize(new Dimension((int) (currFS * 15), (int) (currFS * 2)));
        addField(mainPanel, gridConstraints, 0, L10N.getLocalString("dlg.input_url", "Input Server URL: "), tabTextField);

        JTextField tokenTextField = new JTextField();
        tokenTextField.setPreferredSize(new Dimension((int) (currFS * 15), (int) (currFS * 2)));
        addField(mainPanel, gridConstraints, 1, L10N.getLocalString("dlg.input_token","Input token: "), tokenTextField);

        contentPanel.add(mainPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JButton(new AbstractAction("Verify") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                boolean isAlive = new BLSTMRemotePredictor(tabTextField.getText(), tokenTextField.getText()).isAlive();
                if (isAlive) {
                    JOptionPane.showMessageDialog(MainFrame.getInstance(), "Remote is alive!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MainFrame.getInstance(), "Verify failed!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }));

        bottomPanel.add(new JButton(new AbstractAction("Apply") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                AiProject.getInstance().setRemotePredictor(new BLSTMRemotePredictor(tabTextField.getText(), tokenTextField.getText()));
                closeDialog();
            }
        }));

        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        getContentPane().add(contentPanel);
        this.setSize(380, 160);
        this.setResizable(false);
    }

    private void addField(JPanel p, GridBagConstraints gridConstraints, int y, String lbl, JComponent textField) {
        JLabel l = new JLabel(lbl, SwingConstants.TRAILING);
        l.setLabelFor(textField);
        gridConstraints.anchor = GridBagConstraints.LINE_END;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = y;
        gridConstraints.insets = new Insets(3, 3, 3, 3);
        p.add(l, gridConstraints);
        gridConstraints.anchor = GridBagConstraints.LINE_START;
        gridConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridConstraints.gridx = 1;
        p.add(textField, gridConstraints);
    }
}

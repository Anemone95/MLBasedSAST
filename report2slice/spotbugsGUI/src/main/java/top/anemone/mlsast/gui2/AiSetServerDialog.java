package top.anemone.mlsast.gui2;

import edu.umd.cs.findbugs.log.Logger;
import top.anemone.mlsast.core.predict.impl.LSTMRemotePredictor;

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

        setTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.set_server_dialog", "Set Server"));
        JPanel contentPanel = new JPanel(new BorderLayout());
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = c.ipady = 5;


        float currFS = Driver.getFontSize();

        JTextField tabTextField = new JTextField(AiProject.getInstance().getRemotePredictor().getRemoteServer());
        tabTextField.setPreferredSize(new Dimension((int) (currFS * 14), (int) (currFS * 2)));
        addField(mainPanel, c, 0, "Input Server URL: ", tabTextField);

        JTextField tokenTextField = new JTextField();
        tokenTextField.setPreferredSize(new Dimension((int) (currFS * 14), (int) (currFS * 2)));
        addField(mainPanel, c, (int) (currFS * 2.5), "Input token: ", tokenTextField);

        contentPanel.add(mainPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JButton(new AbstractAction("Verify") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                boolean isAlive = new LSTMRemotePredictor(tabTextField.getText(), tokenTextField.getText()).isAlive();
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
                AiProject.getInstance().setRemotePredictor(new LSTMRemotePredictor(tabTextField.getText(), tokenTextField.getText()));
                closeDialog();
            }
        }));

        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        getContentPane().add(contentPanel);
        this.setSize(400, 200);

//        initComponents();

//        try {
//            aboutEditorPane.setPage(AboutDialog.class.getClassLoader().getResource("help/About.html"));
//            licenseEditorPane.setPage(AboutDialog.class.getClassLoader().getResource("help/License.html"));
//            acknowldgementsEditorPane.setPage(AboutDialog.class.getClassLoader().getResource("help/Acknowledgements.html"));
//        } catch (IOException e) {
//            l.logMessage(Logger.ERROR, e.toString());
//        }

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

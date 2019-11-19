package top.anemone.mlBasedSAST.spotbugs.gui2;

import edu.umd.cs.findbugs.log.Logger;
import top.anemone.mlBasedSAST.data.AIBasedSpotbugProject;
import top.anemone.mlBasedSAST.remote.LSTMServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

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

        JTextField tabTextField = new JTextField(AIBasedSpotbugProject.getInstance().getServer().getRemoteServer());
        tabTextField.setPreferredSize(new Dimension((int) (currFS * 14), (int) (currFS * 2)));
        addField(mainPanel, c, 0, "Input Server URL: ", tabTextField);

        contentPanel.add(mainPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JButton(new AbstractAction("Verify") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                boolean isAlive = new LSTMServer(tabTextField.getText()).isAlive();
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
                AIBasedSpotbugProject.getInstance().setServer(new LSTMServer(tabTextField.getText()));
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

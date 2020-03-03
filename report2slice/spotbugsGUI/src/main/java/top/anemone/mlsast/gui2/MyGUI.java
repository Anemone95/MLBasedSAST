package top.anemone.mlsast.gui2;

import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.launchGUI.LaunchGUI;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.core.utils.JarUtil;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;

public class MyGUI extends LaunchGUI {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyGUI.class);
    private static String findsecbugsPluginPath = JarUtil.getPath() + "/contrib/findsecbugs-plugin.jar";

    public static void main(String[] args) {
        launchGUI(null);
    }

    public static void launchGUI(SortedBugCollection bugs) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Running in GUIbk headless mode, can't open GUIbk");
        }
        GUISaveState.loadInstance();

        try {
            BeautyEyeLNFHelper.frameBorderStyle = BeautyEyeLNFHelper.FrameBorderStyle.osLookAndFeelDecorated;
            BeautyEyeLNFHelper.launchBeautyEyeLNF();

            Font font = (Font) UIManager.get("Menu.font");
            InitGlobalFont(font);

            if (SystemProperties.getProperty("os.name").startsWith("Mac")) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "SpotBugs");
                Debug.println("Mac OS detected");
            }


            URL findsecbugsURL = new File(findsecbugsPluginPath).toURL();
            FindBugsLayoutManagerFactory factory = new FindBugsLayoutManagerFactory(SplitLayout.class.getName());
            MainFrame.makeInstance(factory);
            MainFrame instance = MainFrame.getInstance();
            instance.waitUntilReady();
            // load and enable find-sec-bugs for project
            Plugin plugin = Plugin.loadCustomPlugin(Objects.requireNonNull(findsecbugsURL),
                    instance.getCurProject());
            GUISaveState guiSaveState = GUISaveState.getInstance();
            // add to FBGUI custom plugins list
            guiSaveState.addCustomPlugin(findsecbugsURL);
            // add to list of enabled plugins
            guiSaveState.setPluginEnabled(plugin.getPluginId());
            plugin.setGloballyEnabled(true);
            guiSaveState.save();

            // will raise NPE
            instance.openBugCollection(bugs);
        } catch (NullPointerException e) {
            LOGGER.warn("No bug file");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static void InitGlobalFont(Font font) {
        FontUIResource fontRes = new FontUIResource(font);
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontRes);
            }
        }
    }
}

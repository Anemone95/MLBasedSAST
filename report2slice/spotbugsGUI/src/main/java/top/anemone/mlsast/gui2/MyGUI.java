package top.anemone.mlsast.gui2;

import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.launchGUI.LaunchGUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.core.utils.JarUtil;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.Objects;

public class MyGUI extends LaunchGUI{

    private static final Logger LOGGER = LoggerFactory.getLogger(MyGUI.class);
    private static String findsecbugsPluginPath = JarUtil.getPath()+ "/contrib/findsecbugs-plugin-1.10.1.jar";
    public static void main(String[] args) {
        launchGUI(null);
    }

    public static void launchGUI(SortedBugCollection bugs) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Running in GUIbk headless mode, can't open GUIbk");
        }
        GUISaveState.loadInstance();

        try {
            URL findsecbugsURL= new File(findsecbugsPluginPath).toURL();
            FindBugsLayoutManagerFactory factory = new FindBugsLayoutManagerFactory(SplitLayout.class.getName());
            MainFrame.makeInstance(factory);
            MainFrame instance = MainFrame.getInstance();
            instance.waitUntilReady();
            // load and enable find-sec-bugs for project
            Plugin plugin = Plugin.loadCustomPlugin(Objects.requireNonNull(findsecbugsURL),
                    instance.getCurProject());
            GUISaveState guiSaveState = GUISaveState.getInstance();
            URL url = findsecbugsURL;
            // add to FBGUI custom plugins list
            guiSaveState.addCustomPlugin(url);
            // add to list of enabled plugins
            guiSaveState.setPluginEnabled(plugin.getPluginId());
            plugin.setGloballyEnabled(true);
            guiSaveState.save();

            // will raise NPE
            instance.openBugCollection(bugs);
        } catch (NullPointerException e){
            LOGGER.warn("No bug file");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}

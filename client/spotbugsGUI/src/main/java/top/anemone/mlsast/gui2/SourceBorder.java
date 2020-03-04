package top.anemone.mlsast.gui2;

import org.jb2011.lnf.beautyeye.widget.border.BERoundBorder;

import java.awt.*;

public class SourceBorder extends BERoundBorder {
    public SourceBorder(Color color, int width) {
        super(color, 2);
        this.arcWidth = width;
    }
}

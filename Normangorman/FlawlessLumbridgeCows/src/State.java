import org.osbot.rs07.api.ui.Message;

import java.awt.*;

/**
 * Created by Ben on 08/07/2015.
 */
public interface State {
    public void loop();

    public String getDescription();

    default void handleMessage(Message msg) {}

    default void paint(Graphics2D g2d) {}
}

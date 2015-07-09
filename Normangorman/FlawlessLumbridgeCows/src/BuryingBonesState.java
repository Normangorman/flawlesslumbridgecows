import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.ui.Message;

/**
 * Created by Ben on 08/07/2015.
 */
public class BuryingBonesState implements State {
    CowKillerScript script;
    boolean waitingForBuryCompletionMessage = false;

    public BuryingBonesState(CowKillerScript s) {
        script = s;
    }

    public String getDescription() {
        return "Burying bones";
    }

    public void loop() {
        if (waitingForBuryCompletionMessage) {
            return;
        }

        if (script.inventory.contains("Bones")) {
            script.log("There are still bones left in the inventory. Burying them.");
            Item bones = script.inventory.getItem("Bones");
            bones.interact("Bury");
            waitingForBuryCompletionMessage = true;
        }
        else {
            // Sometimes these are picked up accidentally.
            script.log("Finished burying bones.");
            script.inventory.dropAll("Raw beef");
            script.inventory.dropAll("Cowhide");
            script.changeState(new IdleState(script));
        }
    }

    public void handleMessage(Message msg) {
        if (msg.getMessage().equals("You bury the bones.")) {
            waitingForBuryCompletionMessage = false;
            script.logBonesBuried();
        }
    }
}

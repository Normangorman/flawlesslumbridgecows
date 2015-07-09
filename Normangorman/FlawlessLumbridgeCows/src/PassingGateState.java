import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.RS2Object;

import java.util.Arrays;

/**
 * Created by Ben on 08/07/2015.
 */
public class PassingGateState implements State {
    CowKillerScript script;

    public PassingGateState(CowKillerScript s) {
        script = s;
    }

    public String getDescription() {
        return "Passing gate";
    }

    public void loop() {
        script.log("Attempting to pass gate.");

        RS2Object gate;
        // Keep trying to open until it is open.
        while(!isGateOpen(gate = script.getObjects().closest("Gate"))) {
            gate.interact("Open");

            try {
                script.sleep(200);
            } catch (InterruptedException e) {
                script.log(e.getMessage());
            }
        }

        // Walk into the field
        if (script.getCurrentCowField() == CowConstants.CowField.WEST) {
            script.log("Entering western field.");
            script.localWalker.walkPath(new Position[] {CowConstants.WEST_FIELD_INNER_POS});

        }
        else {
            script.log("Entering eastern field.");
            script.localWalker.walkPath(new Position[] {CowConstants.EAST_FIELD_INNER_POS});
        }

        if (script.isWithinCurrentField(script.myPosition())) {
            script.log("Entered field successfully");
            script.changeState(new IdleState(script));
            return;
        }
    }

    private boolean isGateOpen(RS2Object gate) {
        return !Arrays.asList(gate.getDefinition().getActions()).contains("Open");
    }
}

import org.osbot.rs07.api.map.Position;

import java.util.Random;

/**
 * Created by Ben on 08/07/2015.
 */
public class RunningToCowsState implements State {
    CowKillerScript script;

    public RunningToCowsState(CowKillerScript s) {
        script = s;
    }

    public String getDescription() {
        return "Running to cow field";
    }

    public void loop() {
        // Choose a random path to the current field and walk it.
        Position[][] random_path_choices;
        if (script.getCurrentCowField() == CowConstants.CowField.WEST) {
            script.log("Running to west field.");
            random_path_choices = CowConstants.PATHS_TO_WEST_FIELD;
        }
        else {
            script.log("Running to east field.");
            random_path_choices = CowConstants.PATHS_TO_EAST_FIELD;
        }

        Position[] random_path = random_path_choices[new Random().nextInt(3)];
        script.localWalker.walkPath(random_path);

        script.log("Finished walking path.");
        script.changeState(new PassingGateState(script));
    }
}

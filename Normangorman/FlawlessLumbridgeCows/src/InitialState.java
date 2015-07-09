import org.osbot.rs07.api.ui.Skill;

/**
 * Created by Ben on 08/07/2015.
 */
public class InitialState implements State {
    CowKillerScript script;

    public InitialState(CowKillerScript s) {
        script = s;
    }

    public String getDescription() {
        return "Initial";
    }

    public void loop() {
        script.experienceTracker.start(script.getTrainingSkill());

        if (script.getShouldBuryBones()) {
            script.experienceTracker.start(Skill.PRAYER);
        }

        script.equipItems();

        if (CowConstants.WEST_FIELD_AREA.contains(script.myPosition())) {
            script.setCurrentCowField(CowConstants.CowField.WEST);
            script.changeState(new IdleState(script));
            return;
        }
        else if (CowConstants.EAST_FIELD_AREA.contains(script.myPosition())) {
            script.setCurrentCowField(CowConstants.CowField.EAST);
            script.changeState(new IdleState(script));
            return;
        }
        else {
            // Assume we're in Lumbridge. Randomly select a field.
            script.randomlySwapCowFields();
            script.changeState(new RunningToCowsState(script));
            return;
        }
    }
}

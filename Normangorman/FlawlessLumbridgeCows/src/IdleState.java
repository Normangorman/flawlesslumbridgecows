import org.osbot.rs07.api.model.NPC;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ben on 08/07/2015.
 */
public class IdleState implements State {
    CowKillerScript script;

    public IdleState(CowKillerScript s) {
        script = s;
    }

    public String getDescription() {
        return "Idle";
    }

    public void loop() {
        // Error check being in correct field.
        if (!script.isWithinCurrentField(script.myPosition())) {
            script.warn("Out of field for some reason. Going back in.");
            script.changeState(new RunningToCowsState(script));
            return;
        }

        List<NPC> outOfCombatCows = new ArrayList<>();

        for (NPC npc : script.npcs.getAll()) {
            // Since we're looping through all npcs here, error check that none are attacking us.
            if (npc.getInteracting() != null && npc.getInteracting().getId() == script.myPlayer().getId()) {
                script.warn("ERROR - Idle state invalidated because we are being attacked.");
                script.changeState(new MovingToTargetState(script, npc, script.getTrainingSkillXP()));
                return;
            }

            boolean is_cow = npc.getName().equals("Cow") || npc.getName().equals("Cow calf");
            boolean out_of_combat = !npc.isUnderAttack();
            boolean in_field = script.isWithinCurrentField(npc.getPosition());

            if (is_cow && out_of_combat && in_field) {
                outOfCombatCows.add(npc);
            }
        }

        script.log("Number of out of combat cows detected: " + outOfCombatCows.size());
        if(outOfCombatCows.size() == 0) {
            script.warn("ERROR: No out of combat cows found. This is probably a bug.");
            return;
        }

        // Target the cow that is closest to the player.
        NPC closestCow = outOfCombatCows.get(0);
        int closestDistance = 10000;
        for (NPC cow : outOfCombatCows) {
            int dist = script.myPosition().distance(cow.getPosition());
            if (dist < closestDistance) {
                closestCow = cow;
                closestDistance = dist;
            }
        }

        script.log("Closest cow found. Attacking it...");
        if (!closestCow.isVisible()) {
            script.camera.toEntity(closestCow);
        }

        closestCow.interact("Attack");
        script.changeState(new MovingToTargetState(script, closestCow, script.getTrainingSkillXP()));
    }
}

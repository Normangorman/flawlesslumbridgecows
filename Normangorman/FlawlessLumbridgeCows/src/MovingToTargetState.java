import org.osbot.rs07.api.model.*;
import org.osbot.rs07.api.model.Character;
import org.osbot.rs07.api.ui.Message;

import java.awt.*;

/**
 * Created by Ben on 08/07/2015.
 */
public class MovingToTargetState implements State {
    CowKillerScript script;
    NPC target;
    int startingTrainingSkillXP;

    public MovingToTargetState(CowKillerScript s, NPC target, int startingTrainingSkillXP) {
        script = s;
        this.target = target;
        // Used to determine whether we hit a cow:
        this.startingTrainingSkillXP = startingTrainingSkillXP;
    }

    public String getDescription() {
        return "Moving to target";
    }

    public void loop() {
        script.log("MOVING_TO_TARGET_STATE: Looping");

        if (!target.isVisible()) {
            script.camera.toEntity(target);
        }

        if (target.getHealth() == 0) {
           // Possibility 1: The cow is dead and I killed it.
           if (script.getTrainingSkillXP() > startingTrainingSkillXP) {
               script.logCowKill();
               script.changeState(new IdleState(script));
               return;
           }
           else {
               // Possibility 2: The cow is dead and someone else killed it.
               script.log("Someone else must have killed the cow.");
               script.changeState(new IdleState(script));
               return;
           }
        }

        Character targetInteractee = target.getInteracting();
        // Possibility 3: The cow is not interacting with anyone. We are still running to it.
        if (targetInteractee == null) {
            script.log("Target interactee is null. Returning.");
            target.interact("Attack");
            return;
        }
        else {
            int myId = script.myPlayer().getId();

           // Possibility 4: Cow is interacting with me and thus combat is about to start.
           if (targetInteractee.getId() == myId) {
               script.log("Hit cow but did not kill it. Switching to combat state.");
               script.changeState(new CombatState(script, target));
               return;
           }
           else {
               // Possibility 5: Cow is interacting with someone else.
               script.log("Target cow is attacking someone else. Going back to idle state.");
               script.changeState(new IdleState(script));
               return;
           }
       }
    }

    public void handleMessage(Message msg) {
        if (msg.getMessage().equals("Someone else is fighting that.")) {
            script.log("Someone else got to the cow before I did.");
            script.changeState(new IdleState(script));
        }
    }

    public void paint(Graphics2D g2d) {
        g2d.drawPolygon(target.getPosition().getPolygon(script.bot));
    }
}

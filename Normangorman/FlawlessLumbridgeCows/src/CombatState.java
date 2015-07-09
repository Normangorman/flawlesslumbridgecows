import org.osbot.rs07.api.model.*;
import org.osbot.rs07.api.model.Character;

import java.awt.*;

/**
 * Created by Ben on 08/07/2015.
 */
public class CombatState implements State {
    CowKillerScript script;
    NPC target;

    public CombatState(CowKillerScript s, NPC target) {
        script = s;
        this.target = target;
    }

    public String getDescription() {
        return "In combat";
    }

    public void loop() {
        if (target.getHealth() == 0) {
            script.log("Target cow is dead.");

            // If we care about looting, then wait for the death animation to finish.
            // If not then just go back to idle.
            if (script.getShouldBuryBones()) {
                if (target.exists()) {
                    script.log("Waiting for target death animation to finish.");
                    return;
                }
                else {
                    script.log("Target death animation is finished. Beginning looting.");
                    script.logCowKill();
                    script.changeState(new LootingState(script, target));
                    return;
                }
            }
            else {
                script.log("Not bothering with looting.");
                script.logCowKill();
                script.changeState(new IdleState(script));
                return;
            }
        }
        else {
            // Validate that I am still attacking.
            if (script.myPlayer().isAnimating()
                    || script.myPlayer().isUnderAttack()
                    || script.myPlayer().isInteracting(target)) {
                // Everything's fine
                return;
            }

            // Validate that cow is interacting with me.
            Character targetInteractee = target.getInteracting();
            int myId = script.myPlayer().getId();
            if (targetInteractee == null || !(targetInteractee.getId() == myId)) {
                script.warn("ERROR: Combat state invalidated due to target not attacking me.");
                script.changeState(new IdleState(script));
                return;
            }

            script.warn("ERROR: Combat state invalidated due to lack mutual interaction.");
            script.changeState(new IdleState(script));
            return;
        }
    }

    public void paint(Graphics2D g2d) {
        g2d.drawPolygon(target.getPosition().getPolygon(script.bot));
    }
}

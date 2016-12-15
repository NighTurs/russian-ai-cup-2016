import model.*;

import java.util.*;
import java.util.stream.Collectors;

public class WizardControl {

    private static final double MAX_SPEED_BOOST_FACTOR = 1.5;
    private static final int SHADOW_TICKS_TO_EXPIRE = 200;
    private final List<WizardProxy> shadowWizardsForCurrentTurn;

    public WizardControl(Wizard self, Memory memory, World world, Game game) {
        Map<Long, WizardProxy> shadowWizards = memory.getShadowWizards();
        List<LivingUnit> allyUnits = WorldProxy.allyAllyUnits(self, world);
        List<WizardProxy> newShadowWizards = new ArrayList<>();
        outer_loop:
        for (WizardProxy shadowWizard : shadowWizards.values()) {
            if (world.getTickIndex() - shadowWizard.getShadowImageTick() >= SHADOW_TICKS_TO_EXPIRE) {
                continue;
            }
            for (LivingUnit ally : allyUnits) {
                if (visionRange(ally) >= ally.getDistanceTo(shadowWizard)) {
                    continue outer_loop;
                }
            }
            int[] remainingCooldownTicksByAction = shadowWizard.getRemainingCooldownTicksByAction();
            int remainingActionCooldownTicks = Math.max(0, shadowWizard.getRemainingActionCooldownTicks() - 1);
            for (int i = 0; i < remainingCooldownTicksByAction.length; i++) {
                remainingCooldownTicksByAction[i] = Math.max(0, remainingCooldownTicksByAction[i] - 1);
            }

            for (Projectile projectile : world.getProjectiles()) {
                if (projectile.getOwnerUnitId() != shadowWizard.getId()) {
                    continue;
                }

                ProjectileType type = projectile.getType();
                int cooldown = CastProjectileTacticBuilders.projectileCooldown(type, shadowWizard, game);
                double speed = CastProjectileTacticBuilders.projectileMoveSpeed(game, type);
                double dist = shadowWizard.getDistanceTo(projectile);
                int ticksTraveled = (int) (dist / speed);
                remainingCooldownTicksByAction[CastProjectileTacticBuilders.projectileActionType(type).ordinal()] =
                        Math.max(0, cooldown - ticksTraveled);
                remainingActionCooldownTicks = Math.max(0, game.getWizardActionCooldownTicks() - ticksTraveled);
            }

            newShadowWizards.add(WizardProxy.shadowWizard(shadowWizard.getWizard(),
                    shadowWizard.getShadowImageTick(),
                    shadowWizard.getX(),
                    shadowWizard.getY(),
                    remainingActionCooldownTicks,
                    remainingCooldownTicksByAction,
                    shadowWizard.getMana() + shadowWizard.getWizardManaPerTurn(game),
                    world,
                    game));
        }
        shadowWizards.clear();
        newShadowWizards.forEach(x -> shadowWizards.put(x.getId(), x));
        for (Wizard wizard : world.getWizards()) {
            if (self.getFaction() == wizard.getFaction()) {
                continue;
            }
            shadowWizards.remove(wizard.getId());
            LivingUnit revealedBy = null;
            double minRevealedBy = Double.MAX_VALUE;
            for (LivingUnit ally : allyUnits) {
                double untilHidden = visionRange(ally) - ally.getDistanceTo(wizard);
                if (untilHidden >= 0 && minRevealedBy > untilHidden) {
                    minRevealedBy = untilHidden;
                    revealedBy = ally;
                }
            }
            if (revealedBy == null) {
                throw new RuntimeException("Enemy wizard should not be seen, id " + wizard.getId());
            }
            Point potentialHidingPoint = MathMethods.distPoint(revealedBy.getX(),
                    revealedBy.getY(),
                    wizard.getX(),
                    wizard.getY(),
                    revealedBy.getDistanceTo(wizard) + game.getWizardForwardSpeed() * MAX_SPEED_BOOST_FACTOR);
            WizardProxy shadowWizard = WizardProxy.shadowWizard(wizard,
                    world.getTickIndex(),
                    potentialHidingPoint.getX(),
                    potentialHidingPoint.getY(),
                    wizard.getRemainingActionCooldownTicks(),
                    wizard.getRemainingCooldownTicksByAction(),
                    wizard.getMana(),
                    world,
                    game);
            shadowWizards.put(shadowWizard.getId(), shadowWizard);
        }
        this.shadowWizardsForCurrentTurn = Collections.unmodifiableList(shadowWizards.values()
                .stream()
                .filter(x -> x.getShadowImageTick() != world.getTickIndex())
                .collect(Collectors.toList()));
    }

    public List<WizardProxy> shadowWizardsForCurrentTurn() {
        return shadowWizardsForCurrentTurn;
    }

    private double visionRange(LivingUnit unit) {
        if (unit instanceof Wizard) {
            return ((Wizard) unit).getVisionRange();
        } else if (unit instanceof Minion) {
            return ((Minion) unit).getVisionRange();
        } else if (unit instanceof Building) {
            return ((Building) unit).getVisionRange();
        } else {
            throw new RuntimeException("Unexpected unit type " + unit.getClass());
        }
    }
}

import model.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class CastProjectileTacticBuilders {

    private static final double E = 1e-6;
    private static final Map<ActionType, Integer> manaPriorities;

    static {
        manaPriorities = new EnumMap<>(ActionType.class);
        manaPriorities.put(ActionType.FIREBALL, 2);
        manaPriorities.put(ActionType.MAGIC_MISSILE, 1);
    }

    private CastProjectileTacticBuilders() {
        throw new UnsupportedOperationException("Instance not supported");
    }

    public static double castRangeToBuilding(WizardProxy self, Building building, Game game) {
        return self.getCastRange() + building.getRadius() + game.getMagicMissileRadius();
    }

    public static double castRangeToMinion(WizardProxy self, Minion minion, Game game) {
        return self.getCastRange() + minion.getRadius();
    }

    public static boolean inCastSector(TurnContainer turnContainer, Point point) {
        WizardProxy self = turnContainer.getSelf();
        if (Math.abs(self.getX() - point.getX()) < E && Math.abs(self.getY() - point.getY()) < E) {
            return true;
        } else {
            double angle = self.getAngleTo(point.getX(), point.getY());
            return WizardProxy.getWizardCastSector(turnContainer.getGame()) > Math.abs(angle);
        }
    }

    public static boolean shouldSaveUpMana(TurnContainer turnContainer, ActionType actionType) {
        if (actionType != ActionType.MAGIC_MISSILE) {
            throw new RuntimeException("Not supported action type");
        }
        Game game = turnContainer.getGame();
        WizardProxy self = turnContainer.getSelf();
        if (!game.isSkillsEnabled()) {
            return false;
        }
        int priority = manaPriorities.get(actionType);
        int manaCost = game.getMagicMissileManacost();
        //noinspection RedundantIfStatement
        if (self.isSkillLearned(SkillType.FIREBALL) && manaPriorities.get(ActionType.FIREBALL) > priority &&
                self.getMana() +
                        untilNextProjectile(self, ProjectileType.FIREBALL, game) * self.getWizardManaPerTurn(game) -
                        manaCost < game.getFireballManacost()) {
            return true;
        }
        return false;
    }

    public static Optional<Unit> bestFocusTarget(TurnContainer turnContainer, double castRangeBoost) {
        WorldProxy world = turnContainer.getWorldProxy();
        WizardProxy self = turnContainer.getSelf();

        Unit bestUnit = null;
        int lowestLife = Integer.MAX_VALUE;
        for (WizardProxy wizard : world.getWizards()) {
            if (!turnContainer.isOffensiveWizard(wizard) || !wizard.isRealOrFreshShadow()) {
                continue;
            }
            double dist = wizard.getDistanceTo(self);
            if (dist <= turnContainer.getCastRangeService()
                    .castRangeToWizardPessimistic(self, wizard, turnContainer.getGame(), ProjectileType.MAGIC_MISSILE)
                    .getDistToCenter() && lowestLife > wizard.getLife() + castRangeBoost) {
                lowestLife = wizard.getLife();
                bestUnit = wizard;
            }
        }
        if (bestUnit != null) {
            return Optional.of(bestUnit);
        }

        for (Minion minion : world.getMinions()) {
            if (!turnContainer.isOffensiveMinion(minion)) {
                continue;
            }
            double dist = minion.getDistanceTo(self);
            if (dist <= turnContainer.getGame().getStaffRange() + minion.getRadius()) {
                return Optional.of(minion);
            }
        }

        for (Building building : world.getBuildings()) {
            if (!turnContainer.isOffensiveBuilding(building)) {
                continue;
            }
            double dist = building.getDistanceTo(self);
            if (dist <= CastProjectileTacticBuilders.castRangeToBuilding(self, building, turnContainer.getGame()) +
                    castRangeBoost) {
                return Optional.of(building);
            }
        }

        for (Minion minion : world.getMinions()) {
            if (!turnContainer.isOffensiveMinion(minion)) {
                continue;
            }
            double dist = minion.getDistanceTo(self);
            if (dist <= CastProjectileTacticBuilders.castRangeToMinion(self, minion, turnContainer.getGame()) +
                    castRangeBoost && lowestLife > minion.getLife()) {
                lowestLife = minion.getLife();
                bestUnit = minion;
            }
        }
        return Optional.ofNullable(bestUnit);
    }

    public static double projectileRadius(Game game, ProjectileType projectileType) {
        switch (projectileType) {
            case DART:
                return game.getDartRadius();
            case MAGIC_MISSILE:
                return game.getMagicMissileRadius();
            case FROST_BOLT:
                return game.getFrostBoltRadius();
            case FIREBALL:
                return game.getFireballRadius();
            default:
                throw new RuntimeException("Unexpected projectile type " + projectileType);
        }
    }

    public static double projectileEffectiveRadius(Game game, ProjectileType projectileType) {
        switch (projectileType) {
            case DART:
                return game.getDartRadius();
            case MAGIC_MISSILE:
                return game.getMagicMissileRadius();
            case FROST_BOLT:
                return game.getFrostBoltRadius();
            case FIREBALL:
                return game.getFireballExplosionMinDamageRange();
            default:
                throw new RuntimeException("Unexpected projectile type " + projectileType);
        }
    }

    public static double projectileMoveSpeed(Game game, ProjectileType projectileType) {
        switch (projectileType) {
            case DART:
                return game.getDartSpeed();
            case MAGIC_MISSILE:
                return game.getMagicMissileSpeed();
            case FROST_BOLT:
                return game.getFrostBoltSpeed();
            case FIREBALL:
                return game.getFireballSpeed();
            default:
                throw new RuntimeException("Unexpected projectile type " + projectileType);
        }
    }

    public static int projectileCooldown(ProjectileType projectileType, WizardProxy wizard, Game game) {
        switch (projectileType) {
            case MAGIC_MISSILE:
                return wizard.isSkillLearned(SkillType.ADVANCED_MAGIC_MISSILE) ?
                        0 :
                        game.getMagicMissileCooldownTicks();
            case FIREBALL:
                return game.getFireballCooldownTicks();
            case FROST_BOLT:
                return game.getFrostBoltCooldownTicks();
            default:
                throw new RuntimeException("Unexpected projectile type " + projectileType);
        }
    }

    public static int projectileManacost(ProjectileType projectileType, Game game) {
        switch (projectileType) {
            case MAGIC_MISSILE:
                return game.getMagicMissileManacost();
            case FROST_BOLT:
                return game.getFrostBoltManacost();
            case FIREBALL:
                return game.getFireballManacost();
            default:
                throw new RuntimeException("Unexpected projectile type " + projectileType);
        }
    }

    public static ActionType projectileActionType(ProjectileType projectileType) {
        switch (projectileType) {
            case MAGIC_MISSILE:
                return ActionType.MAGIC_MISSILE;
            case FROST_BOLT:
                return ActionType.FROST_BOLT;
            case FIREBALL:
                return ActionType.FIREBALL;
            default:
                throw new RuntimeException("Unexpected projectile type " + projectileType);
        }
    }

    public static int untilNextProjectile(WizardProxy wizard, ProjectileType projectileType, Game game) {
        ActionType actionType = projectileActionType(projectileType);
        int manaCost = projectileManacost(projectileType, game);
        int untilCooldown = Math.max(wizard.getRemainingCooldownTicksByAction()[actionType.ordinal()],
                wizard.getRemainingActionCooldownTicks());
        return untilCooldown + (int) Math.ceil(
                Math.max(0.0, manaCost - (wizard.getMana() + untilCooldown * wizard.getWizardManaPerTurn(game))) /
                        wizard.getWizardManaPerTurn(game));
    }

    public static boolean isProjectileLearned(TurnContainer turnContainer,
                                              WizardProxy wizard,
                                              ProjectileType projectileType) {
        switch (projectileType) {
            case MAGIC_MISSILE:
                return true;
            case FROST_BOLT:
                return wizard.isSkillLearned(SkillType.FROST_BOLT);
            case FIREBALL:
                return wizard.isSkillLearned(SkillType.FIREBALL);
            default:
                throw new RuntimeException("Unexpected projectile type " + projectileType);
        }
    }

    public static Optional<Double> justInTimeTurn(WizardProxy wizard, Point target, int ticksLeft, Game game) {
        double currentAngle = wizard.getAngleTo(target.getX(), target.getY());
        int ticksToTurn = (int) Math.ceil(Math.max(0,
                Math.abs(currentAngle) + wizard.getWizardMaxTurnAngle(game) - WizardProxy.getWizardCastSector(game)) /
                wizard.getWizardMaxTurnAngle(game));
        if (ticksToTurn < ticksLeft) {
            return Optional.empty();
        } else {
            return Optional.of(currentAngle);
        }
    }
}

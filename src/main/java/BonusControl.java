import model.*;

import java.util.EnumMap;
import java.util.Map;

public class BonusControl {

    private static final double E = 1e-6;
    private final WorldProxy world;
    private final Memory memory;
    private final Game game;
    private final Point topBonusPosition;
    private final Point bottomBonusPosition;

    public BonusControl(Wizard self, WorldProxy world, Game game, Memory memory) {
        double base = world.allyBase().getX();
        this.topBonusPosition = new Point(base * 3, base * 3);
        this.bottomBonusPosition = new Point(world.getWidth() - base * 3, world.getHeight() - base * 3);
        this.world = world;
        this.memory = memory;
        this.game = game;

        if (world.getTickIndex() != 1 && world.getTickIndex() % game.getBonusAppearanceIntervalTicks() == 1) {
            memory.setBottomBonusTaken(false);
            memory.setTopBonusTaken(false);
        }
        checkIfWizardsTookBonuses(topBonusPosition, bottomBonusPosition, world, game, memory);
        if (!memory.isBottomBonusTaken()) {
            memory.setBottomBonusTaken(detectIfBonusIsTaken(bottomBonusPosition, world, game, self, memory));
        }
        if (!memory.isTopBonusTaken()) {
            memory.setTopBonusTaken(detectIfBonusIsTaken(topBonusPosition, world, game, self, memory));
        }
    }

    private static void checkIfWizardsTookBonuses(Point topBonusPosition,
                                                  Point bottomBonusPosition,
                                                  WorldProxy world,
                                                  Game game,
                                                  Memory memory) {
        for (Map<StatusType, Integer> cooldowns : memory.getPastBonusCooldowns().values()) {
            //noinspection KeySetIterationMayUseEntrySet
            for (StatusType type : cooldowns.keySet()) {
                cooldowns.put(type, cooldowns.get(type) - 1);
            }
        }
        for (Wizard wizard : world.getWizards()) {
            if (!memory.getPastBonusCooldowns().containsKey(wizard.getId())) {
                Map<StatusType, Integer> cooldowns = new EnumMap<>(StatusType.class);
                cooldowns.put(StatusType.EMPOWERED, 0);
                cooldowns.put(StatusType.HASTENED, 0);
                cooldowns.put(StatusType.SHIELDED, 0);
                memory.getPastBonusCooldowns().put(wizard.getId(), cooldowns);
            }
            boolean tookBonus = false;
            outer_loop:
            for (Status status : wizard.getStatuses()) {
                switch (status.getType()) {
                    case EMPOWERED:
                        if (memory.getPastBonusCooldowns().get(wizard.getId()).get(StatusType.EMPOWERED) <
                                status.getRemainingDurationTicks()) {
                            tookBonus = true;
                            memory.getPastBonusCooldowns()
                                    .get(wizard.getId())
                                    .put(StatusType.EMPOWERED, status.getRemainingDurationTicks());
                            break outer_loop;
                        }
                        break;
                    case HASTENED:
                        if (status.getRemainingDurationTicks() > game.getHastenedDurationTicks() &&
                                memory.getPastBonusCooldowns().get(wizard.getId()).get(StatusType.HASTENED) <
                                        status.getRemainingDurationTicks()) {
                            tookBonus = true;
                            memory.getPastBonusCooldowns()
                                    .get(wizard.getId())
                                    .put(StatusType.HASTENED, status.getRemainingDurationTicks());
                            break outer_loop;
                        }
                        break;
                    case SHIELDED:
                        if (status.getRemainingDurationTicks() > game.getShieldedDurationTicks() &&
                                memory.getPastBonusCooldowns().get(wizard.getId()).get(StatusType.SHIELDED) <
                                        status.getRemainingDurationTicks()) {
                            tookBonus = true;
                            memory.getPastBonusCooldowns()
                                    .get(wizard.getId())
                                    .put(StatusType.SHIELDED, status.getRemainingDurationTicks());
                            break outer_loop;
                        }
                        break;
                    default:
                }
            }
            if (tookBonus) {
                if (wizard.getDistanceTo(topBonusPosition.getX(), topBonusPosition.getY()) >
                        wizard.getDistanceTo(bottomBonusPosition.getX(), bottomBonusPosition.getY())) {
                    memory.setBottomBonusTaken(true);
                } else {
                    memory.setTopBonusTaken(true);
                }
            }
        }
    }

    private static boolean detectIfBonusIsTaken(Point bonusPoint,
                                                WorldProxy world,
                                                Game game,
                                                Wizard self,
                                                Memory memory) {
        for (Bonus bonus : world.getBonuses()) {
            if (Math.abs(bonus.getX() - bonusPoint.getX()) < E && Math.abs(bonus.getY() - bonusPoint.getY()) < E) {
                return false;
            }
        }
        for (Unit unit : world.allUnitsWoTrees()) {
            if (unit.getFaction() != self.getFaction()) {
                continue;
            }
            double visionRange;
            if (unit instanceof Minion) {
                visionRange = ((Minion) unit).getVisionRange();
            } else if (unit instanceof Building) {
                visionRange = ((Building) unit).getVisionRange();
            } else if (unit instanceof Wizard) {
                visionRange = ((Wizard) unit).getVisionRange();
            } else {
                continue;
            }
            double dist = unit.getDistanceTo(bonusPoint.getX(), bonusPoint.getY());
            if (dist < visionRange) {
                return true;
            }
        }
        return false;
    }

    public int ticksUntilTopBonus() {
        if (!memory.isTopBonusTaken()) {
            return 0;
        } else {
            return nextBonusSpawnTick();
        }
    }

    public int ticksUntilBottomBonus() {
        if (!memory.isBottomBonusTaken()) {
            return 0;
        } else {
            return nextBonusSpawnTick();
        }
    }

    private int nextBonusSpawnTick() {
        return ((world.getTickIndex() - 1) / game.getBonusAppearanceIntervalTicks() + 1) *
                game.getBonusAppearanceIntervalTicks() - world.getTickIndex() + 1;
    }

    public Point topBonusPosition() {
        return topBonusPosition;
    }

    public Point bottomBonusPosition() {
        return bottomBonusPosition;
    }
}

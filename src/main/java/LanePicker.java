import model.Wizard;

public class LanePicker {

    private final WorldProxy world;
    private final Wizard self;
    private final UnitLocationType unitLocationType;

    public LanePicker(WorldProxy world, Wizard self, UnitLocationType unitLocationType) {
        this.world = world;
        this.self = self;
        this.unitLocationType = unitLocationType;
    }

    public LocationType myLane() {
        LocationType location = unitLocationType.getLocationType(self.getId());
        if (location.isLane()) {
            return location;
        } else {
            int mid = 0;
            int bot = 0;
            int top = 0;
            for (Wizard wizard : world.getWizards()) {
                LocationType curType = unitLocationType.getLocationType(wizard.getId());
                switch (curType) {
                    case TOP_LANE:
                        top++;
                        break;
                    case MIDDLE_LANE:
                        mid++;
                        break;
                    case BOTTOM_LANE:
                        bot++;
                        break;
                    default:
                }
            }
            if (mid <= bot && mid <= top) {
                return LocationType.MIDDLE_LANE;
            } else if (bot <= mid && bot <= top) {
                return LocationType.BOTTOM_LANE;
            } else {
                return LocationType.TOP_LANE;
            }
        }
    }
}

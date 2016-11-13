import model.Wizard;

public class LanePicker {

    public static final int UNFIXED_LANE_TICK_THRESHOLD = 500;
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
        if (location.isLane() && world.getTickIndex() > UNFIXED_LANE_TICK_THRESHOLD) {
            return location;
        } else {
            int mid = 0;
            int bot = 0;
            int top = 0;
            for (Wizard wizard : world.getWizards()) {
                if (wizard.getId() == self.getId()) {
                    continue;
                }
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
            if (mid < 2) {
                return LocationType.MIDDLE_LANE;
            } else if (bot < 2) {
                return LocationType.BOTTOM_LANE;
            } else {
                return LocationType.TOP_LANE;
            }
        }
    }
}

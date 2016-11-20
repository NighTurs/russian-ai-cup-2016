import model.Wizard;

public class LanePicker {

    private static final int UNFIXED_LANE_TICK_THRESHOLD = 800;
    private final WorldProxy world;
    private final Wizard self;
    private final MapUtils mapUtils;
    private final Memory memory;

    public LanePicker(WorldProxy world, Wizard self, MapUtils mapUtils, Memory memory) {
        this.world = world;
        this.self = self;
        this.mapUtils = mapUtils;
        this.memory = memory;
    }

    public LocationType myLane() {
        if (world.getTickIndex() < UNFIXED_LANE_TICK_THRESHOLD || memory.getLane() == null) {
            memory.setLane(vacantLane());
            return memory.getLane();
        } else {
            return memory.getLane();
        }
    }

    private LocationType vacantLane() {
        int mid = 0;
        int bot = 0;
        int top = 0;
        for (Wizard wizard : world.getWizards()) {
            if (wizard.getId() == self.getId() || wizard.getFaction() != self.getFaction()) {
                continue;
            }
            LocationType curType = mapUtils.getLocationType(wizard.getId());
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
        if (mid == 0) {
            return LocationType.MIDDLE_LANE;
        } else if (bot == 0) {
            return LocationType.BOTTOM_LANE;
        } else if (top == 0) {
            return LocationType.TOP_LANE;
        } else if (mid < 2) {
            return LocationType.MIDDLE_LANE;
        } else if (bot < 2) {
            return LocationType.BOTTOM_LANE;
        } else if (top < 2) {
            return LocationType.TOP_LANE;
        }
        throw new RuntimeException(String.format("Anomaly top=%s, bot=%s, mid=%s", top, bot, mid));
    }
}

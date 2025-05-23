package ai.timefold.solver.benchmarks.examples.curriculumcourse.domain.solver;

import ai.timefold.solver.benchmarks.examples.curriculumcourse.domain.CourseSchedule;
import ai.timefold.solver.benchmarks.examples.curriculumcourse.domain.Room;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

import java.util.Comparator;

import static java.util.Comparator.comparingInt;

public class RoomStrengthWeightFactory implements
        SelectionSorterWeightFactory<CourseSchedule, Room> {

    @Override
    public RoomStrengthWeight createSorterWeight(CourseSchedule schedule,
                                                 Room room) {
        return new RoomStrengthWeight(room);
    }

    public static class RoomStrengthWeight implements Comparable<RoomStrengthWeight> {

        private static final Comparator<Room> COMPARATOR =
                comparingInt(Room::getCapacity)
                        .thenComparingLong(Room::getId);

        private final Room room;

        public RoomStrengthWeight(Room room) {
            this.room = room;
        }

        @Override
        public int compareTo(RoomStrengthWeight other) {
            return COMPARATOR.compare(room, other.room);
        }
    }
}

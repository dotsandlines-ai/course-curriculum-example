package ai.timefold.solver.benchmarks.examples.curriculumcourse.score;

import ai.timefold.solver.benchmarks.examples.curriculumcourse.domain.*;
import ai.timefold.solver.benchmarks.examples.curriculumcourse.domain.solver.CourseConflict;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class MapIncrementalScoreCalculator implements ConstraintMatchAwareIncrementalScoreCalculator<CourseSchedule, HardSoftScore> {

    // Score
    private final int nc2CacheLimit = 100;
    private Map<Course, List<CourseConflict>> conflictingLessonsDifferentCourse;
    private Map<Course, Map<Period, Integer>> conflictingLecturesSameCourse;
    private Map<Room, Map<Period, Integer>> roomOccupancy;
    private Map<Course, Map<Period, Integer>> unavailablePeriod;
    private Map<Course, Map<Day, Integer>> minimumWorkingDays;
    private Map<Course, Integer> minimumWorkingDaysDelta;
    private Map<Curriculum, Map<Day, List<Integer>>> curriculumCompactness;
    private Map<Course, Map<Room, Integer>> roomStability;
    private int[] nC2CacheArray; // n choose 2 in score so constraints are verifiable via streams
    private int hard;
    private int soft;

    @Override
    public void resetWorkingSolution(@NonNull CourseSchedule solution, boolean b) {
        resetWorkingSolution(solution);
    }

    @Override
    public @NonNull Collection<ConstraintMatchTotal<HardSoftScore>> getConstraintMatchTotals() {
        return List.of();
    }

    @Override
    public @Nullable Map<Object, Indictment<HardSoftScore>> getIndictmentMap() {
        return Map.of();
    }

    @Override
    public void resetWorkingSolution(@NonNull CourseSchedule solution) {
        conflictingLessonsDifferentCourse = new HashMap<>();
        conflictingLecturesSameCourse = new HashMap<>();
        roomOccupancy = new HashMap<>();
        unavailablePeriod = new HashMap<>();
        minimumWorkingDays = new HashMap<>();
        minimumWorkingDaysDelta = new HashMap<>();
        curriculumCompactness = new HashMap<>();
        roomStability = new HashMap<>();
        for (Course course : solution.getCourseList()) {
            conflictingLecturesSameCourse.put(course, new HashMap<>());
            roomStability.put(course, new HashMap<>());
            minimumWorkingDays.put(course, new HashMap<>());
            minimumWorkingDaysDelta.put(course, 0);
            unavailablePeriod.put(course, new HashMap<>());
        }
        for (Room room : solution.getRoomList()) {
            roomOccupancy.put(room, new HashMap<>());
        }
        for (Curriculum curriculum : solution.getCurriculumList()) {
            Map<Day, List<Integer>> dayMap = new HashMap<>();
            for (Day day : solution.getDayList()) {
                int maxTimeslotIndex = 0;
                for (Period period : day.getPeriodList()) {
                    int timeslotIndex = period.getTimeslot().getTimeslotIndex();
                    if (timeslotIndex > maxTimeslotIndex) {
                        maxTimeslotIndex = timeslotIndex;
                    }
                }
                List<Integer> timeslotIndices = new ArrayList<>(Collections.nCopies(maxTimeslotIndex + 1, 0));
                dayMap.put(day, timeslotIndices);
            }
            curriculumCompactness.put(curriculum, dayMap);
        }
        for (UnavailablePeriodPenalty unavailable : solution.getUnavailablePeriodPenaltyList()) {
            Map<Period, Integer> periodMap = unavailablePeriod.get(unavailable.getCourse());
            periodMap.put(unavailable.getPeriod(), periodMap.getOrDefault(unavailable.getPeriod(), 0) + 1);
        }
        for (CourseConflict conflict : solution.calculateCourseConflictList()) {
            conflictingLessonsDifferentCourse.computeIfAbsent(conflict.getLeftCourse(), k -> new ArrayList<>()).add(conflict);
            conflictingLessonsDifferentCourse.computeIfAbsent(conflict.getRightCourse(), k -> new ArrayList<>()).add(conflict);
        }
        nC2CacheArray = new int[nc2CacheLimit + 1];
        for (int freq = 0; freq <= nc2CacheLimit; freq++) {
            nC2CacheArray[freq] = nCr(freq, 2);
        }

        // Empty the schedule, remember rooms and periods
        Map<Lecture, Period> lecturePeriods = new HashMap<>();
        Map<Lecture, Room> lectureRooms = new HashMap<>();
        for (Lecture lecture : solution.getLectureList()) {
            lecturePeriods.put(lecture, lecture.getPeriod());
            lectureRooms.put(lecture, lecture.getRoom());
            lecture.setRoom(null);
            lecture.setPeriod(null);
        }
        hard = 0;
        soft = 0;
        // Insert all rooms and periods into an empty schedule
        for (Lecture lecture : solution.getLectureList()) {
            Room room = lectureRooms.get(lecture);
            lecture.setRoom(room);
            insertRoom(lecture);
            Period period = lecturePeriods.get(lecture);
            lecture.setPeriod(period);
            insertPeriod(lecture);
        }
    }

    @Override
    public void beforeEntityAdded(@NonNull Object o) {

    }

    @Override
    public void afterEntityAdded(@NonNull Object o) {

    }

    @Override
    public void beforeVariableChanged(@NonNull Object o, @NonNull String s) {
        if (o instanceof Lecture && s.equals("period")) {
            retractPeriod((Lecture) o);
        } else if (o instanceof Lecture && s.equals("room")) {
            retractRoom((Lecture) o);
        }
    }

    @Override
    public void afterVariableChanged(@NonNull Object o, @NonNull String s) {
        if (o instanceof Lecture && s.equals("period")) {
            insertPeriod((Lecture) o);
        } else if (o instanceof Lecture && s.equals("room")) {
            insertRoom((Lecture) o);
        }
    }

    @Override
    public void beforeEntityRemoved(@NonNull Object o) {

    }

    @Override
    public void afterEntityRemoved(@NonNull Object o) {

    }

    @Override
    public @NonNull HardSoftScore calculateScore() {
        return HardSoftScore.of(hard, soft);
    }

    private void retractPeriod(Lecture lecture) {
        Period period = lecture.getPeriod();
        if (period == null) return;

        // Conflicting lectures same course in same period
        Course course = lecture.getCourse();
        Map<Period, Integer> conflictSameCourse = conflictingLecturesSameCourse.get(course);
        int oldFreq = conflictSameCourse.get(period);
        if (oldFreq > 1) {
            int matchWeight = 1 + lecture.getCurriculumSet().size();
            hard += nC2Delta(oldFreq) * matchWeight;
        }
        conflictSameCourse.put(period, oldFreq - 1);

        // Conflicting lectures different course in same period
        List<CourseConflict> conflicts = conflictingLessonsDifferentCourse.getOrDefault(course, new ArrayList<>());
        for (CourseConflict conflict : conflicts) {
            Course otherCourse = conflict.getLeftCourse().equals(course) ? conflict.getRightCourse() : conflict.getLeftCourse();
            int numLessons = conflictingLecturesSameCourse.get(otherCourse).getOrDefault(period, 0);
            hard += numLessons * conflict.getConflictCount();
        }

        // Room occupancy
        Room room = lecture.getRoom();
        if (room != null) {
            Map<Period, Integer> roomOccupancyPeriods = roomOccupancy.get(room);
            oldFreq = roomOccupancyPeriods.get(period);
            if (oldFreq > 1) {
                hard += nC2Delta(oldFreq);
            }
            roomOccupancyPeriods.put(period, oldFreq - 1);
        }

        // Unavailable period penalty
        int numUnavailable = unavailablePeriod.get(course).getOrDefault(period, 0);
        if (numUnavailable > 0) {
            hard += 10 * numUnavailable;
        }

        // Minimum working days
        Day day = lecture.getDay();
        Map<Day, Integer> days = minimumWorkingDays.get(course);
        int oldDelta = minimumWorkingDaysDelta.get(course);
        int newFreq = days.get(day) - 1;
        if (newFreq == 0) days.remove(day);
        else days.put(day, newFreq);
        int newNumDays = days.size();
        int newDelta = course.getMinWorkingDaySize() - newNumDays;
        if (newNumDays == 0 || newNumDays >= course.getMinWorkingDaySize()) {
            newDelta = 0;
        }
        if (oldDelta != newDelta) {
            int weight = 5;
            soft += weight * oldDelta;
            soft -= weight * newDelta;
        }
        minimumWorkingDaysDelta.put(course, newDelta);

        // Curriculum compactness
        Set<Curriculum> curriculums = lecture.getCurriculumSet();
        int timeslotIndex = lecture.getTimeslotIndex();
        for (Curriculum curriculum : curriculums) {
            List<Integer> timeslotIndices = curriculumCompactness.get(curriculum).get(day);
            timeslotIndices.set(timeslotIndex, timeslotIndices.get(timeslotIndex) - 1);
            int current = timeslotIndices.get(timeslotIndex);
            int left = timeslotIndex - 1 >= 0 ? timeslotIndices.get(timeslotIndex - 1) : 0;
            int right = timeslotIndex + 1 < timeslotIndices.size() ? timeslotIndices.get(timeslotIndex + 1) : 0;
            if (left == 0 && right == 0) {
                soft += 2;
            } else {
                if (left > 0) {
                    int leftLeft = timeslotIndex - 2 >= 0 ? timeslotIndices.get(timeslotIndex - 2) : 0;
                    if (leftLeft == 0 && current == 0) {
                        soft -= (2 * left); // left wasn't alone before, now it is, penalize score
                    }
                }
                if (right > 0) {
                    int rightRight = timeslotIndex + 2 < timeslotIndices.size() ? timeslotIndices.get(timeslotIndex + 2) : 0;
                    if (rightRight == 0 && current == 0) {
                        soft -= (2 * right); // right wasn't alone before, now it is, penalize score
                    }
                }
            }
        }
    }

    private void retractRoom(Lecture lecture) {
        Course course = lecture.getCourse();
        Room room = lecture.getRoom();
        if (room == null) return;

        // Room occupancy
        Period period = lecture.getPeriod();
        if (period != null) {
            Map<Period, Integer> roomOccupancyPeriods = roomOccupancy.get(room);
            int oldFreq = roomOccupancyPeriods.get(period);
            if (oldFreq > 1) {
                hard += nC2Delta(oldFreq);
            }
            roomOccupancyPeriods.put(period, oldFreq - 1);
        }

        // Room capacity
        if (lecture.getStudentSize() > room.getCapacity()) {
            soft += (lecture.getStudentSize() - room.getCapacity());
        }

        // Room stability
        Map<Room, Integer> rooms = roomStability.get(course);
        int newFreq = rooms.get(room) - 1;
        if (rooms.size() > 1 && newFreq == 0) {
            soft++;
        }
        if (newFreq == 0) rooms.remove(room);
        else rooms.put(room, newFreq);
    }

    private void insertPeriod(Lecture lecture) {
        Period period = lecture.getPeriod();
        if (period == null) return;

        // Conflicting lectures same course in same period
        Course course = lecture.getCourse();
        Map<Period, Integer> conflictSameCourse = conflictingLecturesSameCourse.get(course);
        int newFreq = conflictSameCourse.getOrDefault(period, 0) + 1;
        if (newFreq > 1) {
            int matchWeight = 1 + lecture.getCurriculumSet().size();
            hard -= nC2Delta(newFreq) * matchWeight;
        }
        conflictSameCourse.put(period, newFreq);

        // Conflicting lectures different course in same period
        List<CourseConflict> conflicts = conflictingLessonsDifferentCourse.getOrDefault(course, new ArrayList<>());
        for (CourseConflict conflict : conflicts) {
            Course otherCourse = conflict.getLeftCourse().equals(course) ? conflict.getRightCourse() : conflict.getLeftCourse();
            int numLessons = conflictingLecturesSameCourse.get(otherCourse).getOrDefault(period, 0);
            hard -= numLessons * conflict.getConflictCount();
        }

        // Room occupancy
        Room room = lecture.getRoom();
        if (room != null) {
            Map<Period, Integer> roomOccupancyPeriods = roomOccupancy.get(room);
            newFreq = roomOccupancyPeriods.getOrDefault(period, 0) + 1;
            if (newFreq > 1) {
                hard -= nC2Delta(newFreq);
            }
            roomOccupancyPeriods.put(period, newFreq);
        }

        // Unavailable period penalty
        int numUnavailable = unavailablePeriod.get(course).getOrDefault(period, 0);
        if (numUnavailable > 0) {
            hard -= 10 * numUnavailable;
        }

        // Minimum working days
        Day day = lecture.getDay();
        Map<Day, Integer> days = minimumWorkingDays.get(course);
        days.put(day, days.getOrDefault(day, 0) + 1);
        int numDays = days.size();
        int oldDelta = minimumWorkingDaysDelta.get(course);
        int newDelta = course.getMinWorkingDaySize() - numDays;
        if (numDays >= course.getMinWorkingDaySize()) {
            newDelta = 0;
        }
        if (oldDelta != newDelta) {
            int weight = 5;
            soft += weight * oldDelta;
            soft -= weight * newDelta;
        }
        minimumWorkingDaysDelta.put(course, newDelta);

        // Curriculum compactness
        Set<Curriculum> curriculums = lecture.getCurriculumSet();
        int timeslotIndex = lecture.getTimeslotIndex();
        for (Curriculum curriculum : curriculums) {
            List<Integer> timeslotIndices = curriculumCompactness.get(curriculum).get(day);
            int current = timeslotIndices.get(timeslotIndex);
            int left = timeslotIndex - 1 >= 0 ? timeslotIndices.get(timeslotIndex - 1) : 0;
            int right = timeslotIndex + 1 < timeslotIndices.size() ? timeslotIndices.get(timeslotIndex + 1) : 0;
            if (left == 0 && right == 0) {
                soft -= 2;
            } else {
                if (left > 0) {
                    int leftLeft = timeslotIndex - 2 >= 0 ? timeslotIndices.get(timeslotIndex - 2) : 0;
                    if (leftLeft == 0 && current == 0) {
                        soft += (2 * left); // left was alone before, restore score
                    }
                }
                if (right > 0) {
                    int rightRight = timeslotIndex + 2 < timeslotIndices.size() ? timeslotIndices.get(timeslotIndex + 2) : 0;
                    if (rightRight == 0 && current == 0) {
                        soft += (2 * right); // right was alone before, restore score
                    }
                }
            }
            timeslotIndices.set(timeslotIndex, timeslotIndices.get(timeslotIndex) + 1);
        }
    }

    private void insertRoom(Lecture lecture) {
        Course course = lecture.getCourse();
        Room room = lecture.getRoom();
        if (room == null) return;

        // Room occupancy
        Period period = lecture.getPeriod();
        if (period != null) {
            Map<Period, Integer> roomOccupancyPeriods = roomOccupancy.get(room);
            int newFreq = roomOccupancyPeriods.getOrDefault(period, 0) + 1;
            if (newFreq > 1) {
                hard -= nC2Delta(newFreq);
            }
            roomOccupancyPeriods.put(period, newFreq);
        }

        // Room capacity
        if (lecture.getStudentSize() > room.getCapacity()) {
            soft -= (lecture.getStudentSize() - room.getCapacity());
        }

        // Room stability
        Map<Room, Integer> rooms = roomStability.get(course);
        int newFreq = rooms.getOrDefault(room, 0) + 1;
        rooms.put(room, newFreq);
        if (rooms.size() > 1 && newFreq == 1) {
            soft--;
        }
    }

    // For adding only the pair delta score that current entity causes
    public int nC2Delta(int freq) {
        if (freq <= nc2CacheLimit) return nC2CacheArray[freq] - nC2CacheArray[freq - 1];
        return nCr(freq, 2) - nCr(freq - 1, 2);
    }

    public int nCr(int n, int k) {
        if (k > n) {
            return 0;
        }
        if (k == 0 || k == n) {
            return 1;
        }
        // Ensure k is the smaller value
        k = Math.min(k, n - k);
        int result = 1;
        for (int i = 0; i < k; i++) {
            result *= (n - i);
            result /= (i + 1);
        }
        return result;
    }
}

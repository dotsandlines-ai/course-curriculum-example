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

public class IncrementalScoreCalculator implements ConstraintMatchAwareIncrementalScoreCalculator<CourseSchedule, HardSoftScore> {

    // Score
    private final int nc2CacheLimit = 100;
    private int[][] conflictingLessonsDifferentCourse; // [courseIdx][courseConflictIdx]
    private int[][] conflictingLecturesSameCourse;     // [courseIdx][periodIdx]
    private int[][] roomOccupancy;                     // [roomIdx][periodIdx]
    private int[][] unavailablePeriod;                 // [courseIdx][periodIdx]
    private int[][] minimumWorkingDaysFreq;            // [courseIdx][dayIdx]
    private int[] minimumWorkingDays;                  // [courseIdx]
    private int[] minimumWorkingDaysDelta;             // [courseIdx]
    private int[][][] curriculumCompactness;           // [curriculumIdx][dayIdx][timeslotIdx]
    private int[][] roomStabilityFreq;                 // [courseIdx][roomIdx]
    private int[] roomStabilityNumRooms;               // [courseIdx]
    private CourseConflict[] courseConflicts;          // [courseConflictIdx]
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
        int numCourses = solution.getCourseList().size();
        int numPeriods = solution.getPeriodList().size();
        int numRooms = solution.getRoomList().size();
        int numDays = solution.getDayList().size();
        int numCurriculums = solution.getCurriculumList().size();
        int numTimeslots = solution.getTimeslotList().size();
        conflictingLessonsDifferentCourse = new int[numCourses][];
        conflictingLecturesSameCourse = new int[numCourses][numPeriods];
        roomOccupancy = new int[numRooms][numPeriods];
        unavailablePeriod = new int[numCourses][numPeriods];
        minimumWorkingDaysFreq = new int[numCourses][numDays];
        minimumWorkingDays = new int[numCourses];
        minimumWorkingDaysDelta = new int[numCourses];
        curriculumCompactness = new int[numCurriculums][numDays][numTimeslots];
        roomStabilityFreq = new int[numCourses][numRooms];
        roomStabilityNumRooms = new int[numCourses];
        for (UnavailablePeriodPenalty unavailable : solution.getUnavailablePeriodPenaltyList()) {
            unavailablePeriod[unavailable.getCourse().getIdx()][unavailable.getPeriod().getIdx()]++;
        }
        List<CourseConflict> courseConflictList = solution.calculateCourseConflictList();
        Map<Course, List<CourseConflict>> temporaryMap = new HashMap<>();
        courseConflicts = new CourseConflict[courseConflictList.size()];
        for (int idx = 0; idx < courseConflictList.size(); idx++) {
            CourseConflict conflict = courseConflictList.get(idx);
            courseConflicts[idx] = conflict;
            temporaryMap.computeIfAbsent(conflict.getLeftCourse(), k -> new ArrayList<>()).add(conflict);
            temporaryMap.computeIfAbsent(conflict.getRightCourse(), k -> new ArrayList<>()).add(conflict);
        }
        for (var entry : temporaryMap.entrySet()) {
            Course course = entry.getKey();
            List<CourseConflict> conflicts = entry.getValue();
            conflictingLessonsDifferentCourse[course.getIdx()] = new int[conflicts.size()];
            for (int i = 0; i < conflicts.size(); i++) {
                int conflictIdx = conflicts.get(i).getIdx();
                conflictingLessonsDifferentCourse[course.getIdx()][i] = conflictIdx;
            }
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
        int[] conflictSameCourse = conflictingLecturesSameCourse[course.getIdx()];
        int oldFreq = conflictSameCourse[period.getIdx()]--;
        if (oldFreq > 1) {
            int matchWeight = 1 + lecture.getCurriculumSet().size();
            hard += nC2Delta(oldFreq) * matchWeight;
        }

        // Conflicting lectures different course in same period
        int[] conflictIndices = conflictingLessonsDifferentCourse[course.getIdx()];
        if (conflictIndices != null) {
            for (int conflictIndex : conflictIndices) {
                CourseConflict conflict = courseConflicts[conflictIndex];
                Course otherCourse = conflict.getLeftCourse().equals(course) ? conflict.getRightCourse() : conflict.getLeftCourse();
                int numLessons = conflictingLecturesSameCourse[otherCourse.getIdx()][period.getIdx()];
                hard += numLessons * conflict.getConflictCount();
            }
        }

        // Room occupancy
        Room room = lecture.getRoom();
        if (room != null) {
            oldFreq = roomOccupancy[room.getIdx()][period.getIdx()]--;
            if (oldFreq > 1) {
                hard += nC2Delta(oldFreq);
            }
        }

        // Unavailable period penalty
        int numUnavailable = unavailablePeriod[course.getIdx()][period.getIdx()];
        if (numUnavailable > 0) {
            hard += 10 * numUnavailable;
        }

        // Minimum working days
        Day day = lecture.getDay();
        int newFreq = --minimumWorkingDaysFreq[course.getIdx()][day.getIdx()];
        int newNumDays = newFreq == 0 ? --minimumWorkingDays[course.getIdx()] : minimumWorkingDays[course.getIdx()];
        int oldDelta = minimumWorkingDaysDelta[course.getIdx()];
        int newDelta = course.getMinWorkingDaySize() - newNumDays;
        if (newNumDays == 0 || newNumDays >= course.getMinWorkingDaySize()) {
            newDelta = 0;
        }
        if (oldDelta != newDelta) {
            int weight = 5;
            soft += weight * oldDelta;
            soft -= weight * newDelta;
        }
        minimumWorkingDaysDelta[course.getIdx()] = newDelta;

        // Curriculum compactness
        Set<Curriculum> curriculums = lecture.getCurriculumSet();
        int timeslotIndex = lecture.getTimeslotIndex();
        for (Curriculum curriculum : curriculums) {
            int[] timeslotIndices = curriculumCompactness[curriculum.getIdx()][day.getIdx()];
            int current = --timeslotIndices[timeslotIndex];
            int left = timeslotIndex - 1 >= 0 ? timeslotIndices[timeslotIndex - 1] : 0;
            int right = timeslotIndex + 1 < timeslotIndices.length ? timeslotIndices[timeslotIndex + 1] : 0;
            if (left == 0 && right == 0) {
                soft += 2;
            } else {
                if (left > 0) {
                    int leftLeft = timeslotIndex - 2 >= 0 ? timeslotIndices[timeslotIndex - 2] : 0;
                    if (leftLeft == 0 && current == 0) {
                        soft -= (2 * left); // left wasn't alone before, now it is, penalize score
                    }
                }
                if (right > 0) {
                    int rightRight = timeslotIndex + 2 < timeslotIndices.length ? timeslotIndices[timeslotIndex + 2] : 0;
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
            int oldFreq = roomOccupancy[room.getIdx()][period.getIdx()]--;
            if (oldFreq > 1) {
                hard += nC2Delta(oldFreq);
            }
        }

        // Room capacity
        if (lecture.getStudentSize() > room.getCapacity()) {
            soft += (lecture.getStudentSize() - room.getCapacity());
        }

        // Room stability
        int newFreq = --roomStabilityFreq[course.getIdx()][room.getIdx()];
        int numDistinctRooms = newFreq == 0 ? --roomStabilityNumRooms[course.getIdx()] : roomStabilityNumRooms[course.getIdx()];
        if (numDistinctRooms >= 1 && newFreq == 0) {
            soft++;
        }
    }

    private void insertPeriod(Lecture lecture) {
        Period period = lecture.getPeriod();
        if (period == null) return;

        // Conflicting lectures same course in same period
        Course course = lecture.getCourse();
        int newFreq = ++conflictingLecturesSameCourse[course.getIdx()][period.getIdx()];
        if (newFreq > 1) {
            int matchWeight = 1 + lecture.getCurriculumSet().size();
            hard -= nC2Delta(newFreq) * matchWeight;
        }

        // Conflicting lectures different course in same period
        int[] conflictIndices = conflictingLessonsDifferentCourse[course.getIdx()];
        if (conflictIndices != null) {
            for (int conflictIndex : conflictIndices) {
                CourseConflict conflict = courseConflicts[conflictIndex];
                Course otherCourse = conflict.getLeftCourse().equals(course) ? conflict.getRightCourse() : conflict.getLeftCourse();
                int numLessons = conflictingLecturesSameCourse[otherCourse.getIdx()][period.getIdx()];
                hard -= numLessons * conflict.getConflictCount();
            }
        }

        // Room occupancy
        Room room = lecture.getRoom();
        if (room != null) {
            newFreq = ++roomOccupancy[room.getIdx()][period.getIdx()];
            if (newFreq > 1) {
                hard -= nC2Delta(newFreq);
            }
        }

        // Unavailable period penalty
        int numUnavailable = unavailablePeriod[course.getIdx()][period.getIdx()];
        if (numUnavailable > 0) {
            hard -= 10 * numUnavailable;
        }

        // Minimum working days
        Day day = lecture.getDay();
        newFreq = ++minimumWorkingDaysFreq[course.getIdx()][day.getIdx()];
        int numDays = newFreq == 1 ? ++minimumWorkingDays[course.getIdx()] : minimumWorkingDays[course.getIdx()];
        int oldDelta = minimumWorkingDaysDelta[course.getIdx()];
        int newDelta = course.getMinWorkingDaySize() - numDays;
        if (numDays >= course.getMinWorkingDaySize()) {
            newDelta = 0;
        }
        if (oldDelta != newDelta) {
            int weight = 5;
            soft += weight * oldDelta;
            soft -= weight * newDelta;
        }
        minimumWorkingDaysDelta[course.getIdx()] = newDelta;

        // Curriculum compactness
        Set<Curriculum> curriculums = lecture.getCurriculumSet();
        int timeslotIndex = lecture.getTimeslotIndex();
        for (Curriculum curriculum : curriculums) {
            int[] timeslotIndices = curriculumCompactness[curriculum.getIdx()][day.getIdx()];
            int current = timeslotIndices[timeslotIndex];
            int left = timeslotIndex - 1 >= 0 ? timeslotIndices[timeslotIndex - 1] : 0;
            int right = timeslotIndex + 1 < timeslotIndices.length ? timeslotIndices[timeslotIndex + 1] : 0;
            if (left == 0 && right == 0) {
                soft -= 2;
            } else {
                if (left > 0) {
                    int leftLeft = timeslotIndex - 2 >= 0 ? timeslotIndices[timeslotIndex - 2] : 0;
                    if (leftLeft == 0 && current == 0) {
                        soft += (2 * left); // left was alone before, restore score
                    }
                }
                if (right > 0) {
                    int rightRight = timeslotIndex + 2 < timeslotIndices.length ? timeslotIndices[timeslotIndex + 2] : 0;
                    if (rightRight == 0 && current == 0) {
                        soft += (2 * right); // right was alone before, restore score
                    }
                }
            }
            timeslotIndices[timeslotIndex]++;
        }
    }

    private void insertRoom(Lecture lecture) {
        Course course = lecture.getCourse();
        Room room = lecture.getRoom();
        if (room == null) return;

        // Room occupancy
        Period period = lecture.getPeriod();
        if (period != null) {
            int newFreq = ++roomOccupancy[room.getIdx()][period.getIdx()];
            if (newFreq > 1) {
                hard -= nC2Delta(newFreq);
            }
        }

        // Room capacity
        if (lecture.getStudentSize() > room.getCapacity()) {
            soft -= (lecture.getStudentSize() - room.getCapacity());
        }

        // Room stability
        int newFreq = ++roomStabilityFreq[course.getIdx()][room.getIdx()];
        int numDistinctRooms = newFreq == 1 ? ++roomStabilityNumRooms[course.getIdx()] : roomStabilityNumRooms[course.getIdx()];
        if (numDistinctRooms > 1 && newFreq == 1) {
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

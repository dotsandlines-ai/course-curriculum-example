package ai.timefold.solver.benchmarks.examples.curriculumcourse.domain;

import ai.timefold.solver.benchmarks.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.benchmarks.examples.curriculumcourse.domain.solver.CourseConflict;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.ArrayList;
import java.util.List;

@PlanningSolution
public class CourseSchedule extends AbstractPersistable {

    private String name;

    private List<Teacher> teacherList;
    private List<Curriculum> curriculumList;
    private List<Course> courseList;
    private List<Day> dayList;
    private List<Timeslot> timeslotList;
    private List<Period> periodList;
    private List<Room> roomList;

    private List<UnavailablePeriodPenalty> unavailablePeriodPenaltyList;

    private List<Lecture> lectureList;

    private HardSoftScore score;

    public CourseSchedule() {
    }

    public CourseSchedule(long id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ProblemFactCollectionProperty
    public List<Teacher> getTeacherList() {
        return teacherList;
    }

    public void setTeacherList(List<Teacher> teacherList) {
        this.teacherList = teacherList;
    }

    @ProblemFactCollectionProperty
    public List<Curriculum> getCurriculumList() {
        return curriculumList;
    }

    public void setCurriculumList(List<Curriculum> curriculumList) {
        this.curriculumList = curriculumList;
        for (int idx = 0; idx < curriculumList.size(); idx++) {
            curriculumList.get(idx).setIdx(idx);
        }
    }

    @ProblemFactCollectionProperty
    public List<Course> getCourseList() {
        return courseList;
    }

    public void setCourseList(List<Course> courseList) {
        this.courseList = courseList;
        for (int idx = 0; idx < courseList.size(); idx++) {
            this.courseList.get(idx).setIdx(idx);
        }
    }

    @ProblemFactCollectionProperty
    public List<Day> getDayList() {
        return dayList;
    }

    public void setDayList(List<Day> dayList) {
        this.dayList = dayList;
        for (int idx = 0; idx < dayList.size(); idx++) {
            dayList.get(idx).setIdx(idx);
        }
    }

    @ProblemFactCollectionProperty
    public List<Timeslot> getTimeslotList() {
        return timeslotList;
    }

    public void setTimeslotList(List<Timeslot> timeslotList) {
        this.timeslotList = timeslotList;
        for (int idx = 0; idx < timeslotList.size(); idx++) {
            timeslotList.get(idx).setIdx(idx);
        }
    }

    @ValueRangeProvider
    @ProblemFactCollectionProperty
    public List<Period> getPeriodList() {
        return periodList;
    }

    public void setPeriodList(List<Period> periodList) {
        this.periodList = periodList;
        for (int idx = 0; idx < periodList.size(); idx++) {
            periodList.get(idx).setIdx(idx);
        }
    }

    @ValueRangeProvider
    @ProblemFactCollectionProperty
    public List<Room> getRoomList() {
        return roomList;
    }

    public void setRoomList(List<Room> roomList) {
        this.roomList = roomList;
        for (int idx = 0; idx < roomList.size(); idx++) {
            roomList.get(idx).setIdx(idx);
        }
    }

    @ProblemFactCollectionProperty
    public List<UnavailablePeriodPenalty> getUnavailablePeriodPenaltyList() {
        return unavailablePeriodPenaltyList;
    }

    public void setUnavailablePeriodPenaltyList(List<UnavailablePeriodPenalty> unavailablePeriodPenaltyList) {
        this.unavailablePeriodPenaltyList = unavailablePeriodPenaltyList;
    }

    @PlanningEntityCollectionProperty
    public List<Lecture> getLectureList() {
        return lectureList;
    }

    public void setLectureList(List<Lecture> lectureList) {
        this.lectureList = lectureList;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @ProblemFactCollectionProperty
    public List<CourseConflict>
    calculateCourseConflictList() {
        List<CourseConflict> courseConflictList =
                new ArrayList<>();
        int courseConflictIdx = 0;
        for (Course leftCourse : courseList) {
            for (Course rightCourse : courseList) {
                if (leftCourse.getId() < rightCourse.getId()) {
                    int conflictCount = 0;
                    if (leftCourse.getTeacher().equals(rightCourse.getTeacher())) {
                        conflictCount++;
                    }
                    for (Curriculum curriculum : leftCourse.getCurriculumSet()) {
                        if (rightCourse.getCurriculumSet().contains(curriculum)) {
                            conflictCount++;
                        }
                    }
                    if (conflictCount > 0) {
                        CourseConflict conflict = new CourseConflict(leftCourse, rightCourse, conflictCount);
                        conflict.setIdx(courseConflictIdx++);
                        courseConflictList.add(conflict);
                    }
                }
            }
        }
        return courseConflictList;
    }
}

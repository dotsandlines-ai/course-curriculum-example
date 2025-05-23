package ai.timefold.solver.benchmarks.examples.curriculumcourse.domain;

import ai.timefold.solver.benchmarks.examples.common.domain.AbstractPersistable;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@JsonIdentityInfo(scope = Course.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Course extends AbstractPersistable {

    private String code;

    private Teacher teacher;
    private int lectureSize;
    private int minWorkingDaySize;

    private Set<Curriculum> curriculumSet;
    private int studentSize;
    private int idx; // for incremental score calculator only

    public Course() {
    }

    public Course(int id, String code, Teacher teacher, int lectureSize, int studentSize, int minWorkingDaySize,
                  Curriculum... curricula) {
        super(id);
        this.code = requireNonNull(code);
        this.teacher = requireNonNull(teacher);
        this.lectureSize = lectureSize;
        this.minWorkingDaySize = minWorkingDaySize;
        this.curriculumSet = Arrays.stream(curricula).collect(Collectors.toCollection(LinkedHashSet::new));
        this.studentSize = studentSize;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public int getLectureSize() {
        return lectureSize;
    }

    public void setLectureSize(int lectureSize) {
        this.lectureSize = lectureSize;
    }

    public int getMinWorkingDaySize() {
        return minWorkingDaySize;
    }

    public void setMinWorkingDaySize(int minWorkingDaySize) {
        this.minWorkingDaySize = minWorkingDaySize;
    }

    public Set<Curriculum> getCurriculumSet() {
        return curriculumSet;
    }

    public void setCurriculumSet(Set<Curriculum> curriculumSet) {
        this.curriculumSet = curriculumSet;
    }

    public int getStudentSize() {
        return studentSize;
    }

    public void setStudentSize(int studentSize) {
        this.studentSize = studentSize;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    @Override
    public String toString() {
        return code;
    }

}

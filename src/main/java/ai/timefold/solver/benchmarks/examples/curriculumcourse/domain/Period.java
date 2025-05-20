package ai.timefold.solver.benchmarks.examples.curriculumcourse.domain;

import ai.timefold.solver.benchmarks.examples.common.domain.AbstractPersistable;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import static java.util.Objects.requireNonNull;

@JsonIdentityInfo(scope = Period.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Period extends AbstractPersistable {

    private Day day;
    private Timeslot timeslot;
    private int idx; // for incremental score calculator only

    public Period() {
    }

    public Period(long id, Day day, Timeslot timeslot) {
        super(id);
        this.day = requireNonNull(day);
        day.getPeriodList().add(this);
        this.timeslot = requireNonNull(timeslot);
    }

    public Day getDay() {
        return day;
    }

    public void setDay(Day day) {
        this.day = day;
    }

    public Timeslot getTimeslot() {
        return timeslot;
    }

    public void setTimeslot(Timeslot timeslot) {
        this.timeslot = timeslot;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    @Override
    public String toString() {
        return day + "-" + timeslot;
    }

}

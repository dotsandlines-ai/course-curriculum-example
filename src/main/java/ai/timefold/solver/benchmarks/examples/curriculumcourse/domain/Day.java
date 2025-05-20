package ai.timefold.solver.benchmarks.examples.curriculumcourse.domain;

import ai.timefold.solver.benchmarks.examples.common.domain.AbstractPersistable;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@JsonIdentityInfo(scope = Day.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Day extends AbstractPersistable {

    private static final String[] WEEKDAYS = {"Mo", "Tu", "We", "Th", "Fr", "Sat", "Sun"};

    private int dayIndex;

    private List<Period> periodList;

    private int idx; // for incremental score calculator only

    public Day() {
    }

    public Day(int dayIndex, Period... periods) {
        super(dayIndex);
        this.dayIndex = dayIndex;
        this.periodList = Arrays.stream(periods)
                .collect(Collectors.toList());
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(int dayIndex) {
        this.dayIndex = dayIndex;
    }

    public List<Period> getPeriodList() {
        return periodList;
    }

    public void setPeriodList(List<Period> periodList) {
        this.periodList = periodList;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    @Override
    public String toString() {
        return Integer.toString(dayIndex);
    }

}

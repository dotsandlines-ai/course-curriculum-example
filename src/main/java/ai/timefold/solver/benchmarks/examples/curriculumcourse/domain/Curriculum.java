package ai.timefold.solver.benchmarks.examples.curriculumcourse.domain;

import ai.timefold.solver.benchmarks.examples.common.domain.AbstractPersistable;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import static java.util.Objects.requireNonNull;

@JsonIdentityInfo(scope = Curriculum.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Curriculum extends AbstractPersistable {

    private String code;
    private int idx; // for incremental score calculator only

    public Curriculum() {
    }

    public Curriculum(int id, String code) {
        super(id);
        this.code = requireNonNull(code);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

package ai.timefold.solver.benchmarks.examples.curriculumcourse.domain;

import ai.timefold.solver.benchmarks.examples.common.domain.AbstractPersistable;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import static java.util.Objects.requireNonNull;

@JsonIdentityInfo(scope = Teacher.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Teacher extends AbstractPersistable {

    private String code;

    public Teacher() {
    }

    public Teacher(int id, String code) {
        super(id);
        this.code = requireNonNull(code);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }

}

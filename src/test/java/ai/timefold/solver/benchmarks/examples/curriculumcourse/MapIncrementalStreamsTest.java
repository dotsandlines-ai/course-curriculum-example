package ai.timefold.solver.benchmarks.examples.curriculumcourse;

import ai.timefold.solver.benchmarks.examples.curriculumcourse.domain.CourseSchedule;
import ai.timefold.solver.benchmarks.examples.curriculumcourse.persistence.CurriculumCourseSolutionFileIO;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.fail;

public class MapIncrementalStreamsTest {
    private static final Logger logger = LoggerFactory.getLogger(MapIncrementalStreamsTest.class);
    public static final String SOLVER_CONFIG =
            "ai/timefold/solver/benchmarks/examples/curriculumcourse/curriculumTestMapIncrementalSolverConfig.xml";
    public static final String DATA_DIR_NAME = "curriculumcourse";

    @Test
    public void allDatasetsTest() {
        File directory = new File("data/curriculumcourse/unsolved");
        if(!directory.exists()) {
            fail("Directory: " + directory.getPath() + " doesn't exist.");
        }
        File[] files = directory.listFiles();
        if(files == null) {
            fail("Directory: " + directory.getPath() + " has no files.");
        }
        int fileCnt = 1;
        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".json")) continue;
            String datasetName = file.getName();
            logger.info("\nTesting: {} (File {}/{})", datasetName, fileCnt++, files.length);
            runSolverTest(datasetName);
        }
    }

    private static CourseSchedule runSolverTest(String datasetName) {
        var solutionFileIo = new CurriculumCourseSolutionFileIO();
        var solution = solutionFileIo.read(Path.of("data", DATA_DIR_NAME, "unsolved", datasetName).toFile().getAbsoluteFile());
        logger.info("Input problem successfully loaded. ({}). ", datasetName);
        var solverFactory = SolverFactory.<CourseSchedule> createFromXmlResource(SOLVER_CONFIG);
        var solver = solverFactory.buildSolver(new SolverConfigOverride<CourseSchedule>()
                .withTerminationConfig(new TerminationConfig()
                        .withMinutesSpentLimit(5L)));
        logger.info(String.format("Solver configured with %s, starting solving.", SOLVER_CONFIG));
        return solver.solve(solution);
    }
}


package ai.timefold.solver.benchmarks.examples.common.persistence;

import ai.timefold.solver.benchmarks.examples.common.app.CommonApp;
import ai.timefold.solver.benchmarks.examples.common.app.LoggingMain;
import ai.timefold.solver.benchmarks.examples.common.business.ProblemFileComparator;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

import java.io.File;
import java.util.Arrays;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SolutionConverter<Solution_> extends LoggingMain {

    public static <Solution_> SolutionConverter<Solution_> createImportConverter(String dataDirName,
            AbstractSolutionImporter<Solution_> importer, SolutionFileIO<Solution_> outputSolutionFileIO) {
        SolutionFileIO<Solution_> inputSolutionFileIO = new SolutionFileIO<>() {
            @Override
            public String getInputFileExtension() {
                return importer.getInputFileSuffix();
            }

            @Override
            public Solution_ read(File inputSolutionFile) {
                return importer.readSolution(inputSolutionFile);
            }

            @Override
            public void write(Solution_ solution_, File outputSolutionFile) {
                throw new UnsupportedOperationException();
            }
        };
        return new SolutionConverter<>(CommonApp.determineDataDir(dataDirName).getName(), inputSolutionFileIO, "import",
                outputSolutionFileIO, "unsolved");
    }

    protected SolutionFileIO<Solution_> inputSolutionFileIO;
    protected final File inputDir;
    protected SolutionFileIO<Solution_> outputSolutionFileIO;
    protected final File outputDir;

    private SolutionConverter(String dataDirName,
            SolutionFileIO<Solution_> inputSolutionFileIO, String inputDirName,
            SolutionFileIO<Solution_> outputSolutionFileIO, String outputDirName) {
        this.inputSolutionFileIO = inputSolutionFileIO;
        this.outputSolutionFileIO = outputSolutionFileIO;
        File dataDir = CommonApp.determineDataDir(dataDirName);
        inputDir = new File(dataDir, inputDirName);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new IllegalStateException("The directory inputDir (" + inputDir.getAbsolutePath()
                    + ") does not exist or is not a directory.");
        }
        outputDir = new File(dataDir, outputDirName);
    }

    public void convertAll() {
        File[] inputFiles = inputDir.listFiles();
        if (inputFiles == null) {
            throw new IllegalStateException("Unable to list the files in the inputDirectory ("
                    + inputDir.getAbsolutePath() + ").");
        }
        Arrays.sort(inputFiles, new ProblemFileComparator());
        Arrays.stream(inputFiles)
                .parallel()
                .filter(this::acceptInputFile)
                .forEach(this::convert);
    }

    public boolean acceptInputFile(File inputFile) {
        return inputFile.getName().endsWith("." + inputSolutionFileIO.getInputFileExtension());
    }

    public void convert(String inputFileName) {
        String outputFileName = inputFileName.substring(0,
                inputFileName.length() - inputSolutionFileIO.getInputFileExtension().length())
                + outputSolutionFileIO.getOutputFileExtension();
        convert(inputFileName, outputFileName);
    }

    public void convert(String inputFileName, String outputFileName) {
        File inputFile = new File(inputDir, inputFileName);
        if (!inputFile.exists()) {
            throw new IllegalStateException("The file inputFile (" + inputFile.getAbsolutePath()
                    + ") does not exist.");
        }
        File outputFile = new File(outputDir, outputFileName);
        outputFile.getParentFile().mkdirs();
        convert(inputFile, outputFile);
    }

    public void convert(File inputFile) {
        String inputFileName = inputFile.getName();
        String outputFileName = inputFileName.substring(0,
                inputFileName.length() - inputSolutionFileIO.getInputFileExtension().length())
                + outputSolutionFileIO.getOutputFileExtension();
        File outputFile = new File(outputDir, outputFileName);
        convert(inputFile, outputFile);
    }

    protected void convert(File inputFile, File outputFile) {
        Solution_ solution = inputSolutionFileIO.read(inputFile);
        outputSolutionFileIO.write(solution, outputFile);
        logger.info("Saved: {}", outputFile);
    }

}

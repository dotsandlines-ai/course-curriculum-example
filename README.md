# Course curriculum - 3 score calculators

1. [Constraint streams][1]  
2. [Incremental score calculator with maps][2]  
3. [Incremental score calculator with arrays][3]  

Constraint streams are an exact copy from the Timefold examples, and the incrementals we have implemented ourselves.

### Run the app

You can run the app via [CurriculumCourseApp.java][4], incremental with arrays is the default score calculator.

### Testing

You can run the ```assertionScoreDirector``` to test both incrementals against constraint streams.

- Test the map incremental via: [MapIncrementalStreamsTest.java][5]  
- Test the array incremental via: [ArrayIncrementalStreamsTest.java][6]

These tests will perform a 5 minute ```assertionScoreDirector``` run on every dataset, which is about 1.5 hours of computation. You can
increase the 5 minute limit if you want. We ran these tests and they passed on all datasets.

### Benchmarks

| Dataset | Streams | Incremental maps | Incremental arrays |
|---------|---------|------------------|---------------------|
| comp01  | 68.957 (1.0X) | 268.511 (3.9X)   | **421.814 (6.1X)**  |
| comp02  | 38.887 (1.0X) | 205.743 (5.3X)   | **370.694 (9.5X)**  |
| comp05  | 21.912 (1.0X) | 143.531 (6.6X)   | **264.653 (12.1X)** |
| comp07  | 40.765 (1.0X) | 217.882 (5.3X)   | **388.035 (9.5X)**  |
| comp10  | 42.286 (1.0X) | 221.029 (5.2X)   | **390.036 (9.2X)**  |
| comp11  | 68.043 (1.0X) | 252.742 (3.7X)   | **422.128 (6.2X)**  |
| comp13  | 48.492 (1.0X) | 231.647 (4.8X)   | **400.617 (8.3X)**  |
| comp14  | 43.729 (1.0X) | 216.760 (5.0X)   | **381.908 (8.7X)**  |

_Table 1 - Move evaluation speed per dataset per score calculator_

Incremental arrays clearly won over streams and incremental maps on every dataset. Keep in mind that this example problem has only
8 constraints—in practice, you're likely to see more. In that case, incremental arrays are very likely to perform even better compared
to the alternatives. Since these score calculators are equivalent functions, if you're able to consistently maintain the incremental arrays,
you can just use that.

---

Timefold course curriculum example we cloned can be found [here][7].

[1]: https://github.com/dotsandlines-ai/course-curriculum-example/blob/main/src/main/java/ai/timefold/solver/benchmarks/examples/curriculumcourse/score/CurriculumCourseConstraintProvider.java
[2]: https://github.com/dotsandlines-ai/course-curriculum-example/blob/main/src/main/java/ai/timefold/solver/benchmarks/examples/curriculumcourse/score/MapIncrementalScoreCalculator.java
[3]: https://github.com/dotsandlines-ai/course-curriculum-example/blob/main/src/main/java/ai/timefold/solver/benchmarks/examples/curriculumcourse/score/IncrementalScoreCalculator.java
[4]: https://github.com/dotsandlines-ai/course-curriculum-example/blob/5716e31547bcb30a076e2614b4ab10d79d0347af/src/main/java/ai/timefold/solver/benchmarks/examples/curriculumcourse/app/CurriculumCourseApp.java
[5]: https://github.com/dotsandlines-ai/course-curriculum-example/blob/main/src/test/java/ai/timefold/solver/benchmarks/examples/curriculumcourse/MapIncrementalStreamsTest.java
[6]: https://github.com/dotsandlines-ai/course-curriculum-example/blob/main/src/test/java/ai/timefold/solver/benchmarks/examples/curriculumcourse/ArrayIncrementalStreamsTest.java
[7]: https://github.com/TimefoldAI/timefold-solver-benchmarks/tree/main/src/main/java/ai/timefold/solver/benchmarks/examples/curriculumcourse

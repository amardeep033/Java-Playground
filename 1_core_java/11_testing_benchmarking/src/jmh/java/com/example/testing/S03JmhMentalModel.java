package com.example.testing;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;


// JMH mental model:
// 1. Unit tests answer "is this correct?"
// 2. Benchmarks answer "how fast is this under a controlled harness?"
// 3. JMH is preferred because Java has warm-up, JIT compilation, dead-code elimination, and measurement noise.
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 1)
@Fork(1)
@State(Scope.Benchmark)
public class S03JmhMentalModel {

    // 4. @Benchmark marks the method JMH should measure.
    // Returning a value helps prevent the JVM from treating the work as useless.
    @Benchmark
    public String stringBuilderDemo() {
        StringBuilder builder = new StringBuilder();
        builder.append("AAAA");
        builder.append("BBBB");
        return builder.toString();
    }

    // 5. This is intentionally small. The goal here is the mental model, not a serious performance claim.
    @Benchmark
    public String plusOperatorDemo() {
        return "AAAA" + "BBBB";
    }
}

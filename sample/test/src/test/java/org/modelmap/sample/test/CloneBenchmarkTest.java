package org.modelmap.sample.test;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.modelmap.sample.model.CloneBenchmark;

public class CloneBenchmarkTest {
    @Rule
    public TestRule chrono = new TestRule() {
        @Override
        public Statement apply(Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    final long startTime = System.nanoTime();
                    base.evaluate();
                    final long elapsedTime = System.nanoTime() - startTime;
                    System.out.println(description.getMethodName() + " " + elapsedTime / 1000 + " micros");
                }
            };
        }

    };

    private CloneBenchmark clone = new CloneBenchmark();

    @Before
    public void before() {
        clone.init();
    }

    @Test
    public void clone_java_bean() {
        clone.clone_java_bean();
    }

    @Test
    public void clone_field_model() {
        clone.clone_field_model();
    }

    @Test
    public void clone_stream_sequential() {
        clone.clone_stream_sequential();
    }

    @Test
    public void clone_stream_parallel() {
        clone.clone_stream_parallel();
    }
}
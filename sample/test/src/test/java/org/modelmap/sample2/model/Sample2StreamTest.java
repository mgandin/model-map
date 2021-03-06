package org.modelmap.sample2.model;

import static org.modelmap.util.LoopingRule.doSomeStuff;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.modelmap.core.FieldModels;
import org.modelmap.util.LoopingRule;

public class Sample2StreamTest {

    @Rule
    public final TestRule looping = new LoopingRule(1);

    @Test
    public void stream_parallel_apply_count() {
        new Sample2ModelWrapper(Sample2Models.sample()).parallelStream().map(e -> {
            doSomeStuff();
            return e;
        }).count();
    }

    @Test
    public void stream_sequential_apply_count() {
        new Sample2ModelWrapper(Sample2Models.sample()).stream().map(e -> {
            doSomeStuff();
            return e;
        }).count();
    }

    @Test
    public void stream_parallel_apply_wait_collect_map() {
        new Sample2ModelWrapper(Sample2Models.sample()).parallelStream().map(e -> {
            doSomeStuff();
            return e;
        }).collect(FieldModels.toConcurrentFieldModel(new Sample2ModelWrapper()));
    }

    @Test
    public void stream_parallel_apply_wait_collect_concurrent() {
        new Sample2ModelWrapper(Sample2Models.sample()).parallelStream().map(e -> {
            doSomeStuff();
            return e;
        }).collect(FieldModels.toConcurrentFieldModel(new Sample2ModelWrapper()));
    }

    @Test
    public void stream_sequential_apply_wait_collect() {
        new Sample2ModelWrapper(Sample2Models.sample()).stream().map(e -> {
            doSomeStuff();
            return e;
        }).collect(FieldModels.toFieldModel(new Sample2ModelWrapper()));
    }
}

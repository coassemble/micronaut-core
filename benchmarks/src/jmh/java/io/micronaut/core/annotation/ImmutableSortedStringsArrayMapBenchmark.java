/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.core.annotation;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class ImmutableSortedStringsArrayMapBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(ImmutableSortedStringsArrayMapBenchmark.class.getName() + ".*")
            .warmupIterations(3)
            .measurementIterations(5)
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.NANOSECONDS)
            .forks(1)
            .build();

        new Runner(opt).run();
    }

    @Benchmark
    public Object getOne(ThisState s) {
        return s.map.get("foo");
    }

    @Benchmark
    public Object containsKey(ThisState s) {
        return s.map.containsKey("foo");
    }

    @State(Scope.Thread)
    public static class ThisState {
        @Param({"ISSAM", "MAP_OF"})
        ImmutableSortedStringsArrayMapBenchmark.Type type;
        @Param({"0", "1", "2", "3", "4", "5", "10"})
        int load;
        private Map<String, Object> map;

        @Setup
        public void setUp() {
            Map<String, Object> def = new TreeMap<>();
            for (int i = 0; i < load; i++) {
                def.put("f" + i, "b" + i);
            }
            map = switch (type) {
                case ISSAM ->
                    new ImmutableSortedStringsArrayMap<>(def.keySet().toArray(new String[0]), def.values().toArray());
                case MAP_OF -> asMapOf(def);
            };
        }
    }

    private static Map<String, Object> asMapOf(Map<String, Object> map) {
        return switch (map.size()) {
            case 0 -> Map.of();
            case 1 -> {
                Map.Entry<String, Object> e = map.entrySet().iterator().next();
                yield Map.of(e.getKey(), e.getValue());
            }
            case 2 -> {
                ArrayList<Map.Entry<String, Object>> entries = new ArrayList<>(map.entrySet());
                Map.Entry<String, Object> e1 = entries.get(0);
                Map.Entry<String, Object> e2 = entries.get(1);
                yield Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue());
            }
            case 3 -> {
                ArrayList<Map.Entry<String, Object>> entries = new ArrayList<>(map.entrySet());
                Map.Entry<String, Object> e1 = entries.get(0);
                Map.Entry<String, Object> e2 = entries.get(1);
                Map.Entry<String, Object> e3 = entries.get(2);
                yield Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(), e3.getValue());
            }
            case 4 -> {
                ArrayList<Map.Entry<String, Object>> entries = new ArrayList<>(map.entrySet());
                Map.Entry<String, Object> e1 = entries.get(0);
                Map.Entry<String, Object> e2 = entries.get(1);
                Map.Entry<String, Object> e3 = entries.get(2);
                Map.Entry<String, Object> e4 = entries.get(3);
                yield Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(), e3.getValue(), e4.getKey(), e4.getValue());
            }
            case 5 -> {
                ArrayList<Map.Entry<String, Object>> entries = new ArrayList<>(map.entrySet());
                Map.Entry<String, Object> e1 = entries.get(0);
                Map.Entry<String, Object> e2 = entries.get(1);
                Map.Entry<String, Object> e3 = entries.get(2);
                Map.Entry<String, Object> e4 = entries.get(3);
                Map.Entry<String, Object> e5 = entries.get(4);
                yield Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(), e3.getValue(), e4.getKey(), e4.getValue(), e5.getKey(), e5.getValue());
            }
            default -> map;
        };
    }

    public enum Type {
        ISSAM,
        MAP_OF,
    }
}

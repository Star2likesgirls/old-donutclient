package org.reflections.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.Collector.Characteristics;
import org.reflections.ReflectionUtils;

public class AnnotationMergeCollector implements Collector<Annotation, Map<String, Object>, Map<String, Object>> {
   private final AnnotatedElement annotatedElement;
   private final BiFunction<Object, Object, Object> mergeFunction;

   public AnnotationMergeCollector(AnnotatedElement annotatedElement, BiFunction<Object, Object, Object> mergeFunction) {
      this.annotatedElement = annotatedElement;
      this.mergeFunction = mergeFunction;
   }

   public AnnotationMergeCollector() {
      this((AnnotatedElement)null);
   }

   public AnnotationMergeCollector(AnnotatedElement annotatedElement) {
      this(annotatedElement, AnnotationMergeCollector::concatValues);
   }

   public Supplier<Map<String, Object>> supplier() {
      return HashMap::new;
   }

   public BiConsumer<Map<String, Object>, Annotation> accumulator() {
      return (acc, ann) -> {
         this.mergeMaps(acc, ReflectionUtils.toMap(ann, this.annotatedElement));
      };
   }

   public BinaryOperator<Map<String, Object>> combiner() {
      return this::mergeMaps;
   }

   public Function<Map<String, Object>, Map<String, Object>> finisher() {
      return Function.identity();
   }

   public Set<Characteristics> characteristics() {
      return Collections.emptySet();
   }

   private Map<String, Object> mergeMaps(Map<String, Object> m1, Map<String, Object> m2) {
      m2.forEach((k1, v1) -> {
         m1.merge(k1, v1, this.mergeFunction);
      });
      return m1;
   }

   private static Object concatValues(Object v1, Object v2) {
      if (v1.getClass().isArray()) {
         return v2.getClass().getComponentType().equals(String.class) ? stringArrayConcat((String[])((String[])v1), (String[])((String[])v2)) : arrayAdd((Object[])((Object[])v1), (Object[])((Object[])v2));
      } else {
         return v2.getClass().equals(String.class) ? stringConcat((String)v1, (String)v2) : v2;
      }
   }

   private static Object[] arrayAdd(Object[] o1, Object[] o2) {
      return o2.length == 0 ? o1 : (o1.length == 0 ? o2 : Stream.concat(Stream.of(o1), Stream.of(o2)).toArray((x$0) -> {
         return new Object[x$0];
      }));
   }

   private static Object stringArrayConcat(String[] v1, String[] v2) {
      return v2.length == 0 ? v1 : (v1.length == 0 ? v2 : Arrays.stream(v2).flatMap((s2) -> {
         return Arrays.stream(v1).map((s1) -> {
            return s2 + s1;
         });
      }).toArray((x$0) -> {
         return new String[x$0];
      }));
   }

   private static Object stringConcat(String v1, String v2) {
      return v2.isEmpty() ? v1 : (v1.isEmpty() ? v2 : v1 + v2);
   }
}

package org.reflections.util;

import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.reflections.ReflectionUtils;
import org.reflections.Store;

public interface UtilQueryBuilder<F, E> {
   QueryFunction<Store, E> get(F var1);

   default QueryFunction<Store, E> of(F element) {
      return this.of(ReflectionUtils.extendType().get((AnnotatedElement)element));
   }

   default QueryFunction<Store, E> of(F element, Predicate<? super E> predicate) {
      return this.of(element).filter(predicate);
   }

   default <T> QueryFunction<Store, E> of(QueryFunction<Store, T> function) {
      return (store) -> {
         return (LinkedHashSet)function.apply(store).stream().flatMap((t) -> {
            return this.get(t).apply(store).stream();
         }).collect(Collectors.toCollection(LinkedHashSet::new));
      };
   }
}

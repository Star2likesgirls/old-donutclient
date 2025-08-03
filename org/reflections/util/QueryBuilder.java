package org.reflections.util;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.reflections.Store;

public interface QueryBuilder extends NameHelper {
   default String index() {
      return this.getClass().getSimpleName();
   }

   default QueryFunction<Store, String> get(String key) {
      return (store) -> {
         return new LinkedHashSet((Collection)((Map)store.getOrDefault(this.index(), Collections.emptyMap())).getOrDefault(key, Collections.emptySet()));
      };
   }

   default QueryFunction<Store, String> get(AnnotatedElement element) {
      return this.get(this.toName(element));
   }

   default QueryFunction<Store, String> get(Collection<String> keys) {
      return (QueryFunction)keys.stream().map(this::get).reduce(QueryFunction::add).get();
   }

   default QueryFunction<Store, String> getAll(Collection<String> keys) {
      return QueryFunction.set(keys).getAll(this::get);
   }

   default QueryFunction<Store, String> getAllIncluding(String key) {
      return QueryFunction.single(key).add(QueryFunction.single(key).getAll(this::get));
   }

   default QueryFunction<Store, String> getAllIncluding(Collection<String> keys) {
      return QueryFunction.set(keys).add(QueryFunction.set(keys).getAll(this::get));
   }

   default QueryFunction<Store, String> of(Collection<String> keys) {
      return this.getAll(keys);
   }

   default QueryFunction<Store, String> of(String key) {
      return this.getAll(Collections.singletonList(key));
   }

   default QueryFunction<Store, String> of(AnnotatedElement... elements) {
      return this.getAll(this.toNames(elements));
   }

   default QueryFunction<Store, String> of(Set<? extends AnnotatedElement> elements) {
      return this.getAll(this.toNames(elements));
   }

   default QueryFunction<Store, String> with(Collection<String> keys) {
      return this.of(keys);
   }

   default QueryFunction<Store, String> with(String key) {
      return this.of(key);
   }

   default QueryFunction<Store, String> with(AnnotatedElement... keys) {
      return this.of(keys);
   }

   default QueryFunction<Store, String> with(Set<? extends AnnotatedElement> keys) {
      return this.of(keys);
   }

   default <T> QueryFunction<Store, T> of(QueryFunction queryFunction) {
      return queryFunction.add(queryFunction.getAll(this::get));
   }
}

package org.reflections.util;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.reflections.Store;

public interface QueryFunction<C, T> extends Function<C, Set<T>>, NameHelper {
   Set<T> apply(C var1);

   static <C, T> QueryFunction<Store, T> empty() {
      return (ctx) -> {
         return Collections.emptySet();
      };
   }

   static <C, T> QueryFunction<Store, T> single(T element) {
      return (ctx) -> {
         return Collections.singleton(element);
      };
   }

   static <C, T> QueryFunction<Store, T> set(Collection<T> elements) {
      return (ctx) -> {
         return new LinkedHashSet(elements);
      };
   }

   default QueryFunction<C, T> filter(Predicate<? super T> predicate) {
      return (ctx) -> {
         return (LinkedHashSet)this.apply(ctx).stream().filter(predicate).collect(Collectors.toCollection(LinkedHashSet::new));
      };
   }

   default <R> QueryFunction<C, R> map(Function<? super T, ? extends R> function) {
      return (ctx) -> {
         return (LinkedHashSet)this.apply(ctx).stream().map(function).collect(Collectors.toCollection(LinkedHashSet::new));
      };
   }

   default <R> QueryFunction<C, R> flatMap(Function<T, ? extends Function<C, Set<R>>> function) {
      return (ctx) -> {
         return (LinkedHashSet)this.apply(ctx).stream().flatMap((t) -> {
            return ((Set)((Function)function.apply(t)).apply(ctx)).stream();
         }).collect(Collectors.toCollection(LinkedHashSet::new));
      };
   }

   default QueryFunction<C, T> getAll(Function<T, QueryFunction<C, T>> builder) {
      return this.getAll(builder, (t) -> {
         return t;
      });
   }

   default <R> QueryFunction<C, R> getAll(Function<T, QueryFunction<C, R>> builder, Function<R, T> traverse) {
      return (ctx) -> {
         List<T> workKeys = new ArrayList(this.apply(ctx));
         Set<R> result = new LinkedHashSet();

         for(int i = 0; i < workKeys.size(); ++i) {
            T key = workKeys.get(i);
            Set<R> apply = ((QueryFunction)builder.apply(key)).apply(ctx);
            Iterator var9 = apply.iterator();

            while(var9.hasNext()) {
               R r = var9.next();
               if (result.add(r)) {
                  workKeys.add(traverse.apply(r));
               }
            }
         }

         return result;
      };
   }

   default <R> QueryFunction<C, T> add(QueryFunction<C, T> function) {
      return (ctx) -> {
         return (LinkedHashSet)Stream.of(this.apply(ctx), function.apply(ctx)).flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new));
      };
   }

   default <R> QueryFunction<C, R> as(Class<? extends R> type, ClassLoader... loaders) {
      return (ctx) -> {
         Set<T> apply = this.apply(ctx);
         return (Set)apply.stream().findFirst().map((first) -> {
            return type.isAssignableFrom(first.getClass()) ? apply : (first instanceof String ? (Set)this.forNames(apply, type, loaders) : (first instanceof AnnotatedElement ? (Set)this.forNames(this.toNames(apply), type, loaders) : (Set)apply.stream().map((t) -> {
               return t;
            }).collect(Collectors.toCollection(LinkedHashSet::new))));
         }).orElse(apply);
      };
   }

   default <R> QueryFunction<C, Class<?>> asClass(ClassLoader... loaders) {
      return (ctx) -> {
         return (Set)this.forNames(this.apply(ctx), Class.class, loaders);
      };
   }

   default QueryFunction<C, String> asString() {
      return (ctx) -> {
         return new LinkedHashSet(this.toNames(new AnnotatedElement[]{(AnnotatedElement)this.apply(ctx)}));
      };
   }

   default <R> QueryFunction<C, Class<? extends R>> as() {
      return (ctx) -> {
         return new LinkedHashSet(this.apply(ctx));
      };
   }
}

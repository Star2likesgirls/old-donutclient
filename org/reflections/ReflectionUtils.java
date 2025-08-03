package org.reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.QueryFunction;
import org.reflections.util.ReflectionUtilsPredicates;
import org.reflections.util.UtilQueryBuilder;

public abstract class ReflectionUtils extends ReflectionUtilsPredicates {
   private static final List<String> objectMethodNames = Arrays.asList("equals", "hashCode", "toString", "wait", "notify", "notifyAll");
   public static final Predicate<Method> notObjectMethod = (m) -> {
      return !objectMethodNames.contains(m.getName());
   };
   public static final UtilQueryBuilder<Class<?>, Class<?>> SuperClass = (element) -> {
      return (ctx) -> {
         Class<?> superclass = element.getSuperclass();
         return superclass != null && !superclass.equals(Object.class) ? Collections.singleton(superclass) : Collections.emptySet();
      };
   };
   public static final UtilQueryBuilder<Class<?>, Class<?>> Interfaces = (element) -> {
      return (ctx) -> {
         return (LinkedHashSet)Stream.of(element.getInterfaces()).collect(Collectors.toCollection(LinkedHashSet::new));
      };
   };
   public static final UtilQueryBuilder<Class<?>, Class<?>> SuperTypes = new UtilQueryBuilder<Class<?>, Class<?>>() {
      public QueryFunction<Store, Class<?>> get(Class<?> element) {
         return ReflectionUtils.SuperClass.get(element).add(ReflectionUtils.Interfaces.get(element));
      }

      public QueryFunction<Store, Class<?>> of(Class<?> element) {
         QueryFunction var10000 = QueryFunction.single(element);
         UtilQueryBuilder var10001 = ReflectionUtils.SuperTypes;
         var10001.getClass();
         return var10000.getAll(var10001::get);
      }
   };
   public static final UtilQueryBuilder<AnnotatedElement, Annotation> Annotations = new UtilQueryBuilder<AnnotatedElement, Annotation>() {
      public QueryFunction<Store, Annotation> get(AnnotatedElement element) {
         return (ctx) -> {
            return (LinkedHashSet)Arrays.stream(element.getAnnotations()).collect(Collectors.toCollection(LinkedHashSet::new));
         };
      }

      public QueryFunction<Store, Annotation> of(AnnotatedElement element) {
         return ReflectionUtils.extendType().get(element).getAll(ReflectionUtils.Annotations::get, Annotation::annotationType);
      }
   };
   public static final UtilQueryBuilder<AnnotatedElement, Class<? extends Annotation>> AnnotationTypes = new UtilQueryBuilder<AnnotatedElement, Class<? extends Annotation>>() {
      public QueryFunction<Store, Class<? extends Annotation>> get(AnnotatedElement element) {
         return ReflectionUtils.Annotations.get(element).map(Annotation::annotationType);
      }

      public QueryFunction<Store, Class<? extends Annotation>> of(AnnotatedElement element) {
         return ReflectionUtils.extendType().get(element).getAll(ReflectionUtils.AnnotationTypes::get, (a) -> {
            return a;
         });
      }
   };
   public static final UtilQueryBuilder<Class<?>, Method> Methods = (element) -> {
      return (ctx) -> {
         return (LinkedHashSet)Arrays.stream(element.getDeclaredMethods()).filter(notObjectMethod).collect(Collectors.toCollection(LinkedHashSet::new));
      };
   };
   public static final UtilQueryBuilder<Class<?>, Constructor> Constructors = (element) -> {
      return (ctx) -> {
         return (LinkedHashSet)Arrays.stream(element.getDeclaredConstructors()).collect(Collectors.toCollection(LinkedHashSet::new));
      };
   };
   public static final UtilQueryBuilder<Class<?>, Field> Fields = (element) -> {
      return (ctx) -> {
         return (LinkedHashSet)Arrays.stream(element.getDeclaredFields()).collect(Collectors.toCollection(LinkedHashSet::new));
      };
   };
   public static final UtilQueryBuilder<String, URL> Resources = (element) -> {
      return (ctx) -> {
         return new HashSet(ClasspathHelper.forResource(element));
      };
   };

   public static <C, T> Set<T> get(QueryFunction<C, T> function) {
      return function.apply((Object)null);
   }

   public static <T> Set<T> get(QueryFunction<Store, T> queryFunction, Predicate<? super T>... predicates) {
      return get(queryFunction.filter((Predicate)Arrays.stream((Predicate[])predicates).reduce((t) -> {
         return true;
      }, Predicate::and)));
   }

   public static <T extends AnnotatedElement> UtilQueryBuilder<AnnotatedElement, T> extendType() {
      return (element) -> {
         if (element instanceof Class && !((Class)element).isAnnotation()) {
            QueryFunction<Store, Class<?>> single = QueryFunction.single((Class)element);
            UtilQueryBuilder var10002 = SuperTypes;
            var10002.getClass();
            return single.add(single.getAll(var10002::get));
         } else {
            return QueryFunction.single(element);
         }
      };
   }

   public static <T extends AnnotatedElement> Set<Annotation> getAllAnnotations(T type, Predicate<Annotation>... predicates) {
      return get(Annotations.of((Object)type), predicates);
   }

   public static Set<Class<?>> getAllSuperTypes(Class<?> type, Predicate<? super Class<?>>... predicates) {
      Predicate<? super Class<?>>[] filter = predicates != null && predicates.length != 0 ? predicates : new Predicate[]{(t) -> {
         return !Object.class.equals(t);
      }};
      return get(SuperTypes.of((Object)type), filter);
   }

   public static Set<Class<?>> getSuperTypes(Class<?> type) {
      return get(SuperTypes.get(type));
   }

   public static Set<Method> getAllMethods(Class<?> type, Predicate<? super Method>... predicates) {
      return get(Methods.of((Object)type), predicates);
   }

   public static Set<Method> getMethods(Class<?> t, Predicate<? super Method>... predicates) {
      return get(Methods.get(t), predicates);
   }

   public static Set<Constructor> getAllConstructors(Class<?> type, Predicate<? super Constructor>... predicates) {
      return get(Constructors.of((Object)type), predicates);
   }

   public static Set<Constructor> getConstructors(Class<?> t, Predicate<? super Constructor>... predicates) {
      return get(Constructors.get(t), predicates);
   }

   public static Set<Field> getAllFields(Class<?> type, Predicate<? super Field>... predicates) {
      return get(Fields.of((Object)type), predicates);
   }

   public static Set<Field> getFields(Class<?> type, Predicate<? super Field>... predicates) {
      return get(Fields.get(type), predicates);
   }

   public static <T extends AnnotatedElement> Set<Annotation> getAnnotations(T type, Predicate<Annotation>... predicates) {
      return get(Annotations.get(type), predicates);
   }

   public static Map<String, Object> toMap(Annotation annotation) {
      return (Map)get(Methods.of((Object)annotation.annotationType()).filter(notObjectMethod.and(withParametersCount(0)))).stream().collect(Collectors.toMap(Method::getName, (m) -> {
         Object v1 = invoke(m, annotation);
         return v1.getClass().isArray() && v1.getClass().getComponentType().isAnnotation() ? Stream.of((Annotation[])((Annotation[])v1)).map(ReflectionUtils::toMap).collect(Collectors.toList()) : v1;
      }));
   }

   public static Map<String, Object> toMap(Annotation annotation, AnnotatedElement element) {
      Map<String, Object> map = toMap(annotation);
      if (element != null) {
         map.put("annotatedElement", element);
      }

      return map;
   }

   public static Annotation toAnnotation(Map<String, Object> map) {
      return toAnnotation(map, (Class)map.get("annotationType"));
   }

   public static <T extends Annotation> T toAnnotation(Map<String, Object> map, Class<T> annotationType) {
      return (Annotation)Proxy.newProxyInstance(annotationType.getClassLoader(), new Class[]{annotationType}, (proxy, method, args) -> {
         return notObjectMethod.test(method) ? map.get(method.getName()) : method.invoke(map);
      });
   }

   public static Object invoke(Method method, Object obj, Object... args) {
      try {
         return method.invoke(obj, args);
      } catch (Exception var4) {
         return var4;
      }
   }
}

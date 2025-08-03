package org.reflections.scanners;

import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javassist.bytecode.ClassFile;
import org.reflections.Store;
import org.reflections.util.FilterBuilder;
import org.reflections.util.JavassistHelper;
import org.reflections.util.NameHelper;
import org.reflections.util.QueryBuilder;
import org.reflections.util.QueryFunction;
import org.reflections.vfs.Vfs;

public enum Scanners implements Scanner, QueryBuilder, NameHelper {
   SubTypes {
      {
         this.filterResultsBy((new FilterBuilder()).excludePattern("java\\.lang\\.Object"));
      }

      public void scan(ClassFile classFile, List<Entry<String, String>> entries) {
         entries.add(this.entry(classFile.getSuperclass(), classFile.getName()));
         entries.addAll(this.entries(Arrays.asList(classFile.getInterfaces()), classFile.getName()));
      }
   },
   TypesAnnotated {
      public boolean acceptResult(String annotation) {
         return super.acceptResult(annotation) || annotation.equals(Inherited.class.getName());
      }

      public void scan(ClassFile classFile, List<Entry<String, String>> entries) {
         classFile.getClass();
         entries.addAll(this.entries(JavassistHelper.getAnnotations(classFile::getAttribute), classFile.getName()));
      }
   },
   MethodsAnnotated {
      public void scan(ClassFile classFile, List<Entry<String, String>> entries) {
         JavassistHelper.getMethods(classFile).forEach((method) -> {
            method.getClass();
            entries.addAll(this.entries(JavassistHelper.getAnnotations(method::getAttribute), JavassistHelper.methodName(classFile, method)));
         });
      }
   },
   ConstructorsAnnotated {
      public void scan(ClassFile classFile, List<Entry<String, String>> entries) {
         JavassistHelper.getConstructors(classFile).forEach((constructor) -> {
            constructor.getClass();
            entries.addAll(this.entries(JavassistHelper.getAnnotations(constructor::getAttribute), JavassistHelper.methodName(classFile, constructor)));
         });
      }
   },
   FieldsAnnotated {
      public void scan(ClassFile classFile, List<Entry<String, String>> entries) {
         classFile.getFields().forEach((field) -> {
            field.getClass();
            entries.addAll(this.entries(JavassistHelper.getAnnotations(field::getAttribute), JavassistHelper.fieldName(classFile, field)));
         });
      }
   },
   Resources {
      public boolean acceptsInput(String file) {
         return !file.endsWith(".class");
      }

      public List<Entry<String, String>> scan(Vfs.File file) {
         return Collections.singletonList(this.entry(file.getName(), file.getRelativePath()));
      }

      public void scan(ClassFile classFile, List<Entry<String, String>> entries) {
         throw new IllegalStateException();
      }

      public QueryFunction<Store, String> with(String pattern) {
         return (store) -> {
            return (LinkedHashSet)((Map)store.getOrDefault(this.index(), Collections.emptyMap())).entrySet().stream().filter((entry) -> {
               return ((String)entry.getKey()).matches(pattern);
            }).flatMap((entry) -> {
               return ((Set)entry.getValue()).stream();
            }).collect(Collectors.toCollection(LinkedHashSet::new));
         };
      }
   },
   MethodsParameter {
      public void scan(ClassFile classFile, List<Entry<String, String>> entries) {
         JavassistHelper.getMethods(classFile).forEach((method) -> {
            String value = JavassistHelper.methodName(classFile, method);
            entries.addAll(this.entries(JavassistHelper.getParameters(method), value));
            JavassistHelper.getParametersAnnotations(method).forEach((annotations) -> {
               entries.addAll(this.entries(annotations, value));
            });
         });
      }
   },
   ConstructorsParameter {
      public void scan(ClassFile classFile, List<Entry<String, String>> entries) {
         JavassistHelper.getConstructors(classFile).forEach((constructor) -> {
            String value = JavassistHelper.methodName(classFile, constructor);
            entries.addAll(this.entries(JavassistHelper.getParameters(constructor), value));
            JavassistHelper.getParametersAnnotations(constructor).forEach((annotations) -> {
               entries.addAll(this.entries(annotations, value));
            });
         });
      }
   },
   MethodsSignature {
      public void scan(ClassFile classFile, List<Entry<String, String>> entries) {
         JavassistHelper.getMethods(classFile).forEach((method) -> {
            entries.add(this.entry(JavassistHelper.getParameters(method).toString(), JavassistHelper.methodName(classFile, method)));
         });
      }

      public QueryFunction<Store, String> with(AnnotatedElement... keys) {
         return QueryFunction.single(this.toNames(keys).toString()).getAll(this::get);
      }
   },
   ConstructorsSignature {
      public void scan(ClassFile classFile, List<Entry<String, String>> entries) {
         JavassistHelper.getConstructors(classFile).forEach((constructor) -> {
            entries.add(this.entry(JavassistHelper.getParameters(constructor).toString(), JavassistHelper.methodName(classFile, constructor)));
         });
      }

      public QueryFunction<Store, String> with(AnnotatedElement... keys) {
         return QueryFunction.single(this.toNames(keys).toString()).getAll(this::get);
      }
   },
   MethodsReturn {
      public void scan(ClassFile classFile, List<Entry<String, String>> entries) {
         JavassistHelper.getMethods(classFile).forEach((method) -> {
            entries.add(this.entry(JavassistHelper.getReturnType(method), JavassistHelper.methodName(classFile, method)));
         });
      }
   };

   private Predicate<String> resultFilter;

   private Scanners() {
      this.resultFilter = (s) -> {
         return true;
      };
   }

   public String index() {
      return this.name();
   }

   public Scanners filterResultsBy(Predicate<String> filter) {
      this.resultFilter = filter;
      return this;
   }

   public final List<Entry<String, String>> scan(ClassFile classFile) {
      List<Entry<String, String>> entries = new ArrayList();
      this.scan(classFile, entries);
      return (List)entries.stream().filter((a) -> {
         return this.acceptResult((String)a.getKey());
      }).collect(Collectors.toList());
   }

   abstract void scan(ClassFile var1, List<Entry<String, String>> var2);

   protected boolean acceptResult(String fqn) {
      return fqn != null && this.resultFilter.test(fqn);
   }

   // $FF: synthetic method
   Scanners(Object x2) {
      this();
   }
}

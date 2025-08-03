package org.reflections;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javassist.bytecode.ClassFile;
import javax.annotation.Nullable;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.MethodParameterNamesScanner;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;
import org.reflections.serializers.Serializer;
import org.reflections.serializers.XmlSerializer;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.reflections.util.NameHelper;
import org.reflections.util.QueryFunction;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reflections implements NameHelper {
   public static final Logger log = LoggerFactory.getLogger(Reflections.class);
   protected final transient Configuration configuration;
   protected final Store store;

   public Reflections(Configuration configuration) {
      this.configuration = configuration;
      Map<String, Map<String, Set<String>>> storeMap = this.scan();
      if (configuration.shouldExpandSuperTypes()) {
         this.expandSuperTypes((Map)storeMap.get(Scanners.SubTypes.index()), (Map)storeMap.get(Scanners.TypesAnnotated.index()));
      }

      this.store = new Store(storeMap);
   }

   public Reflections(Store store) {
      this.configuration = new ConfigurationBuilder();
      this.store = store;
   }

   public Reflections(String prefix, Scanner... scanners) {
      this(prefix, scanners);
   }

   public Reflections(Object... params) {
      this((Configuration)ConfigurationBuilder.build(params));
   }

   protected Reflections() {
      this.configuration = new ConfigurationBuilder();
      this.store = new Store(new HashMap());
   }

   protected Map<String, Map<String, Set<String>>> scan() {
      long start = System.currentTimeMillis();
      Map<String, Set<Entry<String, String>>> collect = (Map)this.configuration.getScanners().stream().map(Scanner::index).distinct().collect(Collectors.toMap((s) -> {
         return s;
      }, (s) -> {
         return Collections.synchronizedSet(new HashSet());
      }));
      Set<URL> urls = this.configuration.getUrls();
      (this.configuration.isParallel() ? (Stream)urls.stream().parallel() : urls.stream()).forEach((url) -> {
         Vfs.Dir dir = null;

         try {
            dir = Vfs.fromURL(url);
            Iterator var4 = dir.getFiles().iterator();

            while(true) {
               Vfs.File file;
               do {
                  if (!var4.hasNext()) {
                     return;
                  }

                  file = (Vfs.File)var4.next();
               } while(!this.doFilter(file, this.configuration.getInputsFilter()));

               ClassFile classFile = null;
               Iterator var7 = this.configuration.getScanners().iterator();

               while(var7.hasNext()) {
                  Scanner scanner = (Scanner)var7.next();

                  try {
                     scanner.getClass();
                     if (this.doFilter(file, scanner::acceptsInput)) {
                        List<Entry<String, String>> entries = scanner.scan(file);
                        if (entries == null) {
                           if (classFile == null) {
                              classFile = this.getClassFile(file);
                           }

                           entries = scanner.scan(classFile);
                        }

                        if (entries != null) {
                           ((Set)collect.get(scanner.index())).addAll(entries);
                        }
                     }
                  } catch (Exception var14) {
                     if (log != null) {
                        log.trace("could not scan file {} with scanner {}", new Object[]{file.getRelativePath(), scanner.getClass().getSimpleName(), var14});
                     }
                  }
               }
            }
         } catch (Exception var15) {
            if (log != null) {
               log.warn("could not create Vfs.Dir from url. ignoring the exception and continuing", var15);
            }
         } finally {
            if (dir != null) {
               dir.close();
            }

         }

      });
      Map<String, Map<String, Set<String>>> storeMap = (Map)collect.entrySet().stream().collect(Collectors.toMap(Entry::getKey, (entry) -> {
         return (HashMap)((Set)entry.getValue()).stream().filter((e) -> {
            return e.getKey() != null;
         }).collect(Collectors.groupingBy(Entry::getKey, HashMap::new, Collectors.mapping(Entry::getValue, Collectors.toSet())));
      }));
      if (log != null) {
         int keys = 0;
         int values = 0;

         Map map;
         for(Iterator var8 = storeMap.values().iterator(); var8.hasNext(); values = (int)((long)values + map.values().stream().mapToLong(Set::size).sum())) {
            map = (Map)var8.next();
            keys += map.size();
         }

         log.info(String.format("Reflections took %d ms to scan %d urls, producing %d keys and %d values", System.currentTimeMillis() - start, urls.size(), keys, values));
      }

      return storeMap;
   }

   private boolean doFilter(Vfs.File file, @Nullable Predicate<String> predicate) {
      String path = file.getRelativePath();
      String fqn = path.replace('/', '.');
      return predicate == null || predicate.test(path) || predicate.test(fqn);
   }

   private ClassFile getClassFile(Vfs.File file) {
      try {
         DataInputStream dis = new DataInputStream(new BufferedInputStream(file.openInputStream()));
         Throwable var3 = null;

         ClassFile var4;
         try {
            var4 = new ClassFile(dis);
         } catch (Throwable var14) {
            var3 = var14;
            throw var14;
         } finally {
            if (dis != null) {
               if (var3 != null) {
                  try {
                     dis.close();
                  } catch (Throwable var13) {
                     var3.addSuppressed(var13);
                  }
               } else {
                  dis.close();
               }
            }

         }

         return var4;
      } catch (Exception var16) {
         throw new ReflectionsException("could not create class object from file " + file.getRelativePath(), var16);
      }
   }

   public static Reflections collect() {
      return collect((String)"META-INF/reflections/", (Predicate)(new FilterBuilder()).includePattern(".*-reflections\\.xml"));
   }

   public static Reflections collect(String packagePrefix, Predicate<String> resourceNameFilter) {
      return collect(packagePrefix, resourceNameFilter, new XmlSerializer());
   }

   public static Reflections collect(String packagePrefix, Predicate<String> resourceNameFilter, Serializer serializer) {
      Collection<URL> urls = ClasspathHelper.forPackage(packagePrefix);
      Iterable<Vfs.File> files = Vfs.findFiles(urls, packagePrefix, resourceNameFilter);
      Reflections reflections = new Reflections();
      StreamSupport.stream(files.spliterator(), false).forEach((file) -> {
         try {
            InputStream inputStream = file.openInputStream();
            Throwable var4 = null;

            try {
               reflections.collect(inputStream, serializer);
            } catch (Throwable var14) {
               var4 = var14;
               throw var14;
            } finally {
               if (inputStream != null) {
                  if (var4 != null) {
                     try {
                        inputStream.close();
                     } catch (Throwable var13) {
                        var4.addSuppressed(var13);
                     }
                  } else {
                     inputStream.close();
                  }
               }

            }

         } catch (IOException var16) {
            throw new ReflectionsException("could not merge " + file, var16);
         }
      });
      return reflections;
   }

   public Reflections collect(InputStream inputStream, Serializer serializer) {
      return this.merge(serializer.read(inputStream));
   }

   public Reflections collect(File file, Serializer serializer) {
      try {
         FileInputStream inputStream = new FileInputStream(file);
         Throwable var4 = null;

         Reflections var5;
         try {
            var5 = this.collect((InputStream)inputStream, (Serializer)serializer);
         } catch (Throwable var15) {
            var4 = var15;
            throw var15;
         } finally {
            if (inputStream != null) {
               if (var4 != null) {
                  try {
                     inputStream.close();
                  } catch (Throwable var14) {
                     var4.addSuppressed(var14);
                  }
               } else {
                  inputStream.close();
               }
            }

         }

         return var5;
      } catch (IOException var17) {
         throw new ReflectionsException("could not obtain input stream from file " + file, var17);
      }
   }

   public Reflections merge(Reflections reflections) {
      reflections.store.forEach((index, map) -> {
         Map var10000 = (Map)this.store.merge(index, map, (m1, m2) -> {
            m2.forEach((k, v) -> {
               Set var10000 = (Set)m1.merge(k, v, (s1, s2) -> {
                  s1.addAll(s2);
                  return s1;
               });
            });
            return m1;
         });
      });
      return this;
   }

   public void expandSuperTypes(Map<String, Set<String>> subTypesStore, Map<String, Set<String>> typesAnnotatedStore) {
      if (subTypesStore != null && !subTypesStore.isEmpty()) {
         Set<String> keys = new LinkedHashSet(subTypesStore.keySet());
         keys.removeAll((Collection)subTypesStore.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
         keys.remove("java.lang.Object");
         Iterator var4 = keys.iterator();

         while(var4.hasNext()) {
            String key = (String)var4.next();
            Class<?> type = this.forClass(key, this.loaders());
            if (type != null) {
               this.expandSupertypes(subTypesStore, typesAnnotatedStore, key, type);
            }
         }

      }
   }

   private void expandSupertypes(Map<String, Set<String>> subTypesStore, Map<String, Set<String>> typesAnnotatedStore, String key, Class<?> type) {
      Set<Annotation> typeAnnotations = ReflectionUtils.getAnnotations(type);
      if (typesAnnotatedStore != null && !typeAnnotations.isEmpty()) {
         String typeName = type.getName();
         Iterator var7 = typeAnnotations.iterator();

         while(var7.hasNext()) {
            Annotation typeAnnotation = (Annotation)var7.next();
            String annotationName = typeAnnotation.annotationType().getName();
            ((Set)typesAnnotatedStore.computeIfAbsent(annotationName, (s) -> {
               return new HashSet();
            })).add(typeName);
         }
      }

      Iterator var10 = ReflectionUtils.getSuperTypes(type).iterator();

      while(var10.hasNext()) {
         Class<?> supertype = (Class)var10.next();
         String supertypeName = supertype.getName();
         if (subTypesStore.containsKey(supertypeName)) {
            ((Set)subTypesStore.get(supertypeName)).add(key);
         } else {
            ((Set)subTypesStore.computeIfAbsent(supertypeName, (s) -> {
               return new HashSet();
            })).add(key);
            this.expandSupertypes(subTypesStore, typesAnnotatedStore, supertypeName, supertype);
         }
      }

   }

   public <T> Set<T> get(QueryFunction<Store, T> query) {
      return query.apply(this.store);
   }

   public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
      return this.get(Scanners.SubTypes.of(new AnnotatedElement[]{type}).as(Class.class, this.loaders()));
   }

   public Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation) {
      return this.get(Scanners.SubTypes.of(Scanners.TypesAnnotated.with(new AnnotatedElement[]{annotation})).asClass(this.loaders()));
   }

   public Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation, boolean honorInherited) {
      if (!honorInherited) {
         return this.getTypesAnnotatedWith(annotation);
      } else {
         return annotation.isAnnotationPresent(Inherited.class) ? this.get(Scanners.TypesAnnotated.get(annotation).add(Scanners.SubTypes.of(Scanners.TypesAnnotated.get(annotation).filter((c) -> {
            return !this.forClass(c, this.loaders()).isInterface();
         }))).asClass(this.loaders())) : this.get(Scanners.TypesAnnotated.get(annotation).asClass(this.loaders()));
      }
   }

   public Set<Class<?>> getTypesAnnotatedWith(Annotation annotation) {
      return this.get(Scanners.SubTypes.of(Scanners.TypesAnnotated.of(Scanners.TypesAnnotated.get(annotation.annotationType()).filter((c) -> {
         return ReflectionUtils.withAnnotation(annotation).test(this.forClass(c, this.loaders()));
      }))).asClass(this.loaders()));
   }

   public Set<Class<?>> getTypesAnnotatedWith(Annotation annotation, boolean honorInherited) {
      if (!honorInherited) {
         return this.getTypesAnnotatedWith(annotation);
      } else {
         Class<? extends Annotation> type = annotation.annotationType();
         return type.isAnnotationPresent(Inherited.class) ? this.get(Scanners.TypesAnnotated.with(new AnnotatedElement[]{type}).asClass(this.loaders()).filter(ReflectionUtils.withAnnotation(annotation)).add(Scanners.SubTypes.of(Scanners.TypesAnnotated.with(new AnnotatedElement[]{type}).asClass(this.loaders()).filter((c) -> {
            return !c.isInterface();
         })))) : this.get(Scanners.TypesAnnotated.with(new AnnotatedElement[]{type}).asClass(this.loaders()).filter(ReflectionUtils.withAnnotation(annotation)));
      }
   }

   public Set<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
      return this.get(Scanners.MethodsAnnotated.with(new AnnotatedElement[]{annotation}).as(Method.class, this.loaders()));
   }

   public Set<Method> getMethodsAnnotatedWith(Annotation annotation) {
      return this.get(Scanners.MethodsAnnotated.with(new AnnotatedElement[]{annotation.annotationType()}).as(Method.class, this.loaders()).filter(ReflectionUtils.withAnnotation(annotation)));
   }

   public Set<Method> getMethodsWithSignature(Class<?>... types) {
      return this.get(Scanners.MethodsSignature.with(types).as(Method.class, this.loaders()));
   }

   public Set<Method> getMethodsWithParameter(AnnotatedElement type) {
      return this.get(Scanners.MethodsParameter.with(new AnnotatedElement[]{type}).as(Method.class, this.loaders()));
   }

   public Set<Method> getMethodsReturn(Class<?> type) {
      return this.get(Scanners.MethodsReturn.of(new AnnotatedElement[]{type}).as(Method.class, this.loaders()));
   }

   public Set<Constructor> getConstructorsAnnotatedWith(Class<? extends Annotation> annotation) {
      return this.get(Scanners.ConstructorsAnnotated.with(new AnnotatedElement[]{annotation}).as(Constructor.class, this.loaders()));
   }

   public Set<Constructor> getConstructorsAnnotatedWith(Annotation annotation) {
      return this.get(Scanners.ConstructorsAnnotated.with(new AnnotatedElement[]{annotation.annotationType()}).as(Constructor.class, this.loaders()).filter(ReflectionUtils.withAnyParameterAnnotation(annotation)));
   }

   public Set<Constructor> getConstructorsWithSignature(Class<?>... types) {
      return this.get(Scanners.ConstructorsSignature.with(types).as(Constructor.class, this.loaders()));
   }

   public Set<Constructor> getConstructorsWithParameter(AnnotatedElement type) {
      return this.get(Scanners.ConstructorsParameter.of(new AnnotatedElement[]{type}).as(Constructor.class, this.loaders()));
   }

   public Set<Field> getFieldsAnnotatedWith(Class<? extends Annotation> annotation) {
      return this.get(Scanners.FieldsAnnotated.with(new AnnotatedElement[]{annotation}).as(Field.class, this.loaders()));
   }

   public Set<Field> getFieldsAnnotatedWith(Annotation annotation) {
      return this.get(Scanners.FieldsAnnotated.with(new AnnotatedElement[]{annotation.annotationType()}).as(Field.class, this.loaders()).filter(ReflectionUtils.withAnnotation(annotation)));
   }

   public Set<String> getResources(String pattern) {
      return this.get(Scanners.Resources.with(pattern));
   }

   public Set<String> getResources(Pattern pattern) {
      return this.getResources(pattern.pattern());
   }

   public List<String> getMemberParameterNames(Member member) {
      return (List)((Set)((Map)this.store.getOrDefault(MethodParameterNamesScanner.class.getSimpleName(), Collections.emptyMap())).getOrDefault(this.toName((AnnotatedElement)member), Collections.emptySet())).stream().flatMap((s) -> {
         return Stream.of(s.split(", "));
      }).collect(Collectors.toList());
   }

   public Collection<Member> getMemberUsage(Member member) {
      Set<String> usages = (Set)((Map)this.store.getOrDefault(MemberUsageScanner.class.getSimpleName(), Collections.emptyMap())).getOrDefault(this.toName((AnnotatedElement)member), Collections.emptySet());
      return this.forNames(usages, Member.class, this.loaders());
   }

   /** @deprecated */
   @Deprecated
   public Set<String> getAllTypes() {
      return this.getAll(Scanners.SubTypes);
   }

   public Set<String> getAll(Scanner scanner) {
      Map<String, Set<String>> map = (Map)this.store.getOrDefault(scanner.index(), Collections.emptyMap());
      return (Set)Stream.concat(map.keySet().stream(), map.values().stream().flatMap(Collection::stream)).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public Store getStore() {
      return this.store;
   }

   public Configuration getConfiguration() {
      return this.configuration;
   }

   public File save(String filename) {
      return this.save(filename, new XmlSerializer());
   }

   public File save(String filename, Serializer serializer) {
      return serializer.save(this, filename);
   }

   ClassLoader[] loaders() {
      return this.configuration.getClassLoaders();
   }
}

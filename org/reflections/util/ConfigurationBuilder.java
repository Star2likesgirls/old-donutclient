package org.reflections.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.reflections.Configuration;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;

public class ConfigurationBuilder implements Configuration {
   public static final Set<Scanner> DEFAULT_SCANNERS;
   public static final Predicate<String> DEFAULT_INPUTS_FILTER;
   private Set<Scanner> scanners;
   private Set<URL> urls = new HashSet();
   private Predicate<String> inputsFilter;
   private boolean isParallel = true;
   private ClassLoader[] classLoaders;
   private boolean expandSuperTypes = true;

   public static ConfigurationBuilder build(Object... params) {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      List<Object> parameters = new ArrayList();
      Object[] var3 = params;
      int var4 = params.length;

      Object param;
      for(int var5 = 0; var5 < var4; ++var5) {
         param = var3[var5];
         if (param.getClass().isArray()) {
            Object[] var15 = (Object[])((Object[])param);
            int var16 = var15.length;

            for(int var9 = 0; var9 < var16; ++var9) {
               Object p = var15[var9];
               parameters.add(p);
            }
         } else if (param instanceof Iterable) {
            Iterator var7 = ((Iterable)param).iterator();

            while(var7.hasNext()) {
               Object p = var7.next();
               parameters.add(p);
            }
         } else {
            parameters.add(param);
         }
      }

      ClassLoader[] loaders = (ClassLoader[])Stream.of(params).filter((px) -> {
         return px instanceof ClassLoader;
      }).distinct().toArray((x$0) -> {
         return new ClassLoader[x$0];
      });
      if (loaders.length != 0) {
         builder.addClassLoaders(loaders);
      }

      FilterBuilder inputsFilter = new FilterBuilder();
      builder.filterInputsBy(inputsFilter);
      Iterator var14 = parameters.iterator();

      while(true) {
         while(var14.hasNext()) {
            param = var14.next();
            if (param instanceof String && !((String)param).isEmpty()) {
               builder.forPackage((String)param, loaders);
               inputsFilter.includePackage((String)param);
            } else if (param instanceof Class && !Scanner.class.isAssignableFrom((Class)param)) {
               builder.addUrls(ClasspathHelper.forClass((Class)param, loaders));
               inputsFilter.includePackage(((Class)param).getPackage().getName());
            } else if (param instanceof URL) {
               builder.addUrls((URL)param);
            } else if (param instanceof Scanner) {
               builder.addScanners((Scanner)param);
            } else if (param instanceof Class && Scanner.class.isAssignableFrom((Class)param)) {
               try {
                  builder.addScanners((Scanner)((Class)param).getDeclaredConstructor().newInstance());
               } catch (Exception var11) {
                  throw new RuntimeException(var11);
               }
            } else {
               if (!(param instanceof Predicate)) {
                  throw new ReflectionsException("could not use param '" + param + "'");
               }

               builder.filterInputsBy((Predicate)param);
            }
         }

         if (builder.getUrls().isEmpty()) {
            builder.addUrls(ClasspathHelper.forClassLoader(loaders));
         }

         return builder;
      }
   }

   public ConfigurationBuilder forPackage(String pkg, ClassLoader... classLoaders) {
      return this.addUrls(ClasspathHelper.forPackage(pkg, classLoaders));
   }

   public ConfigurationBuilder forPackages(String... packages) {
      String[] var2 = packages;
      int var3 = packages.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String pkg = var2[var4];
         this.forPackage(pkg);
      }

      return this;
   }

   public Set<Scanner> getScanners() {
      return this.scanners != null ? this.scanners : DEFAULT_SCANNERS;
   }

   public ConfigurationBuilder setScanners(Scanner... scanners) {
      this.scanners = new HashSet(Arrays.asList(scanners));
      return this;
   }

   public ConfigurationBuilder addScanners(Scanner... scanners) {
      if (this.scanners == null) {
         this.setScanners(scanners);
      } else {
         this.scanners.addAll(Arrays.asList(scanners));
      }

      return this;
   }

   public Set<URL> getUrls() {
      return this.urls;
   }

   public ConfigurationBuilder setUrls(Collection<URL> urls) {
      this.urls = new HashSet(urls);
      return this;
   }

   public ConfigurationBuilder setUrls(URL... urls) {
      return this.setUrls((Collection)Arrays.asList(urls));
   }

   public ConfigurationBuilder addUrls(Collection<URL> urls) {
      this.urls.addAll(urls);
      return this;
   }

   public ConfigurationBuilder addUrls(URL... urls) {
      return this.addUrls((Collection)Arrays.asList(urls));
   }

   public Predicate<String> getInputsFilter() {
      return this.inputsFilter != null ? this.inputsFilter : DEFAULT_INPUTS_FILTER;
   }

   public ConfigurationBuilder setInputsFilter(Predicate<String> inputsFilter) {
      this.inputsFilter = inputsFilter;
      return this;
   }

   public ConfigurationBuilder filterInputsBy(Predicate<String> inputsFilter) {
      return this.setInputsFilter(inputsFilter);
   }

   public boolean isParallel() {
      return this.isParallel;
   }

   public ConfigurationBuilder setParallel(boolean parallel) {
      this.isParallel = parallel;
      return this;
   }

   public ClassLoader[] getClassLoaders() {
      return this.classLoaders;
   }

   public ConfigurationBuilder setClassLoaders(ClassLoader[] classLoaders) {
      this.classLoaders = classLoaders;
      return this;
   }

   public ConfigurationBuilder addClassLoaders(ClassLoader... classLoaders) {
      this.classLoaders = this.classLoaders == null ? classLoaders : (ClassLoader[])Stream.concat(Arrays.stream(this.classLoaders), Arrays.stream(classLoaders)).distinct().toArray((x$0) -> {
         return new ClassLoader[x$0];
      });
      return this;
   }

   public boolean shouldExpandSuperTypes() {
      return this.expandSuperTypes;
   }

   public ConfigurationBuilder setExpandSuperTypes(boolean expandSuperTypes) {
      this.expandSuperTypes = expandSuperTypes;
      return this;
   }

   static {
      DEFAULT_SCANNERS = new HashSet(Arrays.asList(Scanners.TypesAnnotated, Scanners.SubTypes));
      DEFAULT_INPUTS_FILTER = (t) -> {
         return true;
      };
   }
}

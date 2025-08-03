package org.reflections.util;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.reflections.ReflectionsException;

public interface NameHelper {
   List<String> primitiveNames = Arrays.asList("boolean", "char", "byte", "short", "int", "long", "float", "double", "void");
   List<Class<?>> primitiveTypes = Arrays.asList(Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Void.TYPE);
   List<String> primitiveDescriptors = Arrays.asList("Z", "C", "B", "S", "I", "J", "F", "D", "V");

   default String toName(AnnotatedElement element) {
      return element.getClass().equals(Class.class) ? this.toName((Class)element) : (element.getClass().equals(Constructor.class) ? this.toName((Constructor)element) : (element.getClass().equals(Method.class) ? this.toName((Method)element) : (element.getClass().equals(Field.class) ? this.toName((Field)element) : null)));
   }

   default String toName(Class<?> type) {
      int dim;
      for(dim = 0; type.isArray(); type = type.getComponentType()) {
         ++dim;
      }

      return type.getName() + String.join("", Collections.nCopies(dim, "[]"));
   }

   default String toName(Constructor<?> constructor) {
      return String.format("%s.<init>(%s)", constructor.getName(), String.join(", ", this.toNames((AnnotatedElement[])constructor.getParameterTypes())));
   }

   default String toName(Method method) {
      return String.format("%s.%s(%s)", method.getDeclaringClass().getName(), method.getName(), String.join(", ", this.toNames((AnnotatedElement[])method.getParameterTypes())));
   }

   default String toName(Field field) {
      return String.format("%s.%s", field.getDeclaringClass().getName(), field.getName());
   }

   default Collection<String> toNames(Collection<? extends AnnotatedElement> elements) {
      return (Collection)elements.stream().map(this::toName).filter(Objects::nonNull).collect(Collectors.toList());
   }

   default Collection<String> toNames(AnnotatedElement... elements) {
      return this.toNames((Collection)Arrays.asList(elements));
   }

   default <T> T forName(String name, Class<T> resultType, ClassLoader... loaders) {
      return resultType.equals(Class.class) ? this.forClass(name, loaders) : (resultType.equals(Constructor.class) ? this.forConstructor(name, loaders) : (resultType.equals(Method.class) ? this.forMethod(name, loaders) : (resultType.equals(Field.class) ? this.forField(name, loaders) : (resultType.equals(Member.class) ? this.forMember(name, loaders) : null))));
   }

   default Class<?> forClass(String typeName, ClassLoader... loaders) {
      if (primitiveNames.contains(typeName)) {
         return (Class)primitiveTypes.get(primitiveNames.indexOf(typeName));
      } else {
         String type;
         if (typeName.contains("[")) {
            int i = typeName.indexOf("[");
            type = typeName.substring(0, i);
            String array = typeName.substring(i).replace("]", "");
            if (primitiveNames.contains(type)) {
               type = (String)primitiveDescriptors.get(primitiveNames.indexOf(type));
            } else {
               type = "L" + type + ";";
            }

            type = array + type;
         } else {
            type = typeName;
         }

         ClassLoader[] var11 = ClasspathHelper.classLoaders(loaders);
         int var12 = var11.length;
         int var6 = 0;

         while(var6 < var12) {
            ClassLoader classLoader = var11[var6];
            if (type.contains("[")) {
               try {
                  return Class.forName(type, false, classLoader);
               } catch (Throwable var10) {
               }
            }

            try {
               return classLoader.loadClass(type);
            } catch (Throwable var9) {
               ++var6;
            }
         }

         return null;
      }
   }

   default Member forMember(String descriptor, ClassLoader... loaders) throws ReflectionsException {
      int p0 = descriptor.lastIndexOf(40);
      String memberKey = p0 != -1 ? descriptor.substring(0, p0) : descriptor;
      String methodParameters = p0 != -1 ? descriptor.substring(p0 + 1, descriptor.lastIndexOf(41)) : "";
      int p1 = Math.max(memberKey.lastIndexOf(46), memberKey.lastIndexOf("$"));
      String className = memberKey.substring(0, p1);
      String memberName = memberKey.substring(p1 + 1);
      Class<?>[] parameterTypes = null;
      if (!methodParameters.isEmpty()) {
         String[] parameterNames = methodParameters.split(",");
         parameterTypes = (Class[])Arrays.stream(parameterNames).map((name) -> {
            return this.forClass(name.trim(), loaders);
         }).toArray((x$0) -> {
            return new Class[x$0];
         });
      }

      Class aClass;
      try {
         aClass = this.forClass(className, loaders);
      } catch (Exception var12) {
         return null;
      }

      while(aClass != null) {
         try {
            if (descriptor.contains("(")) {
               if (!descriptor.contains("init>")) {
                  return aClass.isInterface() ? aClass.getMethod(memberName, parameterTypes) : aClass.getDeclaredMethod(memberName, parameterTypes);
               }

               return aClass.isInterface() ? aClass.getConstructor(parameterTypes) : aClass.getDeclaredConstructor(parameterTypes);
            }

            return aClass.isInterface() ? aClass.getField(memberName) : aClass.getDeclaredField(memberName);
         } catch (Exception var13) {
            aClass = aClass.getSuperclass();
         }
      }

      return null;
   }

   @Nullable
   default <T extends AnnotatedElement> T forElement(String descriptor, Class<T> resultType, ClassLoader[] loaders) {
      Member member = this.forMember(descriptor, loaders);
      return member != null && member.getClass().equals(resultType) ? (AnnotatedElement)member : null;
   }

   @Nullable
   default Method forMethod(String descriptor, ClassLoader... loaders) throws ReflectionsException {
      return (Method)this.forElement(descriptor, Method.class, loaders);
   }

   default Constructor<?> forConstructor(String descriptor, ClassLoader... loaders) throws ReflectionsException {
      return (Constructor)this.forElement(descriptor, Constructor.class, loaders);
   }

   @Nullable
   default Field forField(String descriptor, ClassLoader... loaders) {
      return (Field)this.forElement(descriptor, Field.class, loaders);
   }

   default <T> Collection<T> forNames(Collection<String> names, Class<T> resultType, ClassLoader... loaders) {
      return (Collection)names.stream().map((name) -> {
         return this.forName(name, resultType, loaders);
      }).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   default Collection<Class<?>> forNames(Collection<String> names, ClassLoader... loaders) {
      return this.forNames(names, Class.class, loaders);
   }
}

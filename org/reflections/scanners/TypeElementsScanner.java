package org.reflections.scanners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javassist.bytecode.ClassFile;
import org.reflections.util.JavassistHelper;

public class TypeElementsScanner implements Scanner {
   private boolean includeFields = true;
   private boolean includeMethods = true;
   private boolean includeAnnotations = true;
   private boolean publicOnly = true;
   private Predicate<String> resultFilter = (s) -> {
      return true;
   };

   public List<Entry<String, String>> scan(ClassFile classFile) {
      List<Entry<String, String>> entries = new ArrayList();
      String className = classFile.getName();
      if (this.resultFilter.test(className) && this.isPublic(classFile)) {
         entries.add(this.entry(className, ""));
         if (this.includeFields) {
            classFile.getFields().forEach((field) -> {
               entries.add(this.entry(className, field.getName()));
            });
         }

         if (this.includeMethods) {
            classFile.getMethods().stream().filter(this::isPublic).forEach((method) -> {
               entries.add(this.entry(className, method.getName() + "(" + String.join(", ", JavassistHelper.getParameters(method)) + ")"));
            });
         }

         if (this.includeAnnotations) {
            classFile.getClass();
            JavassistHelper.getAnnotations(classFile::getAttribute).stream().filter(this.resultFilter).forEach((annotation) -> {
               entries.add(this.entry(className, "@" + annotation));
            });
         }
      }

      return entries;
   }

   private boolean isPublic(Object object) {
      return !this.publicOnly || JavassistHelper.isPublic(object);
   }

   public TypeElementsScanner filterResultsBy(Predicate<String> filter) {
      this.resultFilter = filter;
      return this;
   }

   public TypeElementsScanner includeFields() {
      return this.includeFields(true);
   }

   public TypeElementsScanner includeFields(boolean include) {
      this.includeFields = include;
      return this;
   }

   public TypeElementsScanner includeMethods() {
      return this.includeMethods(true);
   }

   public TypeElementsScanner includeMethods(boolean include) {
      this.includeMethods = include;
      return this;
   }

   public TypeElementsScanner includeAnnotations() {
      return this.includeAnnotations(true);
   }

   public TypeElementsScanner includeAnnotations(boolean include) {
      this.includeAnnotations = include;
      return this;
   }

   public TypeElementsScanner publicOnly(boolean only) {
      this.publicOnly = only;
      return this;
   }

   public TypeElementsScanner publicOnly() {
      return this.publicOnly(true);
   }
}

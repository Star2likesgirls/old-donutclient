package org.reflections.serializers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.reflections.Reflections;
import org.reflections.scanners.TypeElementsScanner;

public class JavaCodeSerializer implements Serializer {
   private static final String pathSeparator = "_";
   private static final String doubleSeparator = "__";
   private static final String dotSeparator = ".";
   private static final String arrayDescriptor = "$$";
   private static final String tokenSeparator = "_";
   private StringBuilder sb;
   private List<String> prevPaths;
   private int indent;

   public Reflections read(InputStream inputStream) {
      throw new UnsupportedOperationException("read is not implemented on JavaCodeSerializer");
   }

   public File save(Reflections reflections, String name) {
      if (name.endsWith("/")) {
         name = name.substring(0, name.length() - 1);
      }

      String filename = name.replace('.', '/').concat(".java");
      File file = Serializer.prepareFile(filename);
      int lastDot = name.lastIndexOf(46);
      String packageName;
      String className;
      if (lastDot == -1) {
         packageName = "";
         className = name.substring(name.lastIndexOf(47) + 1);
      } else {
         packageName = name.substring(name.lastIndexOf(47) + 1, lastDot);
         className = name.substring(lastDot + 1);
      }

      try {
         this.sb = new StringBuilder();
         this.sb.append("//generated using Reflections JavaCodeSerializer").append(" [").append(new Date()).append("]").append("\n");
         if (packageName.length() != 0) {
            this.sb.append("package ").append(packageName).append(";\n");
            this.sb.append("\n");
         }

         this.sb.append("public interface ").append(className).append(" {\n\n");
         this.toString(reflections);
         this.sb.append("}\n");
         Files.write((new File(filename)).toPath(), this.sb.toString().getBytes(Charset.defaultCharset()), new OpenOption[0]);
         return file;
      } catch (IOException var9) {
         throw new RuntimeException();
      }
   }

   private void toString(Reflections reflections) {
      Map<String, Set<String>> map = (Map)reflections.getStore().get(TypeElementsScanner.class.getSimpleName());
      this.prevPaths = new ArrayList();
      this.indent = 1;
      map.keySet().stream().sorted().forEach((fqn) -> {
         List<String> typePaths = Arrays.asList(fqn.split("\\."));
         String className = fqn.substring(fqn.lastIndexOf(46) + 1);
         List<String> fields = new ArrayList();
         List<String> methods = new ArrayList();
         List<String> annotations = new ArrayList();
         ((Set)map.get(fqn)).stream().sorted().forEach((element) -> {
            if (element.startsWith("@")) {
               annotations.add(element.substring(1));
            } else if (element.contains("(")) {
               if (!element.startsWith("<")) {
                  int i = element.indexOf(40);
                  String name = element.substring(0, i);
                  String params = element.substring(i + 1, element.indexOf(")"));
                  String paramsDescriptor = params.length() != 0 ? "_" + params.replace(".", "_").replace(", ", "__").replace("[]", "$$") : "";
                  methods.add(!methods.contains(name) ? name : name + paramsDescriptor);
               }
            } else if (!element.isEmpty()) {
               fields.add(element);
            }

         });
         int i = this.indentOpen(typePaths, this.prevPaths);
         this.addPackages(typePaths, i);
         this.addClass(typePaths, className);
         this.addFields(typePaths, fields);
         this.addMethods(typePaths, fields, methods);
         this.addAnnotations(typePaths, annotations);
         this.prevPaths = typePaths;
      });
      this.indentClose(this.prevPaths);
   }

   protected int indentOpen(List<String> typePaths, List<String> prevPaths) {
      int i;
      for(i = 0; i < Math.min(typePaths.size(), prevPaths.size()) && ((String)typePaths.get(i)).equals(prevPaths.get(i)); ++i) {
      }

      for(int j = prevPaths.size(); j > i; --j) {
         this.sb.append(this.indent(--this.indent)).append("}\n");
      }

      return i;
   }

   protected void indentClose(List<String> prevPaths) {
      for(int j = prevPaths.size(); j >= 1; --j) {
         this.sb.append(this.indent(j)).append("}\n");
      }

   }

   protected void addPackages(List<String> typePaths, int i) {
      for(int j = i; j < typePaths.size() - 1; ++j) {
         this.sb.append(this.indent(this.indent++)).append("interface ").append(this.uniqueName((String)typePaths.get(j), typePaths, j)).append(" {\n");
      }

   }

   protected void addClass(List<String> typePaths, String className) {
      this.sb.append(this.indent(this.indent++)).append("interface ").append(this.uniqueName(className, typePaths, typePaths.size() - 1)).append(" {\n");
   }

   protected void addFields(List<String> typePaths, List<String> fields) {
      if (!fields.isEmpty()) {
         this.sb.append(this.indent(this.indent++)).append("interface fields {\n");
         Iterator var3 = fields.iterator();

         while(var3.hasNext()) {
            String field = (String)var3.next();
            this.sb.append(this.indent(this.indent)).append("interface ").append(this.uniqueName(field, typePaths)).append(" {}\n");
         }

         this.sb.append(this.indent(--this.indent)).append("}\n");
      }

   }

   protected void addMethods(List<String> typePaths, List<String> fields, List<String> methods) {
      if (!methods.isEmpty()) {
         this.sb.append(this.indent(this.indent++)).append("interface methods {\n");
         Iterator var4 = methods.iterator();

         while(var4.hasNext()) {
            String method = (String)var4.next();
            String methodName = this.uniqueName(method, fields);
            this.sb.append(this.indent(this.indent)).append("interface ").append(this.uniqueName(methodName, typePaths)).append(" {}\n");
         }

         this.sb.append(this.indent(--this.indent)).append("}\n");
      }

   }

   protected void addAnnotations(List<String> typePaths, List<String> annotations) {
      if (!annotations.isEmpty()) {
         this.sb.append(this.indent(this.indent++)).append("interface annotations {\n");
         Iterator var3 = annotations.iterator();

         while(var3.hasNext()) {
            String annotation = (String)var3.next();
            this.sb.append(this.indent(this.indent)).append("interface ").append(this.uniqueName(annotation, typePaths)).append(" {}\n");
         }

         this.sb.append(this.indent(--this.indent)).append("}\n");
      }

   }

   private String uniqueName(String candidate, List<String> prev, int offset) {
      String normalized = this.normalize(candidate);

      for(int i = 0; i < offset; ++i) {
         if (normalized.equals(prev.get(i))) {
            return this.uniqueName(normalized + "_", prev, offset);
         }
      }

      return normalized;
   }

   private String normalize(String candidate) {
      return candidate.replace(".", "_");
   }

   private String uniqueName(String candidate, List<String> prev) {
      return this.uniqueName(candidate, prev, prev.size());
   }

   private String indent(int times) {
      return (String)IntStream.range(0, times).mapToObj((i) -> {
         return "  ";
      }).collect(Collectors.joining());
   }
}

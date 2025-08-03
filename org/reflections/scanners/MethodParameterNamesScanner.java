package org.reflections.scanners;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import org.reflections.util.JavassistHelper;

public class MethodParameterNamesScanner implements Scanner {
   public List<Entry<String, String>> scan(ClassFile classFile) {
      List<Entry<String, String>> entries = new ArrayList();
      Iterator var3 = classFile.getMethods().iterator();

      while(var3.hasNext()) {
         MethodInfo method = (MethodInfo)var3.next();
         String key = JavassistHelper.methodName(classFile, method);
         String value = this.getString(method);
         if (!value.isEmpty()) {
            entries.add(this.entry(key, value));
         }
      }

      return entries;
   }

   private String getString(MethodInfo method) {
      CodeAttribute codeAttribute = method.getCodeAttribute();
      LocalVariableAttribute table = codeAttribute != null ? (LocalVariableAttribute)codeAttribute.getAttribute("LocalVariableTable") : null;
      int length = JavassistHelper.getParameters(method).size();
      if (length > 0) {
         int shift = Modifier.isStatic(method.getAccessFlags()) ? 0 : 1;
         return (String)IntStream.range(shift, length + shift).mapToObj((i) -> {
            return method.getConstPool().getUtf8Info(table.nameIndex(i));
         }).filter((name) -> {
            return !name.startsWith("this$");
         }).collect(Collectors.joining(", "));
      } else {
         return "";
      }
   }
}

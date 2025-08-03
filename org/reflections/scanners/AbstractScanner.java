package org.reflections.scanners;

import java.util.List;
import java.util.Map.Entry;
import javassist.bytecode.ClassFile;

/** @deprecated */
@Deprecated
class AbstractScanner implements Scanner {
   protected final Scanner scanner;

   AbstractScanner(Scanner scanner) {
      this.scanner = scanner;
   }

   public String index() {
      return this.scanner.index();
   }

   public List<Entry<String, String>> scan(ClassFile cls) {
      return this.scanner.scan(cls);
   }
}

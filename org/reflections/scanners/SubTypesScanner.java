package org.reflections.scanners;

import java.util.List;
import java.util.Map.Entry;
import javassist.bytecode.ClassFile;

/** @deprecated */
@Deprecated
public class SubTypesScanner extends AbstractScanner {
   /** @deprecated */
   @Deprecated
   public SubTypesScanner() {
      super(Scanners.SubTypes);
   }

   /** @deprecated */
   @Deprecated
   public SubTypesScanner(boolean excludeObjectClass) {
      super(excludeObjectClass ? Scanners.SubTypes : Scanners.SubTypes.filterResultsBy((s) -> {
         return true;
      }));
   }

   public List<Entry<String, String>> scan(ClassFile cls) {
      return this.scanner.scan(cls);
   }
}

package org.reflections.serializers;

import java.io.File;
import java.io.InputStream;
import org.reflections.Reflections;

public interface Serializer {
   Reflections read(InputStream var1);

   File save(Reflections var1, String var2);

   static File prepareFile(String filename) {
      File file = new File(filename);
      File parent = file.getAbsoluteFile().getParentFile();
      if (!parent.exists()) {
         parent.mkdirs();
      }

      return file;
   }
}

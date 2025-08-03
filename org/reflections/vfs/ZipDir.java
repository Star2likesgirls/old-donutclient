package org.reflections.vfs;

import java.io.IOException;
import java.util.jar.JarFile;
import org.reflections.Reflections;

public class ZipDir implements Vfs.Dir {
   final java.util.zip.ZipFile jarFile;

   public ZipDir(JarFile jarFile) {
      this.jarFile = jarFile;
   }

   public String getPath() {
      return this.jarFile != null ? this.jarFile.getName().replace("\\", "/") : "/NO-SUCH-DIRECTORY/";
   }

   public Iterable<Vfs.File> getFiles() {
      return () -> {
         return this.jarFile.stream().filter((entry) -> {
            return !entry.isDirectory();
         }).map((entry) -> {
            return new ZipFile(this, entry);
         }).iterator();
      };
   }

   public void close() {
      try {
         this.jarFile.close();
      } catch (IOException var2) {
         if (Reflections.log != null) {
            Reflections.log.warn("Could not close JarFile", var2);
         }
      }

   }

   public String toString() {
      return this.jarFile.getName();
   }
}

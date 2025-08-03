package org.reflections.vfs;

import java.net.URL;
import java.util.Iterator;
import java.util.Stack;
import java.util.jar.JarFile;
import org.jboss.vfs.VirtualFile;

public class JbossDir implements Vfs.Dir {
   private final VirtualFile virtualFile;

   private JbossDir(VirtualFile virtualFile) {
      this.virtualFile = virtualFile;
   }

   public static Vfs.Dir createDir(URL url) throws Exception {
      VirtualFile virtualFile = (VirtualFile)url.openConnection().getContent();
      return (Vfs.Dir)(virtualFile.isFile() ? new ZipDir(new JarFile(virtualFile.getPhysicalFile())) : new JbossDir(virtualFile));
   }

   public String getPath() {
      return this.virtualFile.getPathName();
   }

   public Iterable<Vfs.File> getFiles() {
      return () -> {
         return new Iterator<Vfs.File>() {
            Vfs.File entry = null;
            final Stack stack = new Stack();

            {
               this.stack.addAll(JbossDir.this.virtualFile.getChildren());
            }

            public boolean hasNext() {
               return this.entry != null || (this.entry = this.computeNext()) != null;
            }

            public Vfs.File next() {
               Vfs.File next = this.entry;
               this.entry = null;
               return next;
            }

            private Vfs.File computeNext() {
               while(true) {
                  if (!this.stack.isEmpty()) {
                     VirtualFile file = (VirtualFile)this.stack.pop();
                     if (file.isDirectory()) {
                        this.stack.addAll(file.getChildren());
                        continue;
                     }

                     return new JbossFile(JbossDir.this, file);
                  }

                  return null;
               }
            }
         };
      };
   }
}

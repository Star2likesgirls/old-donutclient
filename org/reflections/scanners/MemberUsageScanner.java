package org.reflections.scanners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javassist.CannotCompileException;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;
import javax.annotation.Nonnull;
import org.reflections.ReflectionsException;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.JavassistHelper;

public class MemberUsageScanner implements Scanner {
   private Predicate<String> resultFilter;
   private final ClassLoader[] classLoaders;
   private volatile ClassPool classPool;

   public MemberUsageScanner() {
      this(ClasspathHelper.classLoaders());
   }

   public MemberUsageScanner(@Nonnull ClassLoader[] classLoaders) {
      this.resultFilter = (s) -> {
         return true;
      };
      this.classLoaders = classLoaders;
   }

   public List<Entry<String, String>> scan(ClassFile classFile) {
      List<Entry<String, String>> entries = new ArrayList();
      CtClass ctClass = null;

      try {
         ctClass = this.getClassPool().get(classFile.getName());
         CtConstructor[] var4 = ctClass.getDeclaredConstructors();
         int var5 = var4.length;

         int var6;
         for(var6 = 0; var6 < var5; ++var6) {
            CtBehavior member = var4[var6];
            this.scanMember(member, entries);
         }

         CtMethod[] var13 = ctClass.getDeclaredMethods();
         var5 = var13.length;

         for(var6 = 0; var6 < var5; ++var6) {
            CtBehavior member = var13[var6];
            this.scanMember(member, entries);
         }
      } catch (Exception var11) {
         throw new ReflectionsException("Could not scan method usage for " + classFile.getName(), var11);
      } finally {
         if (ctClass != null) {
            ctClass.detach();
         }

      }

      return entries;
   }

   public Scanner filterResultsBy(Predicate<String> filter) {
      this.resultFilter = filter;
      return this;
   }

   private void scanMember(CtBehavior member, List<Entry<String, String>> entries) throws CannotCompileException {
      final String key = member.getDeclaringClass().getName() + "." + member.getMethodInfo().getName() + "(" + parameterNames(member.getMethodInfo()) + ")";
      member.instrument(new ExprEditor() {
         public void edit(NewExpr e) {
            try {
               MemberUsageScanner.this.add(entries, e.getConstructor().getDeclaringClass().getName() + ".<init>(" + MemberUsageScanner.parameterNames(e.getConstructor().getMethodInfo()) + ")", key + " #" + e.getLineNumber());
            } catch (NotFoundException var3) {
               throw new ReflectionsException("Could not find new instance usage in " + key, var3);
            }
         }

         public void edit(MethodCall m) {
            try {
               MemberUsageScanner.this.add(entries, m.getMethod().getDeclaringClass().getName() + "." + m.getMethodName() + "(" + MemberUsageScanner.parameterNames(m.getMethod().getMethodInfo()) + ")", key + " #" + m.getLineNumber());
            } catch (NotFoundException var3) {
               throw new ReflectionsException("Could not find member " + m.getClassName() + " in " + key, var3);
            }
         }

         public void edit(ConstructorCall c) {
            try {
               MemberUsageScanner.this.add(entries, c.getConstructor().getDeclaringClass().getName() + ".<init>(" + MemberUsageScanner.parameterNames(c.getConstructor().getMethodInfo()) + ")", key + " #" + c.getLineNumber());
            } catch (NotFoundException var3) {
               throw new ReflectionsException("Could not find member " + c.getClassName() + " in " + key, var3);
            }
         }

         public void edit(FieldAccess f) {
            try {
               MemberUsageScanner.this.add(entries, f.getField().getDeclaringClass().getName() + "." + f.getFieldName(), key + " #" + f.getLineNumber());
            } catch (NotFoundException var3) {
               throw new ReflectionsException("Could not find member " + f.getFieldName() + " in " + key, var3);
            }
         }
      });
   }

   private void add(List<Entry<String, String>> entries, String key, String value) {
      if (this.resultFilter.test(key)) {
         entries.add(this.entry(key, value));
      }

   }

   public static String parameterNames(MethodInfo info) {
      return String.join(", ", JavassistHelper.getParameters(info));
   }

   private ClassPool getClassPool() {
      if (this.classPool == null) {
         synchronized(this) {
            if (this.classPool == null) {
               this.classPool = new ClassPool();
               ClassLoader[] var2 = this.classLoaders;
               int var3 = var2.length;

               for(int var4 = 0; var4 < var3; ++var4) {
                  ClassLoader classLoader = var2[var4];
                  this.classPool.appendClassPath((ClassPath)(new LoaderClassPath(classLoader)));
               }
            }
         }
      }

      return this.classPool;
   }
}

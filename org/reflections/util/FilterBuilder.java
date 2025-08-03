package org.reflections.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.reflections.ReflectionsException;

public class FilterBuilder implements Predicate<String> {
   private final List<Predicate<String>> chain = new ArrayList();

   public FilterBuilder() {
   }

   private FilterBuilder(Collection<Predicate<String>> filters) {
      this.chain.addAll(filters);
   }

   public FilterBuilder includePackage(String value) {
      return this.includePattern(prefixPattern(value));
   }

   public FilterBuilder excludePackage(String value) {
      return this.excludePattern(prefixPattern(value));
   }

   public FilterBuilder includePattern(String regex) {
      return this.add(new FilterBuilder.Include(regex));
   }

   public FilterBuilder excludePattern(String regex) {
      return this.add(new FilterBuilder.Exclude(regex));
   }

   /** @deprecated */
   @Deprecated
   public FilterBuilder include(String regex) {
      return this.add(new FilterBuilder.Include(regex));
   }

   /** @deprecated */
   @Deprecated
   public FilterBuilder exclude(String regex) {
      this.add(new FilterBuilder.Exclude(regex));
      return this;
   }

   public static FilterBuilder parsePackages(String includeExcludeString) {
      List<Predicate<String>> filters = new ArrayList();
      String[] var2 = includeExcludeString.split(",");
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String string = var2[var4];
         String trimmed = string.trim();
         char prefix = trimmed.charAt(0);
         String pattern = prefixPattern(trimmed.substring(1));
         switch(prefix) {
         case '+':
            filters.add(new FilterBuilder.Include(pattern));
            break;
         case '-':
            filters.add(new FilterBuilder.Exclude(pattern));
            break;
         default:
            throw new ReflectionsException("includeExclude should start with either + or -");
         }
      }

      return new FilterBuilder(filters);
   }

   public FilterBuilder add(Predicate<String> filter) {
      this.chain.add(filter);
      return this;
   }

   public boolean test(String regex) {
      boolean accept = this.chain.isEmpty() || this.chain.get(0) instanceof FilterBuilder.Exclude;
      Iterator var3 = this.chain.iterator();

      while(var3.hasNext()) {
         Predicate<String> filter = (Predicate)var3.next();
         if ((!accept || !(filter instanceof FilterBuilder.Include)) && (accept || !(filter instanceof FilterBuilder.Exclude))) {
            accept = filter.test(regex);
            if (!accept && filter instanceof FilterBuilder.Exclude) {
               break;
            }
         }
      }

      return accept;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return o != null && this.getClass() == o.getClass() ? Objects.equals(this.chain, ((FilterBuilder)o).chain) : false;
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.chain});
   }

   public String toString() {
      return (String)this.chain.stream().map(Object::toString).collect(Collectors.joining(", "));
   }

   private static String prefixPattern(String fqn) {
      if (!fqn.endsWith(".")) {
         fqn = fqn + ".";
      }

      return fqn.replace(".", "\\.").replace("$", "\\$") + ".*";
   }

   static class Exclude extends FilterBuilder.Matcher {
      Exclude(String regex) {
         super(regex);
      }

      public boolean test(String regex) {
         return !this.pattern.matcher(regex).matches();
      }

      public String toString() {
         return "-" + this.pattern;
      }
   }

   static class Include extends FilterBuilder.Matcher {
      Include(String regex) {
         super(regex);
      }

      public boolean test(String regex) {
         return this.pattern.matcher(regex).matches();
      }

      public String toString() {
         return "+" + this.pattern;
      }
   }

   abstract static class Matcher implements Predicate<String> {
      final Pattern pattern;

      Matcher(String regex) {
         this.pattern = Pattern.compile(regex);
      }

      public int hashCode() {
         return Objects.hash(new Object[]{this.pattern});
      }

      public boolean equals(Object o) {
         return this == o || o != null && this.getClass() == o.getClass() && Objects.equals(this.pattern.pattern(), ((FilterBuilder.Matcher)o).pattern.pattern());
      }

      public String toString() {
         return this.pattern.pattern();
      }
   }
}

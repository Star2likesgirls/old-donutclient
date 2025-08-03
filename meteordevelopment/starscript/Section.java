package meteordevelopment.starscript;

public class Section {
   private static final ThreadLocal<StringBuilder> SB = ThreadLocal.withInitial(StringBuilder::new);
   public final int index;
   public final String text;
   public Section next;

   public Section(int index, String text) {
      this.index = index;
      this.text = text;
   }

   public String toString() {
      StringBuilder sb = (StringBuilder)SB.get();
      sb.setLength(0);

      for(Section s = this; s != null; s = s.next) {
         sb.append(s.text);
      }

      return sb.toString();
   }
}

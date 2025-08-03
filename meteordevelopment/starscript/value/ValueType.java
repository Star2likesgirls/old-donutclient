package meteordevelopment.starscript.value;

public enum ValueType {
   Null,
   Boolean,
   Number,
   String,
   Function,
   Map;

   // $FF: synthetic method
   private static ValueType[] $values() {
      return new ValueType[]{Null, Boolean, Number, String, Function, Map};
   }
}

package meteordevelopment.starscript;

public enum Instruction {
   Constant,
   Null,
   True,
   False,
   Add,
   Subtract,
   Multiply,
   Divide,
   Modulo,
   Power,
   AddConstant,
   Pop,
   Not,
   Negate,
   Equals,
   NotEquals,
   Greater,
   GreaterEqual,
   Less,
   LessEqual,
   Variable,
   Get,
   Call,
   Jump,
   JumpIfTrue,
   JumpIfFalse,
   Section,
   Append,
   ConstantAppend,
   VariableAppend,
   GetAppend,
   CallAppend,
   VariableGet,
   VariableGetAppend,
   End;

   private static final Instruction[] values = values();

   public static Instruction valueOf(int i) {
      return values[i];
   }

   // $FF: synthetic method
   private static Instruction[] $values() {
      return new Instruction[]{Constant, Null, True, False, Add, Subtract, Multiply, Divide, Modulo, Power, AddConstant, Pop, Not, Negate, Equals, NotEquals, Greater, GreaterEqual, Less, LessEqual, Variable, Get, Call, Jump, JumpIfTrue, JumpIfFalse, Section, Append, ConstantAppend, VariableAppend, GetAppend, CallAppend, VariableGet, VariableGetAppend, End};
   }
}

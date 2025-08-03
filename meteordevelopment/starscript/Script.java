package meteordevelopment.starscript;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.starscript.value.Value;

public class Script {
   public byte[] code = new byte[8];
   private int size;
   public final List<Value> constants = new ArrayList();

   private void write(int b) {
      if (this.size >= this.code.length) {
         byte[] newCode = new byte[this.code.length * 2];
         System.arraycopy(this.code, 0, newCode, 0, this.code.length);
         this.code = newCode;
      }

      this.code[this.size++] = (byte)b;
   }

   public void write(Instruction insn) {
      this.write(insn.ordinal());
   }

   public void write(Instruction insn, int b) {
      this.write(insn.ordinal());
      this.write(b);
   }

   public void write(Instruction insn, Value constant) {
      this.write(insn.ordinal());
      this.writeConstant(constant);
   }

   public void writeConstant(Value constant) {
      int constantI = -1;

      for(int i = 0; i < this.constants.size(); ++i) {
         if (((Value)this.constants.get(i)).equals(constant)) {
            constantI = i;
            break;
         }
      }

      if (constantI == -1) {
         constantI = this.constants.size();
         this.constants.add(constant);
      }

      this.write(constantI);
   }

   public int writeJump(Instruction insn) {
      this.write(insn);
      this.write(0);
      this.write(0);
      return this.size - 2;
   }

   public void patchJump(int offset) {
      int jump = this.size - offset - 2;
      this.code[offset] = (byte)(jump >> 8 & 255);
      this.code[offset + 1] = (byte)(jump & 255);
   }

   public void decompile() {
      for(int i = 0; i < this.size; ++i) {
         Instruction insn = Instruction.valueOf(this.code[i]);
         System.out.format("%3d %-18s", i, insn);
         switch(insn) {
         case AddConstant:
         case Variable:
         case VariableAppend:
         case Get:
         case GetAppend:
         case Constant:
         case ConstantAppend:
            ++i;
            System.out.format("%3d '%s'", this.code[i], this.constants.get(this.code[i]));
            break;
         case Call:
         case CallAppend:
            ++i;
            System.out.format("%3d %s", this.code[i], this.code[i] == 1 ? "argument" : "arguments");
            break;
         case Jump:
         case JumpIfTrue:
         case JumpIfFalse:
            i += 2;
            System.out.format("%3d -> %d", i - 2, i + 1 + (this.code[i - 1] << 8 & 255 | this.code[i] & 255));
            break;
         case Section:
            ++i;
            System.out.format("%3d", this.code[i]);
            break;
         case VariableGet:
         case VariableGetAppend:
            i += 2;
            System.out.format("%3d.%-3d '%s.%s'", this.code[i - 1], this.code[i], this.constants.get(this.code[i - 1]), this.constants.get(this.code[i]));
         }

         System.out.println();
      }

   }
}

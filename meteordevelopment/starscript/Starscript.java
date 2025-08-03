package meteordevelopment.starscript;

import java.util.Iterator;
import java.util.function.Supplier;
import meteordevelopment.starscript.compiler.Expr;
import meteordevelopment.starscript.compiler.Parser;
import meteordevelopment.starscript.utils.CompletionCallback;
import meteordevelopment.starscript.utils.Error;
import meteordevelopment.starscript.utils.SFunction;
import meteordevelopment.starscript.utils.Stack;
import meteordevelopment.starscript.utils.StarscriptError;
import meteordevelopment.starscript.value.Value;
import meteordevelopment.starscript.value.ValueMap;

public class Starscript {
   private final ValueMap globals = new ValueMap();
   private final Stack<Value> stack = new Stack();

   public Section run(Script script, StringBuilder sb) {
      this.stack.clear();
      sb.setLength(0);
      int ip = 0;
      Section firstSection = null;
      Section section = null;
      byte index = 0;

      while(true) {
         Value b;
         String name;
         Supplier s;
         byte argCount;
         String name;
         int jump;
         Value a;
         Value r;
         switch(Instruction.valueOf(script.code[ip++])) {
         case Constant:
            this.push((Value)script.constants.get(script.code[ip++]));
            break;
         case Null:
            this.push(Value.null_());
            break;
         case True:
            this.push(Value.bool(true));
            break;
         case False:
            this.push(Value.bool(false));
            break;
         case Add:
            b = this.pop();
            a = this.pop();
            if (a.isNumber() && b.isNumber()) {
               this.push(Value.number(a.getNumber() + b.getNumber()));
            } else if (a.isString()) {
               this.push(Value.string(a.getString() + b.toString()));
            } else {
               this.error("Can only add 2 numbers or 1 string and other value.");
            }
            break;
         case Subtract:
            b = this.pop();
            a = this.pop();
            if (a.isNumber() && b.isNumber()) {
               this.push(Value.number(a.getNumber() - b.getNumber()));
               break;
            }

            this.error("Can only subtract 2 numbers.");
            break;
         case Multiply:
            b = this.pop();
            a = this.pop();
            if (a.isNumber() && b.isNumber()) {
               this.push(Value.number(a.getNumber() * b.getNumber()));
               break;
            }

            this.error("Can only multiply 2 numbers.");
            break;
         case Divide:
            b = this.pop();
            a = this.pop();
            if (a.isNumber() && b.isNumber()) {
               this.push(Value.number(a.getNumber() / b.getNumber()));
               break;
            }

            this.error("Can only divide 2 numbers.");
            break;
         case Modulo:
            b = this.pop();
            a = this.pop();
            if (a.isNumber() && b.isNumber()) {
               this.push(Value.number(a.getNumber() % b.getNumber()));
               break;
            }

            this.error("Can only modulo 2 numbers.");
            break;
         case Power:
            b = this.pop();
            a = this.pop();
            if (a.isNumber() && b.isNumber()) {
               this.push(Value.number(Math.pow(a.getNumber(), b.getNumber())));
               break;
            }

            this.error("Can only power 2 numbers.");
            break;
         case AddConstant:
            b = (Value)script.constants.get(script.code[ip++]);
            a = this.pop();
            if (a.isNumber() && b.isNumber()) {
               this.push(Value.number(a.getNumber() + b.getNumber()));
            } else if (a.isString()) {
               this.push(Value.string(a.getString() + b.toString()));
            } else {
               this.error("Can only add 2 numbers or 1 string and other value.");
            }
            break;
         case Pop:
            this.pop();
            break;
         case Not:
            this.push(Value.bool(!this.pop().isTruthy()));
            break;
         case Negate:
            b = this.pop();
            if (b.isNumber()) {
               this.push(Value.number(-b.getNumber()));
            } else {
               this.error("This operation requires a number.");
            }
            break;
         case Equals:
            this.push(Value.bool(this.pop().equals(this.pop())));
            break;
         case NotEquals:
            this.push(Value.bool(!this.pop().equals(this.pop())));
            break;
         case Greater:
            b = this.pop();
            a = this.pop();
            if (a.isNumber() && b.isNumber()) {
               this.push(Value.bool(a.getNumber() > b.getNumber()));
               break;
            }

            this.error("This operation requires 2 number.");
            break;
         case GreaterEqual:
            b = this.pop();
            a = this.pop();
            if (a.isNumber() && b.isNumber()) {
               this.push(Value.bool(a.getNumber() >= b.getNumber()));
               break;
            }

            this.error("This operation requires 2 number.");
            break;
         case Less:
            b = this.pop();
            a = this.pop();
            if (a.isNumber() && b.isNumber()) {
               this.push(Value.bool(a.getNumber() < b.getNumber()));
               break;
            }

            this.error("This operation requires 2 number.");
            break;
         case LessEqual:
            b = this.pop();
            a = this.pop();
            if (a.isNumber() && b.isNumber()) {
               this.push(Value.bool(a.getNumber() <= b.getNumber()));
               break;
            }

            this.error("This operation requires 2 number.");
            break;
         case Variable:
            name = ((Value)script.constants.get(script.code[ip++])).getString();
            Supplier<Value> s = this.globals.get(name);
            this.push(s != null ? (Value)s.get() : Value.null_());
            break;
         case Get:
            name = ((Value)script.constants.get(script.code[ip++])).getString();
            a = this.pop();
            if (!a.isMap()) {
               this.push(Value.null_());
            } else {
               s = a.getMap().get(name);
               this.push(s != null ? (Value)s.get() : Value.null_());
            }
            break;
         case Call:
            argCount = script.code[ip++];
            a = this.peek(argCount);
            if (a.isFunction()) {
               r = a.getFunction().run(this, argCount);
               this.pop();
               this.push(r);
            } else {
               this.error("Tried to call a %s, can only call functions.", a.type);
            }
            break;
         case Jump:
            jump = script.code[ip++] << 8 & 255 | script.code[ip++] & 255;
            ip += jump;
            break;
         case JumpIfTrue:
            jump = script.code[ip++] << 8 & 255 | script.code[ip++] & 255;
            if (this.peek().isTruthy()) {
               ip += jump;
            }
            break;
         case JumpIfFalse:
            jump = script.code[ip++] << 8 & 255 | script.code[ip++] & 255;
            if (!this.peek().isTruthy()) {
               ip += jump;
            }
            break;
         case Section:
            if (firstSection == null) {
               firstSection = new Section(index, sb.toString());
               section = firstSection;
            } else {
               section.next = new Section(index, sb.toString());
               section = section.next;
            }

            sb.setLength(0);
            index = script.code[ip++];
            break;
         case Append:
            sb.append(this.pop().toString());
            break;
         case ConstantAppend:
            sb.append(((Value)script.constants.get(script.code[ip++])).toString());
            break;
         case VariableAppend:
            Supplier<Value> s = this.globals.get(((Value)script.constants.get(script.code[ip++])).getString());
            sb.append((s == null ? Value.null_() : (Value)s.get()).toString());
            break;
         case GetAppend:
            name = ((Value)script.constants.get(script.code[ip++])).getString();
            a = this.pop();
            if (!a.isMap()) {
               sb.append(Value.null_());
            } else {
               s = a.getMap().get(name);
               sb.append((s != null ? (Value)s.get() : Value.null_()).toString());
            }
            break;
         case CallAppend:
            argCount = script.code[ip++];
            a = this.peek(argCount);
            if (a.isFunction()) {
               r = a.getFunction().run(this, argCount);
               this.pop();
               sb.append(r.toString());
            } else {
               this.error("Tried to call a %s, can only call functions.", a.type);
            }
            break;
         case VariableGet:
            name = ((Value)script.constants.get(script.code[ip++])).getString();
            s = this.globals.get(name);
            b = s != null ? (Value)s.get() : Value.null_();
            name = ((Value)script.constants.get(script.code[ip++])).getString();
            if (!b.isMap()) {
               this.push(Value.null_());
            } else {
               s = b.getMap().get(name);
               this.push(s != null ? (Value)s.get() : Value.null_());
            }
            break;
         case VariableGetAppend:
            name = ((Value)script.constants.get(script.code[ip++])).getString();
            s = this.globals.get(name);
            b = s != null ? (Value)s.get() : Value.null_();
            name = ((Value)script.constants.get(script.code[ip++])).getString();
            if (!b.isMap()) {
               this.push(Value.null_());
            } else {
               s = b.getMap().get(name);
               b = s != null ? (Value)s.get() : Value.null_();
               sb.append(b.toString());
            }
            break;
         case End:
            if (firstSection != null) {
               section.next = new Section(index, sb.toString());
               return firstSection;
            }

            return new Section(index, sb.toString());
         default:
            throw new UnsupportedOperationException("Unknown instruction '" + Instruction.valueOf(script.code[ip]) + "'");
         }
      }
   }

   public Section run(Script script) {
      return this.run(script, new StringBuilder());
   }

   public void push(Value value) {
      this.stack.push(value);
   }

   public Value pop() {
      return (Value)this.stack.pop();
   }

   public Value peek() {
      return (Value)this.stack.peek();
   }

   public Value peek(int offset) {
      return (Value)this.stack.peek(offset);
   }

   public boolean popBool(String errorMsg) {
      Value a = this.pop();
      if (!a.isBool()) {
         this.error(errorMsg);
      }

      return a.getBool();
   }

   public double popNumber(String errorMsg) {
      Value a = this.pop();
      if (!a.isNumber()) {
         this.error(errorMsg);
      }

      return a.getNumber();
   }

   public String popString(String errorMsg) {
      Value a = this.pop();
      if (!a.isString()) {
         this.error(errorMsg);
      }

      return a.getString();
   }

   public void error(String format, Object... args) {
      throw new StarscriptError(String.format(format, args));
   }

   public ValueMap set(String name, Supplier<Value> supplier) {
      return this.globals.set(name, supplier);
   }

   public ValueMap set(String name, Value value) {
      return this.globals.set(name, value);
   }

   public ValueMap set(String name, boolean bool) {
      return this.globals.set(name, bool);
   }

   public ValueMap set(String name, double number) {
      return this.globals.set(name, number);
   }

   public ValueMap set(String name, String string) {
      return this.globals.set(name, string);
   }

   public ValueMap set(String name, SFunction function) {
      return this.globals.set(name, function);
   }

   public ValueMap set(String name, ValueMap map) {
      return this.globals.set(name, map);
   }

   public ValueMap getGlobals() {
      return this.globals;
   }

   public void getCompletions(String source, int position, CompletionCallback callback) {
      Parser.Result result = Parser.parse(source);
      Iterator var5 = result.exprs.iterator();

      while(var5.hasNext()) {
         Expr expr = (Expr)var5.next();
         this.completionsExpr(source, position, expr, callback);
      }

      var5 = result.errors.iterator();

      while(var5.hasNext()) {
         Error error = (Error)var5.next();
         if (error.expr != null) {
            this.completionsExpr(source, position, error.expr, callback);
         }
      }

   }

   private void completionsExpr(String source, int position, Expr expr, CompletionCallback callback) {
      if (position >= expr.start && (position <= expr.end || position == source.length())) {
         String key;
         if (expr instanceof Expr.Variable) {
            Expr.Variable var = (Expr.Variable)expr;
            key = source.substring(var.start, position);
            Iterator var7 = this.globals.keys().iterator();

            while(var7.hasNext()) {
               String key = (String)var7.next();
               if (!key.startsWith("_") && key.startsWith(key)) {
                  callback.onCompletion(key, ((Value)this.globals.get(key).get()).isFunction());
               }
            }
         } else if (expr instanceof Expr.Get) {
            Expr.Get get = (Expr.Get)expr;
            if (position >= get.end - get.name.length()) {
               Value value = this.resolveExpr(get.object);
               if (value != null && value.isMap()) {
                  String start = source.substring(get.object.end + 1, position);
                  Iterator var14 = value.getMap().keys().iterator();

                  while(var14.hasNext()) {
                     String key = (String)var14.next();
                     if (!key.startsWith("_") && key.startsWith(start)) {
                        callback.onCompletion(key, ((Value)value.getMap().get(key).get()).isFunction());
                     }
                  }
               }
            } else {
               expr.forEach((child) -> {
                  this.completionsExpr(source, position, child, callback);
               });
            }
         } else if (expr instanceof Expr.Block) {
            if (((Expr.Block)expr).expr == null) {
               Iterator var11 = this.globals.keys().iterator();

               while(var11.hasNext()) {
                  key = (String)var11.next();
                  if (!key.startsWith("_")) {
                     callback.onCompletion(key, ((Value)this.globals.get(key).get()).isFunction());
                  }
               }
            } else {
               expr.forEach((child) -> {
                  this.completionsExpr(source, position, child, callback);
               });
            }
         } else {
            expr.forEach((child) -> {
               this.completionsExpr(source, position, child, callback);
            });
         }

      }
   }

   private Value resolveExpr(Expr expr) {
      if (expr instanceof Expr.Variable) {
         Supplier<Value> supplier = this.globals.get(((Expr.Variable)expr).name);
         return supplier != null ? (Value)supplier.get() : null;
      } else if (expr instanceof Expr.Get) {
         Value value = this.resolveExpr(((Expr.Get)expr).object);
         if (value != null && value.isMap()) {
            Supplier<Value> supplier = value.getMap().get(((Expr.Get)expr).name);
            return supplier != null ? (Value)supplier.get() : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}

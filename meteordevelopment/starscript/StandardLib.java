package meteordevelopment.starscript;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import meteordevelopment.starscript.value.Value;

public class StandardLib {
   private static final Random rand = new Random();
   public static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
   public static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MM. yyyy");

   public static void init(Starscript ss) {
      ss.set("PI", 3.141592653589793D);
      ss.set("time", () -> {
         return Value.string(timeFormat.format(new Date()));
      });
      ss.set("date", () -> {
         return Value.string(dateFormat.format(new Date()));
      });
      ss.set("round", StandardLib::round);
      ss.set("roundToString", StandardLib::roundToString);
      ss.set("floor", StandardLib::floor);
      ss.set("ceil", StandardLib::ceil);
      ss.set("abs", StandardLib::abs);
      ss.set("random", StandardLib::random);
      ss.set("string", StandardLib::string);
      ss.set("toUpper", StandardLib::toUpper);
      ss.set("toLower", StandardLib::toLower);
      ss.set("contains", StandardLib::contains);
      ss.set("replace", StandardLib::replace);
      ss.set("pad", StandardLib::pad);
   }

   public static Value round(Starscript ss, int argCount) {
      double b;
      if (argCount == 1) {
         b = ss.popNumber("Argument to round() needs to be a number.");
         return Value.number((double)Math.round(b));
      } else if (argCount == 2) {
         b = ss.popNumber("Second argument to round() needs to be a number.");
         double a = ss.popNumber("First argument to round() needs to be a number.");
         double x = Math.pow(10.0D, (double)((int)b));
         return Value.number((double)Math.round(a * x) / x);
      } else {
         ss.error("round() requires 1 or 2 arguments, got %d.", argCount);
         return null;
      }
   }

   public static Value roundToString(Starscript ss, int argCount) {
      double b;
      if (argCount == 1) {
         b = ss.popNumber("Argument to round() needs to be a number.");
         return Value.string(Double.toString((double)Math.round(b)));
      } else if (argCount == 2) {
         b = ss.popNumber("Second argument to round() needs to be a number.");
         double a = ss.popNumber("First argument to round() needs to be a number.");
         double x = Math.pow(10.0D, (double)((int)b));
         return Value.string(Double.toString((double)Math.round(a * x) / x));
      } else {
         ss.error("round() requires 1 or 2 arguments, got %d.", argCount);
         return null;
      }
   }

   public static Value floor(Starscript ss, int argCount) {
      if (argCount != 1) {
         ss.error("floor() requires 1 argument, got %d.", argCount);
      }

      double a = ss.popNumber("Argument to floor() needs to be a number.");
      return Value.number(Math.floor(a));
   }

   public static Value ceil(Starscript ss, int argCount) {
      if (argCount != 1) {
         ss.error("ceil() requires 1 argument, got %d.", argCount);
      }

      double a = ss.popNumber("Argument to ceil() needs to be a number.");
      return Value.number(Math.ceil(a));
   }

   public static Value abs(Starscript ss, int argCount) {
      if (argCount != 1) {
         ss.error("abs() requires 1 argument, got %d.", argCount);
      }

      double a = ss.popNumber("Argument to abs() needs to be a number.");
      return Value.number(Math.abs(a));
   }

   public static Value random(Starscript ss, int argCount) {
      if (argCount == 0) {
         return Value.number(rand.nextDouble());
      } else if (argCount == 2) {
         double max = ss.popNumber("Second argument to random() needs to be a number.");
         double min = ss.popNumber("First argument to random() needs to be a number.");
         return Value.number(min + (max - min) * rand.nextDouble());
      } else {
         ss.error("random() requires 0 or 2 arguments, got %d.", argCount);
         return Value.null_();
      }
   }

   private static Value string(Starscript ss, int argCount) {
      if (argCount != 1) {
         ss.error("string() requires 1 argument, got %d.", argCount);
      }

      return Value.string(ss.pop().toString());
   }

   public static Value toUpper(Starscript ss, int argCount) {
      if (argCount != 1) {
         ss.error("toUpper() requires 1 argument, got %d.", argCount);
      }

      String a = ss.popString("Argument to toUpper() needs to be a string.");
      return Value.string(a.toUpperCase());
   }

   public static Value toLower(Starscript ss, int argCount) {
      if (argCount != 1) {
         ss.error("toLower() requires 1 argument, got %d.", argCount);
      }

      String a = ss.popString("Argument to toLower() needs to be a string.");
      return Value.string(a.toLowerCase());
   }

   public static Value contains(Starscript ss, int argCount) {
      if (argCount != 2) {
         ss.error("replace() requires 2 arguments, got %d.", argCount);
      }

      String search = ss.popString("Second argument to contains() needs to be a string.");
      String string = ss.popString("First argument to contains() needs to be a string.");
      return Value.bool(string.contains(search));
   }

   public static Value replace(Starscript ss, int argCount) {
      if (argCount != 3) {
         ss.error("replace() requires 3 arguments, got %d.", argCount);
      }

      String to = ss.popString("Third argument to replace() needs to be a string.");
      String from = ss.popString("Second argument to replace() needs to be a string.");
      String string = ss.popString("First argument to replace() needs to be a string.");
      return Value.string(string.replace(from, to));
   }

   public static Value pad(Starscript ss, int argCount) {
      if (argCount != 2) {
         ss.error("pad() requires 2 arguments, got %d.", argCount);
      }

      int width = (int)ss.popNumber("Second argument to pad() needs to be a number.");
      String text = ss.pop().toString();
      if (text.length() >= Math.abs(width)) {
         return Value.string(text);
      } else {
         char[] padded = new char[Math.max(text.length(), Math.abs(width))];
         int padLength;
         if (width >= 0) {
            padLength = width - text.length();

            int i;
            for(i = 0; i < padLength; ++i) {
               padded[i] = ' ';
            }

            for(i = 0; i < text.length(); ++i) {
               padded[padLength + i] = text.charAt(i);
            }
         } else {
            for(padLength = 0; padLength < text.length(); ++padLength) {
               padded[padLength] = text.charAt(padLength);
            }

            for(padLength = 0; padLength < Math.abs(width) - text.length(); ++padLength) {
               padded[text.length() + padLength] = ' ';
            }
         }

         return Value.string(new String(padded));
      }
   }
}

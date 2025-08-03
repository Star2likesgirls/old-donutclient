package meteordevelopment.starscript.compiler;

public enum Token {
   String,
   Identifier,
   Number,
   Null,
   True,
   False,
   And,
   Or,
   EqualEqual,
   BangEqual,
   Greater,
   GreaterEqual,
   Less,
   LessEqual,
   Plus,
   Minus,
   Star,
   Slash,
   Percentage,
   UpArrow,
   Bang,
   Dot,
   Comma,
   QuestionMark,
   Colon,
   LeftParen,
   RightParen,
   LeftBrace,
   RightBrace,
   Section,
   Error,
   EOF;

   // $FF: synthetic method
   private static Token[] $values() {
      return new Token[]{String, Identifier, Number, Null, True, False, And, Or, EqualEqual, BangEqual, Greater, GreaterEqual, Less, LessEqual, Plus, Minus, Star, Slash, Percentage, UpArrow, Bang, Dot, Comma, QuestionMark, Colon, LeftParen, RightParen, LeftBrace, RightBrace, Section, Error, EOF};
   }
}

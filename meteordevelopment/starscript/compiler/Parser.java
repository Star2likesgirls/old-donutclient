package meteordevelopment.starscript.compiler;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.starscript.utils.Error;

public class Parser {
   private final Lexer lexer;
   private final Parser.TokenData previous = new Parser.TokenData();
   private final Parser.TokenData current = new Parser.TokenData();
   private int expressionDepth;

   private Parser(String source) {
      this.lexer = new Lexer(source);
   }

   private Parser.Result parse_() {
      Parser.Result result = new Parser.Result();
      this.advance();

      while(!this.isAtEnd()) {
         try {
            result.exprs.add(this.statement());
         } catch (Parser.ParseException var3) {
            result.errors.add(var3.error);
            this.synchronize();
         }
      }

      return result;
   }

   public static Parser.Result parse(String source) {
      return (new Parser(source)).parse_();
   }

   private Expr statement() {
      if (this.match(Token.Section)) {
         if (this.previous.lexeme.isEmpty()) {
            this.error("Expected section index.", (Expr)null);
         }

         int start = this.previous.start;
         int index = Integer.parseInt(this.previous.lexeme);
         Expr expr = this.expression();
         Expr expr = new Expr.Section(start, this.previous.end, index, expr);
         if (index > 255) {
            this.error("Section index cannot be larger than 255.", expr);
         }

         return expr;
      } else {
         return this.expression();
      }
   }

   private Expr expression() {
      return this.conditional();
   }

   private Expr conditional() {
      int start = this.previous.start;
      Expr expr = this.and();
      if (this.match(Token.QuestionMark)) {
         Expr trueExpr = this.statement();
         this.consume(Token.Colon, "Expected ':' after first part of condition.", (Expr)expr);
         Expr falseExpr = this.statement();
         expr = new Expr.Conditional(start, this.previous.end, (Expr)expr, trueExpr, falseExpr);
      }

      return (Expr)expr;
   }

   private Expr and() {
      Object expr;
      int start;
      Expr right;
      for(expr = this.or(); this.match(Token.And); expr = new Expr.Logical(start, this.previous.end, (Expr)expr, Token.And, right)) {
         start = this.previous.start;
         right = this.or();
      }

      return (Expr)expr;
   }

   private Expr or() {
      Object expr;
      int start;
      Expr right;
      for(expr = this.equality(); this.match(Token.Or); expr = new Expr.Logical(start, this.previous.end, (Expr)expr, Token.Or, right)) {
         start = this.previous.start;
         right = this.equality();
      }

      return (Expr)expr;
   }

   private Expr equality() {
      int start = this.previous.start;

      Object expr;
      Token op;
      Expr right;
      for(expr = this.comparison(); this.match(Token.EqualEqual, Token.BangEqual); expr = new Expr.Binary(start, this.previous.end, (Expr)expr, op, right)) {
         op = this.previous.token;
         right = this.comparison();
      }

      return (Expr)expr;
   }

   private Expr comparison() {
      int start = this.previous.start;

      Object expr;
      Token op;
      Expr right;
      for(expr = this.term(); this.match(Token.Greater, Token.GreaterEqual, Token.Less, Token.LessEqual); expr = new Expr.Binary(start, this.previous.end, (Expr)expr, op, right)) {
         op = this.previous.token;
         right = this.term();
      }

      return (Expr)expr;
   }

   private Expr term() {
      int start = this.previous.start;

      Object expr;
      Token op;
      Expr right;
      for(expr = this.factor(); this.match(Token.Plus, Token.Minus); expr = new Expr.Binary(start, this.previous.end, (Expr)expr, op, right)) {
         op = this.previous.token;
         right = this.factor();
      }

      return (Expr)expr;
   }

   private Expr factor() {
      int start = this.previous.start;

      Object expr;
      Token op;
      Expr right;
      for(expr = this.unary(); this.match(Token.Star, Token.Slash, Token.Percentage, Token.UpArrow); expr = new Expr.Binary(start, this.previous.end, (Expr)expr, op, right)) {
         op = this.previous.token;
         right = this.unary();
      }

      return (Expr)expr;
   }

   private Expr unary() {
      if (this.match(Token.Bang, Token.Minus)) {
         int start = this.previous.start;
         Token op = this.previous.token;
         Expr right = this.unary();
         return new Expr.Unary(start, this.previous.end, op, right);
      } else {
         return this.call();
      }
   }

   private Expr call() {
      Expr expr = this.primary();
      int start = this.previous.start;

      while(true) {
         while(!this.match(Token.LeftParen)) {
            if (!this.match(Token.Dot)) {
               return (Expr)expr;
            }

            if (!this.check(Token.Identifier)) {
               expr = new Expr.Get(start, this.current.end, (Expr)expr, "");
            }

            Parser.TokenData name = this.consume(Token.Identifier, "Expected field name after '.'.", (Expr)expr);
            expr = new Expr.Get(start, this.previous.end, (Expr)expr, name.lexeme);
         }

         expr = this.finishCall((Expr)expr);
      }
   }

   private Expr finishCall(Expr callee) {
      List<Expr> args = new ArrayList(2);
      if (!this.check(Token.RightParen)) {
         do {
            args.add(this.expression());
         } while(this.match(Token.Comma));
      }

      Expr expr = new Expr.Call(callee.start, this.previous.end, callee, args);
      this.consume(Token.RightParen, "Expected ')' after function arguments.", expr);
      return expr;
   }

   private Expr primary() {
      if (this.match(Token.Null)) {
         return new Expr.Null(this.previous.start, this.previous.end);
      } else if (this.match(Token.String)) {
         return new Expr.String(this.previous.start, this.previous.end, this.previous.lexeme);
      } else if (this.match(Token.True, Token.False)) {
         return new Expr.Bool(this.previous.start, this.previous.end, this.previous.lexeme.equals("true"));
      } else if (this.match(Token.Number)) {
         return new Expr.Number(this.previous.start, this.previous.end, Double.parseDouble(this.previous.lexeme));
      } else if (this.match(Token.Identifier)) {
         return new Expr.Variable(this.previous.start, this.previous.end, this.previous.lexeme);
      } else {
         int start;
         Expr expr;
         if (this.match(Token.LeftParen)) {
            start = this.previous.start;
            expr = this.statement();
            Expr expr = new Expr.Group(start, this.previous.end, expr);
            this.consume(Token.RightParen, "Expected ')' after expression.", expr);
            return expr;
         } else if (this.expressionDepth == 0 && this.match(Token.LeftBrace)) {
            start = this.previous.start;
            ++this.expressionDepth;

            try {
               expr = this.statement();
            } catch (Parser.ParseException var4) {
               if (var4.error.expr == null) {
                  var4.error.expr = new Expr.Block(start, this.previous.end, (Expr)null);
               }

               throw var4;
            }

            Expr expr = new Expr.Block(start, this.previous.end, expr);
            this.consume(Token.RightBrace, "Expected '}' after expression.", expr);
            --this.expressionDepth;
            return expr;
         } else {
            this.error("Expected expression.", (Expr)null);
            return null;
         }
      }
   }

   private void synchronize() {
      while(!this.isAtEnd()) {
         if (this.match(Token.LeftBrace)) {
            ++this.expressionDepth;
         } else if (this.match(Token.RightBrace)) {
            --this.expressionDepth;
            if (this.expressionDepth == 0) {
               return;
            }
         } else {
            this.advance();
         }
      }

   }

   private void error(String message, Expr expr) {
      throw new Parser.ParseException(new Error(this.current.line, this.current.character, this.current.ch, message, expr));
   }

   private Parser.TokenData consume(Token token, String message, Expr expr) {
      if (this.check(token)) {
         return this.advance();
      } else {
         this.error(message, expr);
         return null;
      }
   }

   private boolean match(Token... tokens) {
      Token[] var2 = tokens;
      int var3 = tokens.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Token token = var2[var4];
         if (this.check(token)) {
            this.advance();
            return true;
         }
      }

      return false;
   }

   private boolean check(Token token) {
      if (this.isAtEnd()) {
         return false;
      } else {
         return this.current.token == token;
      }
   }

   private Parser.TokenData advance() {
      this.previous.set(this.current);
      this.lexer.next();
      this.current.set(this.lexer.token, this.lexer.lexeme, this.lexer.start, this.lexer.current, this.lexer.line, this.lexer.character, this.lexer.ch);
      return this.previous;
   }

   private boolean isAtEnd() {
      return this.current.token == Token.EOF;
   }

   private static class TokenData {
      public Token token;
      public String lexeme;
      public int start;
      public int end;
      public int line;
      public int character;
      public char ch;

      private TokenData() {
      }

      public void set(Token token, String lexeme, int start, int end, int line, int character, char ch) {
         this.token = token;
         this.lexeme = lexeme;
         this.start = start;
         this.end = end;
         this.line = line;
         this.character = character;
         this.ch = ch;
      }

      public void set(Parser.TokenData data) {
         this.set(data.token, data.lexeme, data.start, data.end, data.line, data.character, data.ch);
      }

      public String toString() {
         return String.format("%s '%s'", this.token, this.lexeme);
      }

      // $FF: synthetic method
      TokenData(Object x0) {
         this();
      }
   }

   public static class Result {
      public final List<Expr> exprs = new ArrayList();
      public final List<Error> errors = new ArrayList();

      public boolean hasErrors() {
         return this.errors.size() > 0;
      }
   }

   private static class ParseException extends RuntimeException {
      public final Error error;

      public ParseException(Error error) {
         this.error = error;
      }
   }
}

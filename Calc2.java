/*******************************************
 *File: Calc2.java                          *
 *Author: Tyler Besecker                    *
 *Purpose: Enhancing Dr.Sullivan's original *
 *calculator to handle Graphing, Variables, *
 *and Functions.                            *
 ********************************************/

import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

public class Calc2 extends JFrame {
    Graphics2D ctx;
    Canvas canvas;
    TextField function;
    //Points of the graph
    double x1 = 0.0; 
    double x2 = 0.0;
    double y1 = 0.0;
    double y2 = 0.0;
    //ArrayList of X & Y Points
    ArrayList<Double> xList = new ArrayList<Double>();
    ArrayList<Double> yList = new ArrayList<Double>();
    
    static enum TokenType {
	EOF, PLUS, MINUS, TIMES, DIV, LPAREN, RPAREN, CONSTANT, VARIABLE,SIN,COS, TAN, ATAN, SQRT, LOG, EXP,CAR,E,PI
    }
  
    static class Token {
	TokenType type;
	Object value;

	public Token(TokenType type, Object value) {
	    this.type = type;
	    this.value = value;
	}   

	public Token(TokenType type) {
	    this.type = type;
	    this.value = null;
	}

	public String toString() {
	    return "[" + type + (value == null ? "" : "," + value) + "]";
	}
    }
  
    static class SyntaxError extends Exception {
	public SyntaxError(String msg) {
	    super(msg);
	}
    }
  
    static class Lexer {
	String src;
	int next = 0;
    
	public Lexer(String src) {
	    this.src = src;
	}
    
	Token scanIntegerConstant() {
	    String lexeme  = "";
	    while (next < src.length() && Character.isDigit(src.charAt(next))) { 
		lexeme +=  src.charAt(next);
		next++;
	    }
	    return new Token(TokenType.CONSTANT, Double.parseDouble(lexeme)); 
	}


	void skipWhiteSpace() {
	    char c;
	    while (next < src.length() && Character.isWhitespace(src.charAt(next))) {
		next++;
	    }
	}

	public Token getToken() throws SyntaxError {
	    skipWhiteSpace();
	    if (next == src.length()) {
		return null;
	    }
	    if (Character.isDigit(src.charAt(next))) {
		return scanIntegerConstant();
	    }
	    //if the input is a character runs through it checking for accepted functions/variables. 
	    if(Character.isLetter(src.charAt(next))){
		String Function = "";
		int currentNext = next + 1;
		Function += src.charAt(next);
		//Ajani Charles helped me create this while loop. 
		while(currentNext < src.length() && Character.isLetter(src.charAt(currentNext))){ 
		    Function += src.charAt(currentNext);
		    currentNext++;
		}
		
		Function = Function.toLowerCase(); //makes sure all strings are lowercase to match cases. 
		switch(Function){ //Determines which function/variable/constant we are currently on. 
		case "sin": next+=3; return new Token(TokenType.SIN);
		case "cos": next+=3; return new Token(TokenType.COS);
		case "tan": next+=3; return new Token(TokenType.TAN);
		case "atan": next+=4; return new Token(TokenType.ATAN);
		case "sqrt": next+=4; return new Token(TokenType.SQRT);
		case "log": next+=3; return new Token(TokenType.LOG);
		case "exp": next+=3; return new Token(TokenType.EXP);
		case "x":next++; return new Token(TokenType.VARIABLE);
		case "e":next++; return new Token(TokenType.E);
		case "pi":next+=2; return new Token(TokenType.PI);
		case "π":next+=1; return new Token(TokenType.PI);
		default: throw new SyntaxError("invalid input: " + Function);
		}
		
	    }
	    switch (src.charAt(next)) {
	    case '+': next++; return new Token(TokenType.PLUS);
	    case '-': next++; return new Token(TokenType.MINUS);
	    case '*': next++; return new Token(TokenType.TIMES);
	    case '/': next++; return new Token(TokenType.DIV);
	    case '^': next++; return new Token(TokenType.CAR);
	    case '(': next++; return new Token(TokenType.LPAREN);
	    case ')': next++; return new Token(TokenType.RPAREN);
	    default:
		throw new SyntaxError("invalid character: " + src.charAt(next));
	    }
	}
    }
  
    static abstract class Expr {
	public abstract double eval(double x);
    }
    
    enum Operator {
	ADD, SUB, MUL, DIV,SIN,COS,TAN,EXP,LOG,ATAN,SQRT
    }

    static class VariableExpr extends Expr{
	public double eval(double x){
	    return x;
	}
    }
    
    static class EExpr extends Expr{
	public double eval(double x){
	    return Math.E;
	}
    }
    
    static class PiExpr extends Expr{
	public double eval(double x){
	    return Math.PI;
	}
    }
    
    static class ConstantExpr extends Expr {
	double value;

	public ConstantExpr(double value) {
	    this.value = value;
	}

     

	public double eval(double x) {
	    return value;
	}

	public String toString() {
	    return "[" + value + "]";
	}
    }

    static class BinaryExpr extends Expr {
	Operator op;
	Expr lrand;
	Expr rrand;

	public BinaryExpr(Operator op, Expr lrand, Expr rrand) {
	    this.op = op;
	    this.lrand = lrand;
	    this.rrand = rrand;
	}

	public double eval(double x) {
	    switch (op) {
	    case ADD: return lrand.eval(x) + rrand.eval(x);
	    case SUB: return lrand.eval(x) - rrand.eval(x);
	    case MUL: return lrand.eval(x) * rrand.eval(x);
	    case DIV: return lrand.eval(x) / rrand.eval(x);
	    case SIN: return Math.sin(rrand.eval(x));
	    case COS: return Math.cos(rrand.eval(x));
	    case TAN: return Math.tan(rrand.eval(x));
	    case ATAN: return Math.atan(rrand.eval(x));
	    case SQRT: return Math.sqrt(rrand.eval(x));
	    case LOG: return Math.log(rrand.eval(x));
	    case EXP: return Math.exp(rrand.eval(x));
	    
	    }
	    return 0;
	}
	
	public String toString() {
	    return "[" + lrand + " " + op + " " + rrand + "]";
	}
    }

    static class Parser {
	Lexer lexer;
	Token lookahead = null;
    
	public Parser(Lexer lexer) {
	    this.lexer = lexer;
	}

	Token next() throws SyntaxError {
	    if (lookahead == null) {
		lookahead = lexer.getToken();
	    }
	    return lookahead;
	}

	void discard() {
	    lookahead = null;
	}
    
	void match(TokenType t) throws SyntaxError {
	    if (next().type == t) discard();
	    else {
		throw new SyntaxError("Expecting " + t + " but found " + next());
	    }
	}

	// expr => add_expr
	Expr parseExpr() throws SyntaxError {
	    return parseAddExpr();
	}

	// add_expr => mul-expr ( ( '+' | '-' ) mul-expr ) *
	Expr parseAddExpr() throws SyntaxError {
	    Expr left = parseMulExpr();
	    while (next() != null && (next().type == TokenType.PLUS || next().type == TokenType.MINUS)) {
		if (next().type == TokenType.PLUS) {
		    discard();
		    left = new BinaryExpr(Operator.ADD, left, parseMulExpr());
		}
		else {
		    discard();
		    left = new BinaryExpr(Operator.SUB, left, parseMulExpr());
		}
	    }
	    return left;
	}
      
	// mul-expr => pri-expr ( ( '*' | '/' | '%' ) pri-expr ) *
	Expr parseMulExpr() throws SyntaxError {
	    Expr left = parsePriExpr();
	    while (next() != null && (next().type == TokenType.TIMES || next().type == TokenType.DIV)) {
		if (next().type == TokenType.TIMES) {
		    discard();
		    left = new BinaryExpr(Operator.MUL, left, parsePriExpr());
		}
		else {
		    discard();
		    left = new BinaryExpr(Operator.DIV, left, parsePriExpr());
		}
	    }
	    return left;
	}

	// pri-expr => ICONST
	// pri-expr => '(' expr ')'
	Expr parsePriExpr() throws SyntaxError {
	    Expr e;
	    switch (next().type) {
	    case CONSTANT:
		e = new ConstantExpr((Double) next().value);
		discard();
		return e; 
	    case LPAREN:
		discard();
		e = parseExpr();
		match(TokenType.RPAREN);
		return e;
	    case VARIABLE:
		e = new VariableExpr();
		discard();
		return e;
	    case E:
		e = new EExpr();
		discard();
		return e;
	    case PI:
		e = new PiExpr();
		discard();
		return e;
	    case SIN:
		discard();
		e = new BinaryExpr(Operator.SIN, null, parseExpr());
		return e;
	    case COS:
	        discard();
		e = new BinaryExpr(Operator.COS, null, parseExpr());
		return e;
	    case TAN:
	        discard();
		e = new BinaryExpr(Operator.TAN, null, parseExpr());
		return e;
	    case ATAN:
	        discard();
		e = new BinaryExpr(Operator.ATAN, null, parseExpr());
		return e;
	    case SQRT:
	        discard();
		e = new BinaryExpr(Operator.SQRT, null, parseExpr());
		return e;
	    case LOG:
	        discard();
		e = new BinaryExpr(Operator.LOG, null, parseExpr());
		return e;
	    case EXP:
	        discard();
		e = new BinaryExpr(Operator.EXP, null, parseExpr());
		return e;
	    default:
		throw new SyntaxError("Expecting CONSTANT or LEFT PAREN");
	    }
	}
    }

    class Canvas extends JPanel {
	Canvas(int width, int height) {
	    setPreferredSize(new Dimension(width, height));
	}
    
	public void paint(Graphics g) {
	    Graphics2D ctx = (Graphics2D) g;
	    ctx.translate(250, 250);
	    ctx.scale(50, -50);
	    ctx.setStroke(new BasicStroke(0.02f));
	    RenderingHints rh = new RenderingHints(null);
	    rh.put(RenderingHints.KEY_ANTIALIASING,
		   RenderingHints.VALUE_ANTIALIAS_ON);
	    rh.put(RenderingHints.KEY_TEXT_ANTIALIASING,
		   RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	    ctx.setRenderingHints(rh);
	    //Draws the Graph
	    ctx.draw(new Line2D.Double(-6, 0, 6, 0));
	    ctx.draw(new Line2D.Double(0, -6, 0, 6));
	    //Draws the tick marks
	    for(int j = -5; j < 6;j++) {
		ctx.draw(new Line2D.Double(j,.25,j,-.25));
		ctx.draw(new Line2D.Double(.25,j,-.25,j));
	    }
	    //draws the points 
	    for(int i = 0; i < 1001; i++){
		if(xList.isEmpty() == true) //Checks if the array is empty
		    return;
		else{ // otherwise it iterates through each point 
		    x1 = xList.get(i);
		    y1 = yList.get(i);
		    if (i == 0){
			x2 = x1;
			y2 = y1;
		    }
		    else{
			x2 = xList.get(i-1);
			y2 = yList.get(i-1);
		    }
		    ctx.draw(new Line2D.Double(x1,y1,x2,y2)); //draws the points
		}
	    
	    }
	}
    }
  
    public Calc2(){

	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	canvas = new Canvas(500, 500);
    
	add(canvas);

	//Adds a function textbox towards the top
	function = new TextField();
	add(function,BorderLayout.NORTH);

	//Adds a graph button towards the bottom.
	JButton button = new JButton("Graph");
	add(button, BorderLayout.SOUTH); 
      
	button.addActionListener(new ActionListener(){ //action listener
		public void actionPerformed(ActionEvent e){
		    //when graph button is pressed. 
		    String Function = null;
		    
		    try {

			Function = function.getText(); //Grabs text from textbox
			if (Function == null)
			    return;
			function.setText("");
	        
			int currentIndex = 0;
			for(double i = -5; i < 6; i = i +.01){
			    
			    Lexer lexer = new Lexer(Function);
			    Parser parser = new Parser(lexer);
			    double y = parser.parseExpr().eval(i);
			    double x = i;
			    //adds current point to arrayList
			    xList.add(currentIndex,x); 
			    yList.add(currentIndex,y);
			    currentIndex++;
			}
			repaint(); //redraws the graph
			
		    }
		    catch (SyntaxError g) {
			System.err.println(g.getMessage());
		  
		    }
		   
		}});	
	pack();
	setVisible(true);
    }
    public static void main(String[] argv) {
	Calc2 Calc2 = new Calc2();
    }	    
}	

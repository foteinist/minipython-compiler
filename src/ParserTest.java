import java.io.*;
import java.util.*;
import java.nio.file.*;
import minipython.lexer.Lexer;
import minipython.node.*;
import minipython.parser.Parser;

public class ParserTest {
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println("Usage: java ParserTest <filename.py>");
                System.out.println("Example: java ParserTest test.py");
                return;
            }
            
            // Read source code lines for error printing (used by TypeCheckerVisitor)
            List<String> sourceLines = null;
            try {
                sourceLines = Files.readAllLines(Paths.get(args[0]));
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                return;
            }
            
            // Setup parser with lexer
            Parser parser = new Parser(
                new Lexer(
                    new PushbackReader(
                        new FileReader(args[0]), 1024)));
            
            System.out.println("=== MINIPYTHON SEMANTIC ANALYSIS ===");
            System.out.println("File: " + args[0]);
            System.out.println("=" .repeat(50));
            
            // Parse the AST
            Start ast = parser.parse();
            
            // Create symbol table to share between visitors
            Hashtable<String, Object> symtable = new Hashtable<>();
            
            // PASS 1: Function Declarations (Rules 2, 7)
            System.out.println("\n--- PASS 1: Function Declarations ---");
            System.out.println("Checking: Function redeclaration, undeclared functions");
            DeclarationVisitor declarationVisitor = new DeclarationVisitor(symtable);
            ast.apply(declarationVisitor);
            
            // PASS 2: Variable Declarations (Rule 1)
            System.out.println("\n--- PASS 2: Variable Declarations ---");
            System.out.println("Checking: Undeclared variables, declaration order");
            VariableDeclarationVisitor variableVisitor = new VariableDeclarationVisitor(symtable);
            ast.apply(variableVisitor);
            
            // PASS 3: Type Checking (Rules 3, 4, 5, 6)
            System.out.println("\n--- PASS 3: Type Checking ---");
            System.out.println("Checking: Arithmetic operations, array access, return statements, if/while conditions");
            TypeCheckerVisitor typeChecker = new TypeCheckerVisitor(symtable, sourceLines);
            ast.apply(typeChecker);
            
            // Summary
            System.out.println("\n" + "=" .repeat(50));
            System.out.println("ANALYSIS COMPLETE");
            System.out.println("=" .repeat(50));
            
        } catch (FileNotFoundException e) {
            System.err.println("Error: File not found - " + args[0]);
        } catch (Exception e) {
            System.err.println("Error during parsing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
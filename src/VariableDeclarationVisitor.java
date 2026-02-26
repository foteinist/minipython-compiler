import java.util.*;
import minipython.analysis.*;
import minipython.node.*;

public class VariableDeclarationVisitor extends DepthFirstAdapter 
{
    // Example: For code "x = 5", stores that 'x' exists in symbol table
    private Hashtable<String, Object> symtable;
    
    // Example: For code:
    // 1. global scope
    // 2. function foo()
    // 3. function bar() inside foo()
    // Stack: ["global"] → ["global", "foo"] → ["global", "foo", "bar"]
    private Stack<String> currentScope;
    
    // Example: When inside function foo(), currentFunction = "foo"
    private String currentFunction;
    
    // Example:
    // declaredVariablesByScope = {
    //   "global": {"x", "y"},
    //   "foo": {"param1", "local_var"},
    //   "bar": {"param2"}
    // }
    private Map<String, Set<String>> declaredVariablesByScope;
    
    // Example:
    // variableUsagesByScope = {
    //   "foo": [
    //     VariableUsage("param1", 10, true),    // parameter declaration
    //     VariableUsage("x", 12, false),        // usage of global x
    //     VariableUsage("local_var", 14, true), // local declaration
    //     VariableUsage("local_var", 16, false) // usage of local_var
    //   ]
    // }
    private Map<String, List<VariableUsage>> variableUsagesByScope;
    
    // Example usage: stores info about variable usage at specific line
    private static class VariableUsage {
        String varName;      // Variable name, e.g., "counter"
        int line;           // Line number, e.g., 25
        boolean isDeclaration; // true for "counter = 0", false for "print(counter)"
        
        VariableUsage(String varName, int line, boolean isDeclaration) {
            this.varName = varName;
            this.line = line;
            this.isDeclaration = isDeclaration;
        }
    }
    
    // Example: [ErrorMessage(15, "undeclared_var"), ErrorMessage(20, "unknown")]
    private List<ErrorMessage> allErrorMessages;
    
    // Example: Stores error about variable 'z' at line 30
    private static class ErrorMessage {
        int line;          // Line where error occurs, e.g., 30
        String varName;    // Name of problematic variable, e.g., "z"
        
        ErrorMessage(int line, String varName) {
            this.line = line;
            this.varName = varName;
        }
    }
    
    // Example: Prevents reporting "Line 15: 'x' not declared" multiple times
    private Set<String> reportedErrors;
    
    public VariableDeclarationVisitor(Hashtable<String, Object> symtable) 
    {
        this.symtable = symtable;
        this.currentScope = new Stack<>();
        this.currentScope.push("global");
        this.currentFunction = null;
        this.declaredVariablesByScope = new HashMap<>();
        this.variableUsagesByScope = new HashMap<>();
        this.allErrorMessages = new ArrayList<>();
        this.reportedErrors = new HashSet<>();
        
        // Initialize for global scope
        // Example: declaredVariablesByScope: {"global": {}}
        // Example: variableUsagesByScope: {"global": []}
        declaredVariablesByScope.put("global", new HashSet<String>());
        variableUsagesByScope.put("global", new ArrayList<VariableUsage>());
    }
    
    // ========== METHODS FOR FUNCTIONS ==========
    
    // Example: When parsing "def calculate(a, b):"
    // Enters this method and sets:
    // currentFunction = "calculate"
    // currentScope = ["global", "calculate"]
    // declaredVariablesByScope: {"global": {}, "calculate": {}}
    public void inADefFuncFunction(ADefFuncFunction node)
    {
        currentFunction = node.getName().getText().trim();
        currentScope.push(currentFunction);
        
        // Initialize for this function
        declaredVariablesByScope.put(currentFunction, new HashSet<String>());
        variableUsagesByScope.put(currentFunction, new ArrayList<VariableUsage>());
        
        // Example: For "def calculate(a, b):"
        // paramNames = ["a", "b"]
        // Adds "a" and "b" to calculate's declared variables
        List<String> paramNames = extractParamNamesFromNode(node.getArgs());
        for (String paramName : paramNames) {
            Set<String> funcVars = declaredVariablesByScope.get(currentFunction);
            if (funcVars != null) {
                funcVars.add(paramName);
            }
            // Record parameters as declarations at line -1 (special marker)
            // Example: adds VariableUsage("a", -1, true)
            addVariableUsage(paramName, -1, true, currentFunction);
        }
    }
    
    // Example: After finishing "def calculate(a, b): ..."
    // Pops "calculate" from scope stack
    // If we were in nested function, updates currentFunction
    public void outADefFuncFunction(ADefFuncFunction node)
    {
        // Checking happens later at the end
        // This just exits the scope
        
        if (!currentScope.isEmpty()) {
            currentScope.pop();  // Exit function scope
        }
        // Update current function
        // Example: If stack was ["global", "foo", "bar"] and we pop "bar"
        // currentFunction becomes "foo"
        currentFunction = currentScope.size() > 0 && !currentScope.peek().equals("global") ? 
                        currentScope.peek() : null;
    }
    
    // ========== RULE 1: UNDECLARED VARIABLE ==========
    
    // Example: When parsing "print(x)" - handles the "x" reference
    // Calls handleIdentifier with isDeclaration=false
    public void inAIdPrimary(AIdPrimary node){
        handleIdentifier(node.getName(), false);
    }

    // Example: When parsing "y = value" - handles the "value" reference
    public void inAIdentifierValuenode(AIdentifierValuenode node)
    {
        handleIdentifier(node.getName(), false);
    }
    
    // Example: When parsing "arr[index]" - handles the "arr" reference
    public void inAPinakasExpressionValue(APinakasExpressionValue node) {
        handleIdentifier(node.getId(), false);
    }
    
    // Main handler for all variable occurrences
    // Example scenarios:
    // 1. "x = 10" → handleIdentifier("x", true)  // declaration
    // 2. "y = x + 5" → handleIdentifier("x", false) // usage
    // 3. "print(len)" → returns early (len is built-in)
    private void handleIdentifier(Token varToken, boolean isDeclaration) {
        String varName = varToken.getText().trim();
        int line = varToken.getLine();
        
        // Example: "len", "print", "type" are ignored
        if (isBuiltInFunction(varName)) {
            return;
        }
        
        String currentScopeName = currentScope.peek();
        
        // Example: For line 10: "result = calculation * 2"
        // Adds VariableUsage("calculation", 10, false) to current scope
        addVariableUsage(varName, line, isDeclaration, currentScopeName);
        
        // If declaration, mark as declared in this scope
        // Example: For line 8: "calculation = 10"
        // Adds "calculation" to declaredVariablesByScope[currentScope]
        if (isDeclaration) {
            Set<String> scopeVars = declaredVariablesByScope.get(currentScopeName);
            if (scopeVars != null) {
                scopeVars.add(varName);
            }
        }
    }
    
    // Example: Adds usage to appropriate list
    // For scope "foo", line 15, variable "temp", declaration=true
    // Creates VariableUsage("temp", 15, true)
    // Adds to variableUsagesByScope["foo"]
    private void addVariableUsage(String varName, int line, boolean isDeclaration, String scope) {
        List<VariableUsage> usages = variableUsagesByScope.get(scope);
        if (usages != null) {
            usages.add(new VariableUsage(varName, line, isDeclaration));
        }
    }
    
    // ========== RECORDING VARIABLE INITIALIZATIONS ==========
    
    // Example: For "counter = 0" → handles "counter" as declaration
    public void inAAssignStatementStatement(AAssignStatementStatement node){
        handleIdentifier(node.getId(), true);
    }
    
    // Example: For "counter -= 1" → handles "counter" as usage
    public void inAMineqStatementStatement(AMineqStatementStatement node){
        handleIdentifier(node.getId(), false);
    }
    
    // Example: For "counter += 1" → handles "counter" as usage
    public void inAPluseqStatementStatement(APluseqStatementStatement node){
        handleIdentifier(node.getId(), false);
    }
    
    // Example: For "value *= 2" → handles "value" as usage
    public void inAMulteqStatementStatement(AMulteqStatementStatement node){
        handleIdentifier(node.getId(), false);
    }
    
    // Example: For "total /= count" → handles "total" as usage
    public void inADiveqStatementStatement(ADiveqStatementStatement node){
        handleIdentifier(node.getId(), false);
    }
    
    // Example: For "array[5]" → handles "array" as usage
    public void inAArrayStatementStatement(AArrayStatementStatement node){
        handleIdentifier(node.getId(), false);
    }
    
    // ========== VARIABLE CHECKING IN EACH SCOPE ==========
    
    // Example: Checking scope "calculate" with usages:
    // Line 10: "result = x * y" (x and y are usages)
    // Line 8: "x = 10" (x declaration)
    // Line 6: "y = 5" (y declaration)
    // Will find error: x used before declaration
    private void checkVariablesInScope(String scope) {
        List<VariableUsage> usages = variableUsagesByScope.get(scope);
        if (usages == null) return;
        
        // Sort by line to check in execution order
        // Example: [Line 6, Line 8, Line 10]
        usages.sort(Comparator.comparingInt(u -> u.line));
        
        // Track what's declared so far
        // Example: After line 6: {"y"}
        // After line 8: {"y", "x"}
        Set<String> declaredSoFar = new HashSet<>();
        
        for (VariableUsage usage : usages) {
            if (usage.line == -1) {
                // Parameters are declared from start
                // Example: For "def func(a, b):", adds "a" and "b" immediately
                declaredSoFar.add(usage.varName);
                continue;
            }
            
            if (usage.isDeclaration) {
                // Declaration: add to declared set
                // Example: Line 8: "x = 10" → add "x"
                declaredSoFar.add(usage.varName);
            } else {
                // Usage: check if declared before
                // Example: Line 10: uses "z" but "z" not in declaredSoFar
                if (!declaredSoFar.contains(usage.varName)) {
                    // Check if it's a global variable (for functions)
                    if (!scope.equals("global")) {
                        Set<String> globalVars = declaredVariablesByScope.get("global");
                        if (globalVars != null && globalVars.contains(usage.varName)) {
                            // It's global, OK
                            // Example: Global variable "PI" used inside function
                            continue;
                        }
                    }
                    // Error: variable not declared
                    // Example: "Line 15: Variable 'unknown_var' is not declared"
                    String errorKey = scope + ":" + usage.line + ":" + usage.varName;
                    if (!reportedErrors.contains(errorKey)) {
                        allErrorMessages.add(new ErrorMessage(usage.line, usage.varName));
                        reportedErrors.add(errorKey);
                    }
                }
            }
        }
    }
    
    // Final checking after parsing entire file
    // Example: Checks code:
    // 1. x = 10                 (global)
    // 2. def foo():             
    // 3.     print(x)           (OK - x is global)
    // 4.     print(y)           (ERROR - y not declared)
    // 5.     y = 5              (Too late!)
    public void outStart(Start node) {
        // 1. Check all functions
        for (String scope : variableUsagesByScope.keySet()) {
            if (!scope.equals("global")) {
                checkVariablesInScope(scope);
            }
        }
        
        // 2. Check global scope
        checkVariablesInScope("global");
        
        // 3. Sort errors by line for better output
        // Example: [Line 20 error, Line 25 error, Line 30 error]
        allErrorMessages.sort(Comparator.comparingInt(e -> e.line));
        
        // 4. Print all errors
        // Note: line/2+1 because Minipython compiler uses double line numbers
        for (ErrorMessage error : allErrorMessages) {
            System.out.println("Line " + (error.line/2+1) + 
                             " [Rule 1] : Variable '" + error.varName + "' is not declared");
        }
    }
    
    // ========== HELPER METHODS ==========
    
    // Checks if name is Python built-in
    // Example: "len", "print", "max" return true
    // "my_function", "variable" return false
    private boolean isBuiltInFunction(String name) {
        return name.equals("len") || name.equals("type") || name.equals("open") || 
               name.equals("ascii") || name.equals("max") || name.equals("min") ||
               name.equals("print") || name.equals("assert");
    }
    
    // Extracts parameter names from function definition
    // Example: "def foo(a, b, c):" → ["a", "b", "c"]
    private List<String> extractParamNamesFromNode(PArgumentOpt args) {
        List<String> paramNames = new ArrayList<>();
        
        if (args instanceof AHasArgsArgumentOpt) {
            AHasArgsArgumentOpt hasArgs = (AHasArgsArgumentOpt) args;
            PArgument arg = hasArgs.getArg();
            extractParamNamesFromArgument(arg, paramNames);
        }
        
        return paramNames;
    }
    
    // Recursively extracts parameters
    // Example: Handles "a, b, c" pattern
    private void extractParamNamesFromArgument(PArgument arg, List<String> paramNames) {
        if (arg instanceof AArgumentArgument) {
            AArgumentArgument argument = (AArgumentArgument) arg;
            paramNames.add(argument.getParam().getText().trim());
            
            PArgumentTail tail = argument.getRest();
            if (tail instanceof AContinueArgumentTail) {
                extractParamNamesFromTail((AContinueArgumentTail) tail, paramNames);
            }
        }
    }
    
    // Handles additional parameters
    // Example: For "b, c" part of "a, b, c"
    private void extractParamNamesFromTail(AContinueArgumentTail cont, List<String> paramNames) {
        paramNames.add(cont.getNextParam().getText().trim());
        
        PArgumentTail more = cont.getMore();
        if (more instanceof AContinueArgumentTail) {
            extractParamNamesFromTail((AContinueArgumentTail) more, paramNames);
        }
    }
}
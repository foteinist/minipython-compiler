import java.util.*;
import minipython.analysis.*;
import minipython.node.*;

public class DeclarationVisitor extends DepthFirstAdapter 
{
    // Class to store information about a function
    // Example: For "def calculate(a, b=5):"
    // name = "calculate", paramCount = 2, line = 10
    // paramNames = ["a", "b"], paramHasDefault = [false, true]
    // requiredParams = 1, isDeclared = true
    public static class FunctionInfo {
        public String name;
        public int paramCount;
        public int line;
        public List<String> paramNames;
        public List<Boolean> paramHasDefault;
        public int requiredParams;
        public PArgumentOpt parameters;
        public boolean isDeclared = false; // To know if it has been declared
    }
    
    private Hashtable<String, Object> symtable;
    private Stack<String> currentScope;
    private String currentFunction;
    private List<FunctionCallCheck> pendingChecks;
    
    // Class to store function calls that we check later
    // Example: For "result = calculate(x, y)" at line 25
    // funcName = "calculate", line = 25
    private static class FunctionCallCheck {
        String funcName;
        int line;
        
        FunctionCallCheck(String funcName, int line) {
            this.funcName = funcName;
            this.line = line;
        }
    }
    
    public DeclarationVisitor(Hashtable<String, Object> symtable) 
    {
        this.symtable = symtable;
        this.currentScope = new Stack<>();
        this.currentScope.push("global");
        this.currentFunction = null;
        this.pendingChecks = new ArrayList<>();

        // Initialize symbol table if not already done
        // Example: symtable will contain:
        // "functions" → Hashtable with FunctionInfo objects
        // "variables" → Hashtable for variable information
        if (!symtable.containsKey("functions")) {
            symtable.put("functions", new Hashtable<String, FunctionInfo>());
        }
        if (!symtable.containsKey("variables")) {
            symtable.put("variables", new Hashtable<String, Object>());
        }
    }
    
    // ========== RULE 7: FUNCTION REDECLARATION ==========
    // Checks for duplicate function definitions
    
    // Called when entering a function command
    // Example: Processes "def calculate(a, b): ..."
    public void inAFuncCommands(AFuncCommands node)
    {
        PFunction function = node.getFunc();
        if (function instanceof ADefFuncFunction) {
            ADefFuncFunction funcDef = (ADefFuncFunction) function;
            recordFunctionDefinition(funcDef);
        }
    }
    
    // Records a function definition in the symbol table
    // Example: For "def calculate(a, b=5):"
    // Creates FunctionInfo and checks if it's a duplicate
    private void recordFunctionDefinition(ADefFuncFunction node) 
    {
        Token funcNameToken = node.getName();
        String funcName = funcNameToken.getText().trim();
        int line = funcNameToken.getLine();
        
        Hashtable<String, FunctionInfo> functions = 
            (Hashtable<String, FunctionInfo>) symtable.get("functions");
        
        // Get function information
        int paramCount = countParameters(node.getArgs());
        List<String> paramNames = getParamNames(node.getArgs());
        List<Boolean> paramHasDefault = getParamDefaults(node.getArgs());
        int requiredParams = countRequiredParameters(paramHasDefault);
        
        // Check if function already exists
        if (functions.containsKey(funcName)) {
            FunctionInfo existing = functions.get(funcName);
            
            // Mark that the function is now declared
            existing.isDeclared = true;
            
            // Check if this is a duplicate definition
            if (isDuplicateFunction(existing, paramCount, requiredParams, paramHasDefault)) {
                // Example: If we have both "def calculate(a):" and "def calculate(a, b=5):"
                // This might be considered ambiguous
                System.out.println("Line " + (line/2+1) + " [Rule 7]: Function '" + funcName + 
                                 "' already defined with " + existing.paramCount + 
                                 " parameters (considering default values)");
            }
        } else {
            // First time seeing this function
            FunctionInfo info = new FunctionInfo();
            info.name = funcName;
            info.line = line;
            info.paramCount = paramCount;
            info.paramNames = paramNames;
            info.paramHasDefault = paramHasDefault;
            info.parameters = node.getArgs();
            info.requiredParams = requiredParams;
            info.isDeclared = true; // Now it's declared
            
            functions.put(funcName, info);
            
            // Check if there were any pending calls for this function
            // Example: If someone called "calculate()" before defining it
            checkPendingCallsForFunction(funcName);
        }
    }
    
    // Checks if there are pending calls waiting for this function
    // Example: If "calculate()" was called on line 10 and "def calculate():" appears on line 15
    private void checkPendingCallsForFunction(String funcName) {
        Iterator<FunctionCallCheck> iterator = pendingChecks.iterator();
        while (iterator.hasNext()) {
            FunctionCallCheck check = iterator.next();
            if (check.funcName.equals(funcName)) {
                // Found a pending call - remove it
                iterator.remove();
            }
        }
    }
    
    // Determines if two function definitions are duplicates
    // Example: Are "def f(x,y)" and "def f(x,y,z=1)" considered duplicates?
    private boolean isDuplicateFunction(FunctionInfo existing, int newParamCount, 
                                        int newRequiredParams, List<Boolean> newParamHasDefault) {
        // 1. Same total number of parameters
        // Example: "def f(x,y)" and "def f(a,b)" → both have 2 parameters
        if (existing.paramCount == newParamCount) {
            return true;
        }
        
        // 2. If they have same number of required parameters
        // AND one count is maximum for one of the functions
        if (existing.requiredParams == newRequiredParams) {
            // Case 1: "def f(x,y)" vs "def f(x,y,z=1)"
            // Both have 2 required params, first function has exactly 2 params total
            if (existing.requiredParams == existing.paramCount) {
                return true;
            }
            
            // Case 2: "def f(x,y,z=1)" vs "def f(x,y)"
            // Both have 2 required params, second function has exactly 2 params total
            if (newRequiredParams == newParamCount) {
                return true;
            }
        }
        
        return false;
    }
    
    // Counts required (non-default) parameters
    // Example: For [false, true, false] → 2 required parameters
    private int countRequiredParameters(List<Boolean> paramHasDefault) {
        int count = 0;
        for (boolean hasDefault : paramHasDefault) {
            if (!hasDefault) {
                count++;
            }
        }
        return count;
    }
    
    // ========== FUNCTION ENTRY/EXIT ==========
    
    // Called when entering a function definition
    // Example: For "def calculate():", sets currentFunction = "calculate"
    public void inADefFuncFunction(ADefFuncFunction node)
    {
        currentFunction = node.getName().getText().trim();
        currentScope.push(currentFunction);
    }
    
    // Called when exiting a function definition
    // Example: Restores previous scope after function ends
    public void outADefFuncFunction(ADefFuncFunction node)
    {
        if (!currentScope.isEmpty()) {
            currentScope.pop();
        }
        currentFunction = currentScope.size() > 0 && !currentScope.peek().equals("global") ? 
                        currentScope.peek() : null;
    }
    
    // ========== RULE 2: UNDECLARED FUNCTION ==========
    // MODIFIED: Allows calls before declaration
    
    // Checks function calls in statements
    // Example: "calculate(x, y);"
    public void inAFunctionCallStatementStatement(AFunctionCallStatementStatement node)
    {
        PFunctionCall functionCall = node.getCall();
        checkFunctionCall(functionCall, false);
    }

    // Checks function calls in primary expressions
    // Example: "result = calculate(x, y)"
    public void inAFuncCallPrimary(AFuncCallPrimary node)
    {
        PFunctionCall functionCall = node.getCall();
        checkFunctionCall(functionCall, false);
    }

    // Checks function calls in expression values
    // Example: "x = calculate() + 5"
    public void inAFuncCallExpressionValue(AFuncCallExpressionValue node)
    {
        PFunctionCall functionCall = node.getCall();
        checkFunctionCall(functionCall, false);
    }
    
    // Checks function calls with dot notation
    // Example: "obj.method()"
    public void inAIdDotFuncValuenode(AIdDotFuncValuenode node)
    {
        PFunctionCall functionCall = node.getCall();
        checkFunctionCall(functionCall, false);
    }

    // Main method for checking function calls
    // Example: Processes "calculate(x, y)"
    private void checkFunctionCall(PFunctionCall functionCall, boolean isFinalCheck) {
        Token funcNameToken = null;
        String funcName = "";
        int line = -1;
        
        // Get function name and line number based on call type
        if (functionCall instanceof AWithArgsFunctionCall) {
            AWithArgsFunctionCall call = (AWithArgsFunctionCall) functionCall;
            funcNameToken = call.getName();
            funcName = funcNameToken.getText().trim();
            line = funcNameToken.getLine();
        } else if (functionCall instanceof ANoArgsFunctionCall) {
            ANoArgsFunctionCall call = (ANoArgsFunctionCall) functionCall;
            funcNameToken = call.getName();
            funcName = funcNameToken.getText().trim();
            line = funcNameToken.getLine();
        }
        
        if (!funcName.isEmpty() && line > 0) {
            if (!isFinalCheck) {
                // Save the call for later checking
                // Example: "calculate()" called on line 10, but not defined yet
                pendingChecks.add(new FunctionCallCheck(funcName, line));
            } else {
                // Final check - if function not found, it's an error
                checkFunctionExistence(funcName, line);
            }
        }
    }

    // Checks if a function exists (not built-in)
    // Example: Checks if "calculate" is in the functions table
    private void checkFunctionExistence(String funcName, int line) {
        if (isBuiltInFunction(funcName)) {
            return;
        }
        
        Hashtable<String, FunctionInfo> functions = 
            (Hashtable<String, FunctionInfo>) symtable.get("functions");
        
        // Function must exist AND be marked as declared
        // Example: If only referenced but never defined
        if (!functions.containsKey(funcName) || !functions.get(funcName).isDeclared) {
            System.out.println("Line " + (line/2+1) + "[Rule 2] : Function '" + funcName + "' is not declared");
        }
    }
    
    // ========== FINAL CHECK - at the end of the program ==========
    
    // Called after parsing entire file
    // Example: Checks all function calls that weren't matched with definitions
    public void outStart(Start node) {
        // Now do final check for all pending calls
        // Those that don't have matching functions are errors
        for (FunctionCallCheck check : pendingChecks) {
            checkFunctionExistence(check.funcName, check.line);
        }
    }
    
    // Checks if a name is a Python built-in function
    // Example: "len", "print", "max" return true
    private boolean isBuiltInFunction(String name) {
        return name.equals("len") || name.equals("type") || name.equals("open") || 
               name.equals("ascii") || name.equals("max") || name.equals("min") ||
               name.equals("print") || name.equals("assert");
    }
    
    // ========== HELPER METHODS ==========
    
    // Counts total parameters in a function definition
    // Example: For "def f(a, b, c=5):" returns 3
    private int countParameters(PArgumentOpt args) {
        if(args instanceof AHasArgsArgumentOpt) {
            AHasArgsArgumentOpt hasArgs = (AHasArgsArgumentOpt) args;
            PArgument arg = hasArgs.getArg();
            return countArgumentChain(arg, 0);
        }
        return 0;
    }

    // Recursively counts arguments in a chain
    private int countArgumentChain(PArgument arg, int count) {
        if (arg instanceof AArgumentArgument) {
            AArgumentArgument argument = (AArgumentArgument) arg;
            count++;
            PArgumentTail tail = argument.getRest();
            if (tail instanceof AContinueArgumentTail) {
                AContinueArgumentTail cont = (AContinueArgumentTail) tail;
                return countContinueChain(cont, count);
            }
        }
        return count;
    }

    // Recursively counts continuation arguments
    private int countContinueChain(AContinueArgumentTail cont, int count) {
        count++;
        PArgumentTail more = cont.getMore();
        if (more instanceof AContinueArgumentTail) {
            return countContinueChain((AContinueArgumentTail) more, count);
        }
        return count;
    }

    // Extracts parameter names from function definition
    // Example: For "def f(a, b, c):" returns ["a", "b", "c"]
    private List<String> getParamNames(PArgumentOpt args) {
        List<String> paramNames = new ArrayList<>();
        if (args instanceof AHasArgsArgumentOpt) {
            AHasArgsArgumentOpt hasArgs = (AHasArgsArgumentOpt) args;
            PArgument arg = hasArgs.getArg();
            extractParamNames(arg, paramNames);
        }
        return paramNames;
    }

    // Extracts parameter names recursively
    private void extractParamNames(PArgument arg, List<String> paramNames) {
        if (arg instanceof AArgumentArgument) {
            AArgumentArgument argument = (AArgumentArgument) arg;
            paramNames.add(argument.getParam().getText().trim());
            
            PArgumentTail tail = argument.getRest();
            if (tail instanceof AContinueArgumentTail) {
                extractParamNamesFromTail((AContinueArgumentTail) tail, paramNames);
            }
        }
    }

    // Extracts parameter names from tail arguments
    private void extractParamNamesFromTail(AContinueArgumentTail cont, List<String> paramNames) {
        paramNames.add(cont.getNextParam().getText().trim());
        
        PArgumentTail more = cont.getMore();
        if (more instanceof AContinueArgumentTail) {
            extractParamNamesFromTail((AContinueArgumentTail) more, paramNames);
        }
    }

    // Gets which parameters have default values
    // Example: For "def f(a, b=5, c):" returns [false, true, false]
    private List<Boolean> getParamDefaults(PArgumentOpt args) {
        List<Boolean> paramDefaults = new ArrayList<>();
        if (args instanceof AHasArgsArgumentOpt) {
            AHasArgsArgumentOpt hasArgs = (AHasArgsArgumentOpt) args;
            PArgument arg = hasArgs.getArg();
            extractParamDefaults(arg, paramDefaults);
        }
        return paramDefaults;
    }

    // Extracts default value information recursively
    private void extractParamDefaults(PArgument arg, List<Boolean> paramDefaults) {
        if (arg instanceof AArgumentArgument) {
            AArgumentArgument argument = (AArgumentArgument) arg;
            // Check if this parameter has a default value
            boolean hasDefault = !(argument.getDefault() instanceof ANoValueAssignValueOpt);
            paramDefaults.add(hasDefault);
            
            PArgumentTail tail = argument.getRest();
            if (tail instanceof AContinueArgumentTail) {
                extractParamDefaultsFromTail((AContinueArgumentTail) tail, paramDefaults);
            }
        }
    }

    // Extracts default value information from tail arguments
    private void extractParamDefaultsFromTail(AContinueArgumentTail cont, List<Boolean> paramDefaults) {
        boolean hasDefault = !(cont.getNextDefault() instanceof ANoValueAssignValueOpt);
        paramDefaults.add(hasDefault);
        
        PArgumentTail more = cont.getMore();
        if (more instanceof AContinueArgumentTail) {
            extractParamDefaultsFromTail((AContinueArgumentTail) more, paramDefaults);
        }
    }
}
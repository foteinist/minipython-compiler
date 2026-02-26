import java.util.*;
import minipython.analysis.*;
import minipython.node.*;

public class TypeCheckerVisitor extends DepthFirstAdapter {
    
    private Hashtable<String, Object> symtable;
    private List<String> sourceLines; 
    private Hashtable<Node, String> nodeTypes = new Hashtable<>();
    private Hashtable<String, Hashtable<String, String>> variableTypes = new Hashtable<>();
    private Hashtable<String, String> functionReturnTypes = new Hashtable<>();
    
    //Track which nodes are function calls
    private Hashtable<Node, Boolean> isFunctionCallNode = new Hashtable<>();
    // Store function names for nodes
    private Hashtable<Node, String> nodeFunctionNames = new Hashtable<>();
    
    private Stack<String> currentScope = new Stack<>();
    private String currentFunction = null;

   
    public TypeCheckerVisitor(Hashtable<String, Object> symtable, List<String> sourceLines) {
        this.symtable = symtable;
        this.sourceLines = sourceLines;
        this.currentScope.push("global");
        this.variableTypes.put("global", new Hashtable<>());
    }
    
    private void printError(int line, String message) {
        System.out.println("Line " + line + ": " + message);
        
        if (sourceLines != null && line > 0 && line <= sourceLines.size()) {
            String code = sourceLines.get(line - 1).trim();
            System.out.println("    > " + code);
        }
        System.out.println(); 
    }

    private void setNodeType(Node node, String type) {
        nodeTypes.put(node, type);
    }

    private String getNodeType(Node node) {
        String type = nodeTypes.getOrDefault(node, "unknown");
        return type;
    }

    private void setVarType(String varName, String type) {
        String scope = currentScope.peek();
        if (!variableTypes.containsKey(scope)) {
            variableTypes.put(scope, new Hashtable<>());
        }
        variableTypes.get(scope).put(varName, type);
    }

    private String getVarType(String varName) {
        String scope = currentScope.peek();
        if (variableTypes.containsKey(scope) && variableTypes.get(scope).containsKey(varName)) {
            return variableTypes.get(scope).get(varName);
        }
        if (variableTypes.containsKey("global") && variableTypes.get("global").containsKey(varName)) {
            return variableTypes.get("global").get(varName);
        }
        return "unknown"; 
    }
    
    @Override
    public void inADefFuncFunction(ADefFuncFunction node) {
        currentFunction = node.getName().getText().trim();
        currentScope.push(currentFunction);
        variableTypes.put(currentFunction, new Hashtable<>());
    }

    @Override
    public void outADefFuncFunction(ADefFuncFunction node) {
        if (currentFunction != null && !functionReturnTypes.containsKey(currentFunction)) {
            functionReturnTypes.put(currentFunction, "none");
        }
        
        currentScope.pop();
        currentFunction = currentScope.isEmpty() || currentScope.peek().equals("global") ? null : currentScope.peek();
    }

    // ================= LEAF NODES =================
    
    @Override
    public void outAIntegerLiteralValuenode(AIntegerLiteralValuenode node) {
        setNodeType(node, "int");
    }

    @Override
    public void outADecimalLiteralValuenode(ADecimalLiteralValuenode node) {
        setNodeType(node, "int"); 
    }

    @Override
    public void outADoubleQuotesValuenode(ADoubleQuotesValuenode node) {
        setNodeType(node, "string");
    }

    @Override
    public void outASingleQuotesValuenode(ASingleQuotesValuenode node) {
        setNodeType(node, "string");
    }

    @Override
    public void outANoneValueValuenode(ANoneValueValuenode node) {
        setNodeType(node, "none");
    }

    @Override
    public void outAIdentifierValuenode(AIdentifierValuenode node) {
        String name = node.getName().getText().trim();
        String type = getVarType(name);
        setNodeType(node, type);
    }
    
    // ================= TYPE PROPAGATION =================
    
    @Override
    public void outAValueSubsetValue(AValueSubsetValue node) {
        String type = getNodeType(node.getVal());
        setNodeType(node, type);
        // Propagate function call info
        if (isFunctionCallNode.containsKey(node.getVal())) {
            isFunctionCallNode.put(node, true);
            if (nodeFunctionNames.containsKey(node.getVal())) {
                nodeFunctionNames.put(node, nodeFunctionNames.get(node.getVal()));
            }
        }
    }
    
    @Override
    public void outAValuePow(AValuePow node) {
        String type = getNodeType(node.getExpr());
        setNodeType(node, type);
        // Propagate function call info
        if (isFunctionCallNode.containsKey(node.getExpr())) {
            isFunctionCallNode.put(node, true);
            if (nodeFunctionNames.containsKey(node.getExpr())) {
                nodeFunctionNames.put(node, nodeFunctionNames.get(node.getExpr()));
            }
        }
    }

    @Override
    public void outABasePowMultiplication(ABasePowMultiplication node) {
        String type = getNodeType(node.getExpr());
        setNodeType(node, type);
        // Propagate function call info
        if (isFunctionCallNode.containsKey(node.getExpr())) {
            isFunctionCallNode.put(node, true);
            if (nodeFunctionNames.containsKey(node.getExpr())) {
                nodeFunctionNames.put(node, nodeFunctionNames.get(node.getExpr()));
            }
        }
    }

    @Override
    public void outABaseMultExpression(ABaseMultExpression node) {
        String type = getNodeType(node.getExpr());
        setNodeType(node, type);
        // Propagate function call info
        if (isFunctionCallNode.containsKey(node.getExpr())) {
            isFunctionCallNode.put(node, true);
            if (nodeFunctionNames.containsKey(node.getExpr())) {
                nodeFunctionNames.put(node, nodeFunctionNames.get(node.getExpr()));
            }
        }
    }
    
    @Override
    public void outAFuncCallExpressionValue(AFuncCallExpressionValue node) {
        String type = getNodeType(node.getCall());
        setNodeType(node, type);
        // This IS a function call wrapper
        if (isFunctionCallNode.containsKey(node.getCall())) {
            isFunctionCallNode.put(node, true);
            if (nodeFunctionNames.containsKey(node.getCall())) {
                nodeFunctionNames.put(node, nodeFunctionNames.get(node.getCall()));
            }
        }
    }
    
    @Override
    public void outAParenthesisExpressionValue(AParenthesisExpressionValue node) {
        String type = getNodeType(node.getExpr());
        setNodeType(node, type);
        // Propagate function call info
        if (isFunctionCallNode.containsKey(node.getExpr())) {
            isFunctionCallNode.put(node, true);
            if (nodeFunctionNames.containsKey(node.getExpr())) {
                nodeFunctionNames.put(node, nodeFunctionNames.get(node.getExpr()));
            }
        }
    }

    // ================= ASSIGNMENTS =================

    @Override
    public void outAAssignStatementStatement(AAssignStatementStatement node) {
        String varName = node.getId().getText().trim();
        String exprType = getNodeType(node.getExpr());
        setVarType(varName, exprType);
    }
    
    @Override
    public void outAMineqStatementStatement(AMineqStatementStatement node) { 
        checkOpAssign(node.getId(), node.getExpr(), "-="); 
    }
    
    @Override
    public void outAPluseqStatementStatement(APluseqStatementStatement node) { 
        checkOpAssign(node.getId(), node.getExpr(), "+="); 
    }
    
    @Override
    public void outAMulteqStatementStatement(AMulteqStatementStatement node) { 
        checkOpAssign(node.getId(), node.getExpr(), "*="); 
    }
    
    @Override
    public void outADiveqStatementStatement(ADiveqStatementStatement node) { 
        checkOpAssign(node.getId(), node.getExpr(), "/="); 
    }

    private void checkOpAssign(Token id, PExpression expr, String op) {
        String varName = id.getText().trim();
        String currentType = getVarType(varName);
        String exprType = getNodeType(expr);
        int line = id.getLine();
        
        if (currentType.equals("none") || exprType.equals("none")) {
             printError(line, "[Rule 5] Operation '" + op + "' cannot use 'None'.");
        } 
        else if (!currentType.equals("unknown") && !exprType.equals("unknown") && !currentType.equals(exprType)) {
             printError(line, "[Rule 4] Type mismatch in '" + op + "'. Variable is " + currentType + ", expression is " + exprType + ".");
        }
    }

    // ================= ARITHMETIC LOGIC =================

    private void checkArithmetic(Node node, Node left, Node right, Token opToken) {
        String lType = getNodeType(left);
        String rType = getNodeType(right);
        String op = opToken.getText();
        int line = opToken.getLine();
          
        // Rule 5: None check
        if (lType.equals("none") || rType.equals("none")) {
            printError(line, "[Rule 5] Operation '" + op + "' cannot be performed with 'None'.");
            setNodeType(node, "error");
        } 
        else if (lType.equals("error") || rType.equals("error")) {
            setNodeType(node, "error");
        } 
        else if (lType.equals("unknown") || rType.equals("unknown")) {
            setNodeType(node, "unknown");
        } 
        // Type mismatch: Check if Rule 4 or Rule 6
        else if (!lType.equals(rType)) {
            
            boolean leftIsFunc = isFunctionCallNode.containsKey(left) && isFunctionCallNode.get(left);
            boolean rightIsFunc = isFunctionCallNode.containsKey(right) && isFunctionCallNode.get(right);
                
            // Rule 6: If ANY side is a function call
            if (leftIsFunc || rightIsFunc) {
                String funcName = "";
                String funcType = "";
                String otherType = "";
                
                if (leftIsFunc) {
                    funcName = nodeFunctionNames.getOrDefault(left, "unknown");
                    funcType = lType;
                    otherType = rType;
                } else {
                    funcName = nodeFunctionNames.getOrDefault(right, "unknown");
                    funcType = rType;
                    otherType = lType;
                }
                
                String message;
                
                if (leftIsFunc && rightIsFunc) {
                    String leftFuncName = nodeFunctionNames.getOrDefault(left, "unknown");
                    String rightFuncName = nodeFunctionNames.getOrDefault(right, "unknown");
                    message = "[Rule 6] Functions return incompatible types: '" + leftFuncName + 
                             "' returns " + lType + ", '" + rightFuncName + "' returns " + rType;
                } else {
                    message = "[Rule 6] Function '" + funcName + "' returns '" + funcType + 
                             "' but expected '" + otherType + "' for operation '" + op + "'.";
                }
                
                printError(line, message);
            } else {
                // Rule 4: No functions involved
                printError(line, "[Rule 4] Type mismatch in operation '" + op + 
                          "'. Cannot use " + lType + " with " + rType + ".");
            }
            
            setNodeType(node, "error");
        } 
        else {
            // Types match
            if (op.equals("+")) {
                setNodeType(node, lType); 
            } else if (lType.equals("string")) {
                printError(line, "[Rule 4] Operation '" + op + "' is not defined for strings.");
                setNodeType(node, "error");
            } else {
                setNodeType(node, "int");
            }
        }
    }

    // Check if node is a function call and return its name (recursive)
    private String getFunctionNameIfFunctionCall(Node node) {
        if (isFunctionCallNode.containsKey(node) && isFunctionCallNode.get(node)) {
            return nodeFunctionNames.getOrDefault(node, "");
        }
        return "";
    }

    @Override
    public void outAAddMultExpression(AAddMultExpression node) {
        checkArithmetic(node, node.getLeft(), node.getRight(), node.getOp());
    }

    @Override
    public void outASubMultExpression(ASubMultExpression node) {
        checkArithmetic(node, node.getLeft(), node.getRight(), node.getOp());
    }
    
    @Override
    public void outAMultMultiplication(AMultMultiplication node) {
        checkArithmetic(node, node.getLeft(), node.getRight(), node.getOp()); 
    }
    
    @Override
    public void outADivMultiplication(ADivMultiplication node) {
        checkArithmetic(node, node.getLeft(), node.getRight(), node.getOp());
    }
    
    @Override
    public void outAModMultiplication(AModMultiplication node) {
        checkArithmetic(node, node.getLeft(), node.getRight(), node.getOp());
    }
    
    @Override
    public void outAPowPow(APowPow node) {
        checkArithmetic(node, node.getLeft(), node.getRight(), node.getOp());
    }
    
    // ================= RETURN STATEMENTS =================
    
    @Override
    public void outAReturnStatementStatement(AReturnStatementStatement node) {
        if (currentFunction != null) {
            String returnType = getNodeType(node.getExpr());
            functionReturnTypes.put(currentFunction, returnType);
        }
    }

    // ================= FUNCTION CALLS =================

    @Override
    public void outANoArgsFunctionCall(ANoArgsFunctionCall node) {
        validateFunctionCall(node);
        setFunctionCallType(node);
    }

    @Override
    public void outAWithArgsFunctionCall(AWithArgsFunctionCall node) {
        validateFunctionCall(node);
        setFunctionCallType(node);
    }
    
    private void setFunctionCallType(Node callNode) {
        String funcName = "";
        if (callNode instanceof ANoArgsFunctionCall) {
            funcName = ((ANoArgsFunctionCall) callNode).getName().getText().trim();
        } else if (callNode instanceof AWithArgsFunctionCall) {
            funcName = ((AWithArgsFunctionCall) callNode).getName().getText().trim();
        }
        
        String returnType = functionReturnTypes.getOrDefault(funcName, "unknown");
        if (funcName.equals("len") || funcName.equals("ascii")) returnType = "int";
        if (funcName.equals("type")) returnType = "string";
        
        setNodeType(callNode, returnType);
        
        // MARK THIS NODE AS FUNCTION CALL
        isFunctionCallNode.put(callNode, true);
        nodeFunctionNames.put(callNode, funcName);
    }
    
    private void validateFunctionCall(Node callNode) {
        String funcName = "";
        int line = 0;
        int argCount = 0;
        
        if (callNode instanceof ANoArgsFunctionCall) {
            ANoArgsFunctionCall c = (ANoArgsFunctionCall) callNode;
            funcName = c.getName().getText().trim();
            line = c.getName().getLine();
            argCount = 0;
        } else if (callNode instanceof AWithArgsFunctionCall) {
            AWithArgsFunctionCall c = (AWithArgsFunctionCall) callNode;
            funcName = c.getName().getText().trim();
            line = c.getName().getLine();
            argCount = countArgs(c.getArgs());
        }

        // Rule 3: Argument count check
        Hashtable<String, DeclarationVisitor.FunctionInfo> functions = 
            (Hashtable<String, DeclarationVisitor.FunctionInfo>) symtable.get("functions");
            
        if (functions != null && functions.containsKey(funcName)) {
            DeclarationVisitor.FunctionInfo info = functions.get(funcName);
            
            if (argCount < info.requiredParams || argCount > info.paramCount) {
                String expected = info.requiredParams == info.paramCount ? 
                                  String.valueOf(info.paramCount) : 
                                  info.requiredParams + " to " + info.paramCount;
                
                printError(line, "[Rule 3] Function '" + funcName + "' expects " + expected + " arguments, but got " + argCount + ".");
            }
        }
    }

    private int countArgs(PCallArgs args) {
        if (args instanceof AArgsCallArgs) {
            AArgsCallArgs a = (AArgsCallArgs) args;
            return 1 + countArgsTail(a.getRest());
        }
        return 0;
    }

    private int countArgsTail(PCallArgsTail tail) {
        if (tail instanceof AContinueCallArgsTail) {
            AContinueCallArgsTail c = (AContinueCallArgsTail) tail;
            return 1 + countArgsTail(c.getMore());
        }
        return 0;
    }
}
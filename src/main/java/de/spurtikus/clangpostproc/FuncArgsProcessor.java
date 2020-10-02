package de.spurtikus.clangpostproc;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTIdExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTParameterDeclaration;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FuncArgsProcessor {

    // Initial list of well known functions
    private static String[] initialKnownFunctions = { "GetFuncStatusCode", "GetErrorStatus",
            "ClearFuncStatusCode","TestFuncStatusAndPtr", "RdEngOption"};
    // Increasing list of functions "known", i.e. they use known functions and thus are
    // also considered "known"
    private static List<String> knownFunctions = new ArrayList<>();
    private static int ctrChanged;

    public static void processFunctions(IASTTranslationUnit translationUnit, OutputStream ostream)
            throws ExpansionOverlapsBoundaryException, IOException {
        initializeKnownFunctions();
        IASTDeclaration[] decl = translationUnit.getDeclarations();
        //System.out.println(decl.length);
        ctrChanged = 0;
        for (IASTDeclaration d: decl) {
            //System.out.println(d.getChildren().length);
            IASTNode[] childs = d.getChildren();
            Class<? extends IASTDeclaration> dClass = d.getClass();
            //System.out.println(dClass.getCanonicalName());

            if (d instanceof CPPASTFunctionDefinition) {
                CPPASTFunctionDefinition function = (CPPASTFunctionDefinition) d;
                //System.out.println(function.getDeclarator().getName());
                //if (function.getDeclarator().getName().toString().equals("SetFuncStatusCode")) {
                //System.out.println(function.getRawSignature());
                processFunction(function, ostream);
                //}
            }
        }
        System.out.println("Changed " + ctrChanged + " functions.");
    }

    private static void initializeKnownFunctions() {
        Arrays.stream(initialKnownFunctions).forEach( f -> knownFunctions.add(f));
    }

    public static void processFunction(CPPASTFunctionDefinition f, OutputStream ostream)
            throws ExpansionOverlapsBoundaryException, IOException {
        int index = 0;

        IASTFunctionDeclarator declarator = f.getDeclarator();
        //System.out.println(declarator.getRawSignature());
        List<String> functionArgs = new ArrayList<>();
        for (IASTNode c: declarator.getChildren()) {
            //System.out.println(c.getClass());
            if (c instanceof CPPASTParameterDeclaration) {
                CPPASTParameterDeclaration cc = (CPPASTParameterDeclaration) c;
                //System.out.println(cc.getDeclSpecifier().getRawSignature());
                functionArgs.add(cc.getDeclarator().getName().toString());
            }
        }
        System.out.print("Function " + declarator.getName() + " has these args: " );
        functionArgs.forEach(arg -> System.out.print( arg + ", "));
        System.out.println();
        String firstArg = functionArgs.get(0);
        String argumentName = getFirstArgumentChangeable(f, index, firstArg);

        if (argumentName != null && argumentName != "null") {
            System.out.println("Changeable arg found: " + argumentName);
            writeSedLine(f, declarator, argumentName, ostream);

            // Add function to list of known functions
            knownFunctions.add(declarator.getName().toString());
            ctrChanged++;
        }

    }

    static String getFirstArgumentChangeable(IASTNode f, int index, String firstArg)
            throws ExpansionOverlapsBoundaryException {
        String indent = "";
        for (int i = 0; i < index; i++) {
            indent += " ";
        }
        //System.out.println(/*indent + ": " +*/ f.getClass().getSimpleName());
        if (f instanceof CPPASTFunctionCallExpression) {
            CPPASTFunctionCallExpression function = (CPPASTFunctionCallExpression) f;
            System.out.println(f.getRawSignature());
            String funcName = function.getSyntax().toString();
            //System.out.println("funcName: " + funcName);
            IASTInitializerClause[] args = function.getArguments();
            for (IASTInitializerClause arg: args) {
                //System.out.println(arg.getClass());
                if (arg instanceof CPPASTIdExpression) {
                    CPPASTIdExpression a = (CPPASTIdExpression) arg;
                    String callArg = a.getName().toString();
                    //System.out.println(callArg);
                    if (firstArg.equals(callArg)) {
                        boolean isKnown = knownFunctions.stream().anyMatch(fu -> fu.equals(funcName));
                        if (isKnown) {
                            //System.out.println("Function argument '" + callArg + "' reused in known method call");
                            return callArg;
                        }
                    }
                }
            }
        }
        if (f.getChildren() != null) {
            for (IASTNode c: f.getChildren()) {
                String result = getFirstArgumentChangeable(c, index++, firstArg);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    static void createArgumentPartChanged(IASTFunctionDeclarator declarator, StringBuilder sb,
                                          String from, String to) {
        int pos = 0;
        for (IASTNode c: declarator.getChildren()) {
            if (c instanceof CPPASTParameterDeclaration) {
                CPPASTParameterDeclaration cc = (CPPASTParameterDeclaration) c;
                String sig = cc.getDeclSpecifier().getRawSignature();
                if (cc.getDeclarator().getName().toString().equals(from)) {
                    sig = to;
                }
                sb.append(sig);
                sb.append(" ");
                sb.append(cc.getDeclarator().getName().toString());
                pos += 1;
                if (pos+1 < declarator.getChildren().length) {
                    sb.append(",");
                    sb.append(" ");
                }
            }
        }
    }

    static void createArgumentPart(IASTFunctionDeclarator declarator, StringBuilder sb) {
        int pos = 0;
        for (IASTNode c: declarator.getChildren()) {
            if (c instanceof CPPASTParameterDeclaration) {
                CPPASTParameterDeclaration cc = (CPPASTParameterDeclaration) c;
                sb.append(cc.getDeclSpecifier().getRawSignature());
                sb.append(" ");
                sb.append(cc.getDeclarator().getName().toString());
                pos += 1;
                if (pos+1 < declarator.getChildren().length) {
                    sb.append(",");
                    sb.append(" ");
                }
            }
        }
    }

    private static void writeSedLine(CPPASTFunctionDefinition f, IASTFunctionDeclarator declarator,
                                     String argumentName, OutputStream ostream) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("s/");
        sb.append(f.getDeclarator().getName());
        sb.append("\\(");
        createArgumentPart(declarator, sb);
        sb.append("\\)/");
        sb.append(f.getDeclarator().getName());
        sb.append("\\(");
        createArgumentPartChanged(declarator, sb, argumentName, "SET9052*");
        sb.append("\\)/\n");
        ostream.write(sb.toString().getBytes());
    }

}

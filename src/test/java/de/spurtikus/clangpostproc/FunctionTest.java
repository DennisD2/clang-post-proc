package de.spurtikus.clangpostproc;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.spurtikus.clangpostproc.ClangGenerator.getIastTranslationUnit;

public class FunctionTest {

    @Test
    public void testFunctionDumps() throws CoreException, IOException, ExpansionOverlapsBoundaryException {
        String inFileName = "src/test/resources/morrow/mtcsa32.dll.c";
        IASTTranslationUnit translationUnit = getIastTranslationUnit(inFileName);

        IASTDeclaration[] decl = translationUnit.getDeclarations();
        //System.out.println(decl.length);
        int ctrAll = 0;
        int ctrFun = 0;
        int ctrSimple = 0;
        for (IASTDeclaration d: decl) {
            ctrAll++;
            //System.out.println(d.getChildren().length);
            IASTNode[] childs = d.getChildren();
            Class<? extends IASTDeclaration> dClass = d.getClass();
            //System.out.println(dClass.getCanonicalName());

            if (d instanceof CPPASTFunctionDefinition) {
                ctrFun++;
                CPPASTFunctionDefinition function = (CPPASTFunctionDefinition) d;
                //System.out.println(function.getDeclarator().getName());
                if (function.getDeclarator().getName().toString().equals("SetFuncStatusCode")) {
                    System.out.println(function.getRawSignature());
                }
            }

            /*if (d instanceof CPPASTSimpleDeclaration) {
                ctrSimple++;
                CPPASTSimpleDeclaration simple = (CPPASTSimpleDeclaration) d;
                IASTDeclarator[] declarators = simple.getDeclarators();
                for (IASTDeclarator dd: declarators) {
                    System.out.print(dd.getName().toString());
                }
                System.out.println();
                IASTDeclSpecifier spec = simple.getDeclSpecifier();
                System.out.println(spec.getRawSignature());
            }*/
        }
        System.out.println("All: " + ctrAll);
        System.out.println("Functions: " + ctrFun);
        System.out.println("Simple: " + ctrSimple);
        System.out.println("???: " + (ctrAll - ctrFun - ctrSimple));

    }

    @Test
    public void testFunctionCalls() throws CoreException, IOException, ExpansionOverlapsBoundaryException {
        String inFileName = "src/test/resources/morrow/mtcsa32.dll.c";
        IASTTranslationUnit translationUnit = getIastTranslationUnit(inFileName);

        IASTDeclaration[] decl = translationUnit.getDeclarations();
        //System.out.println(decl.length);
        for (IASTDeclaration d: decl) {
            //System.out.println(d.getChildren().length);
            IASTNode[] childs = d.getChildren();
            Class<? extends IASTDeclaration> dClass = d.getClass();
            //System.out.println(dClass.getCanonicalName());

            if (d instanceof CPPASTFunctionDefinition) {
                CPPASTFunctionDefinition function = (CPPASTFunctionDefinition) d;
                //System.out.println(function.getDeclarator().getName());
                if (function.getDeclarator().getName().toString().equals("SetFuncStatusCode")) {
                    System.out.println(function.getRawSignature());
                    processFunction(function);
                }
            }
        }
    }

    public void processFunction(CPPASTFunctionDefinition f) throws ExpansionOverlapsBoundaryException {
        int index = 0;
        IASTFunctionDeclarator declarator = f.getDeclarator();
        //System.out.println(declarator.getRawSignature());
        List<String> functionArgs = new ArrayList<>();
        for (IASTNode c: declarator.getChildren()) {
            System.out.println(c.getClass());
            if (c instanceof CPPASTParameterDeclaration) {
                CPPASTParameterDeclaration cc = (CPPASTParameterDeclaration) c;
                System.out.println(cc.getDeclSpecifier().getRawSignature());
                functionArgs.add(cc.getDeclarator().getName().toString());
            }
        }
        System.out.print("Function " + f.getDeclarator().getName() + " has these args: " );
        functionArgs.forEach(arg -> System.out.print( arg + ", "));
        System.out.println();
        String argumentName = getAnyArgumentChangeable(f, index, functionArgs);
        System.out.println("Result: " + argumentName);

        StringBuilder sb = new StringBuilder();
        sb.append("s/");
        sb.append(f.getDeclarator().getName());
        sb.append("\\(");
        createArgumentPart(declarator, sb);
        sb.append("\\)//");
        sb.append(f.getDeclarator().getName());
        sb.append("\\(");
        createArgumentPartChanged(declarator, sb, argumentName, "SET9052*");
        sb.append("\\)/");
        System.out.println(sb.toString());
    }

    private void createArgumentPart(IASTFunctionDeclarator declarator, StringBuilder sb) {
        int pos = 0;
        for (IASTNode c: declarator.getChildren()) {
            if (c instanceof CPPASTParameterDeclaration) {
                CPPASTParameterDeclaration cc = (CPPASTParameterDeclaration) c;
                sb.append(cc.getDeclSpecifier().getRawSignature());
                sb.append(" ");
                sb.append(cc.getDeclarator().getName().toString());
                pos+=2;
                if (pos< declarator.getChildren().length) {
                    sb.append(",");
                    sb.append(" ");
                }
            }
        }
    }

    private void createArgumentPartChanged(IASTFunctionDeclarator declarator, StringBuilder sb,
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
                pos+=2;
                if (pos< declarator.getChildren().length) {
                    sb.append(",");
                    sb.append(" ");
                }
            }
        }
    }

    private static String[] knownFunctions = { "GetFuncStatusCode", "GetErrorStatus",
            "ClearFuncStatusCode","TestFuncStatusAndPtr", "RdEngOption"};
    private String getAnyArgumentChangeable(IASTNode f, int index, List<String> functionArgs) throws ExpansionOverlapsBoundaryException {
        String indent = "";
        for (int i = 0; i < index; i++) {
            indent += " ";
        }
        //System.out.println(/*indent + ": " +*/ f.getClass().getSimpleName());
        if (f instanceof CPPASTFunctionCallExpression) {
            CPPASTFunctionCallExpression function = (CPPASTFunctionCallExpression) f;
            System.out.println(f.getRawSignature());
            String funcName = function.getSyntax().toString();
            System.out.println("funcName: " + funcName);
            IASTInitializerClause[] args = function.getArguments();
            for (IASTInitializerClause arg: args) {
                System.out.println(arg.getClass());
                if (arg instanceof CPPASTIdExpression) {
                    CPPASTIdExpression a = (CPPASTIdExpression) arg;
                    String callArg = a.getName().toString();
                    System.out.println(callArg);
                    if (functionArgs.contains(callArg)) {
                        boolean isKnown = Arrays.stream(knownFunctions).anyMatch( fu -> fu.equals(funcName));
                        if (isKnown) {
                            System.out.println("Function argument '" + callArg + "' reused in known method call");
                            return callArg;
                        }
                    }
                }
            }
        }
        if (f.getChildren() != null) {
            for (IASTNode c: f.getChildren()) {
                String result = getAnyArgumentChangeable(c, index++, functionArgs);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}

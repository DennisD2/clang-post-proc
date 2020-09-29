package de.spurtikus.clangpostproc;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static de.spurtikus.clangpostproc.ClangGenerator.getIastTranslationUnit;

public class FunctionTest {

    @Test
    public void testFunctionCalls() throws CoreException, IOException {
        String inFileName = "src/test/resources/morrow/mtcsa32.dll.c";
        IASTTranslationUnit translationUnit = getIastTranslationUnit(inFileName);

        //FuncStatusCodeDefineProcessor.processFuncStatusCodeDefines(translationUnit, System.out);
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
                    dumpFunction(function);

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

    public void dumpFunction(CPPASTFunctionDefinition f) {
        int index = 0;
        IASTFunctionDeclarator declarator = f.getDeclarator();
        //System.out.println(declarator.getRawSignature());
        List<String> functionArgs = new ArrayList<>();
        for (IASTNode c: declarator.getChildren()) {
            System.out.println(c.getClass());
            if (c instanceof CPPASTParameterDeclaration) {
                CPPASTParameterDeclaration cc = (CPPASTParameterDeclaration) c;
                //System.out.println(cc.getDeclarator().getName().toString());
                functionArgs.add(cc.getDeclarator().getName().toString());
            }
        }
        System.out.print("Function has these args: " );
        functionArgs.forEach(arg -> System.out.print( arg + ", "));
        System.out.println();
        dumpFunctionInternal(f, index, functionArgs);
    }

    private void dumpFunctionInternal(IASTNode f, int index, List<String> functionArgs) {
        String indent = "";
        for (int i = 0; i < index; i++) {
            indent += " ";
        }
        //System.out.println(/*indent + ": " +*/ f.getClass().getSimpleName());
        if (f instanceof CPPASTFunctionCallExpression) {
            CPPASTFunctionCallExpression function = (CPPASTFunctionCallExpression) f;
            System.out.println(f.getRawSignature());
            IASTInitializerClause[] args = function.getArguments();
            for (IASTInitializerClause arg: args) {
                System.out.println(arg.getClass());
                if (arg instanceof CPPASTIdExpression) {
                    CPPASTIdExpression a = (CPPASTIdExpression) arg;
                    String callArg = a.getName().toString();
                    System.out.println(callArg);
                    if (functionArgs.contains(callArg)) {
                        System.out.println("Function argument '" + callArg + "' reused in known method call");
                    }
                }

            }
        }
        if (f.getChildren() != null) {
            for (IASTNode c: f.getChildren()) {
                dumpFunctionInternal(c, index++, functionArgs);
            }
        }
    }
}

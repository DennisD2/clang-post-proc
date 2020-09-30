package de.spurtikus.clangpostproc;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import java.io.IOException;

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
                System.out.println(function.getDeclarator().getName());
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

        FuncArgsProcessor.processFunctions(translationUnit, System.out);
    }

}

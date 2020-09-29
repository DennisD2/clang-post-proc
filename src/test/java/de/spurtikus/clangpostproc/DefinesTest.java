package de.spurtikus.clangpostproc;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.core.runtime.CoreException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static de.spurtikus.clangpostproc.ClangGenerator.*;

public class DefinesTest {

    @Test
    public void testDumpDefines() throws CoreException, IOException {
        String inFileName = "src/test/resources/morrow/sa_defin.h";
        IASTTranslationUnit translationUnit = getIastTranslationUnit(inFileName);

        System.out.println(translationUnit.getMacroDefinitions().length);
        Arrays.stream(translationUnit.getMacroDefinitions()).forEach(
                d -> System.out.println(d)
        );
    }

    @Test
    public void testIEDefines() throws CoreException, IOException {
        String inFileName = "src/test/resources/morrow/sa_defin.h";
        IASTTranslationUnit translationUnit = getIastTranslationUnit(inFileName);

        FuncStatusCodeDefineProcessor.processFuncStatusCodeDefines(translationUnit, System.out);
    }

}


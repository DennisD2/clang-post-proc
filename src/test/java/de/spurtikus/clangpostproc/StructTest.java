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

public class StructTest {
    // Visitor class for example
    private ASTVisitor visitor = new ASTVisitor() {
        public int visit(IASTName name) {
            if ((name.getParent() instanceof CPPASTFunctionDeclarator)) {
                System.out.println("V " + "IASTName: " + name.getClass().getSimpleName() +
                        "(" + name.getRawSignature() + ") - > parent: " + name.getParent().getClass().getSimpleName());
                System.out.println("V" + "-- isVisible: " + StructProcessor.isVisible(name));
            }

            return 3;
        }

        public int visit(IASTDeclaration declaration) {
            System.out.println("V" + "declaration: " + declaration + " ->  " + declaration.getRawSignature());

            if ((declaration instanceof IASTSimpleDeclaration)) {
                IASTSimpleDeclaration ast = (IASTSimpleDeclaration) declaration;
                try {
                    System.out.println("V" + "--- type: " + ast.getSyntax() + " (childs: " + ast.getChildren().length + ")");
                    IASTNode typedef = ast.getChildren().length == 1 ? ast.getChildren()[0] : ast.getChildren()[1];
                    System.out.println("V " + "------- typedef: " + typedef);
                    IASTNode[] children = typedef.getChildren();
                    if ((children != null) && (children.length > 0))
                        System.out.println("V " + "------- typedef-name: " + children[0].getRawSignature());
                } catch (ExpansionOverlapsBoundaryException e) {
                    e.printStackTrace();
                }

                IASTDeclarator[] declarators = ast.getDeclarators();
                for (IASTDeclarator iastDeclarator : declarators) {
                    System.out.println("V " + "iastDeclarator > " + iastDeclarator.getName());
                }

                IASTAttribute[] attributes = ast.getAttributes();
                for (IASTAttribute iastAttribute : attributes) {
                    System.out.println("V " + "iastAttribute > " + iastAttribute);
                }
            }

            if ((declaration instanceof IASTFunctionDefinition)) {
                IASTFunctionDefinition ast = (IASTFunctionDefinition) declaration;
                IScope scope = ast.getScope();
                try {
                    System.out.println("V " + "### function() - Parent = " + scope.getParent().getScopeName());
                    System.out.println("V " + "### function() - Syntax = " + ast.getSyntax());
                } catch (DOMException e) {
                    e.printStackTrace();
                } catch (ExpansionOverlapsBoundaryException e) {
                    e.printStackTrace();
                }
                ICPPASTFunctionDeclarator typedef = (ICPPASTFunctionDeclarator) ast.getDeclarator();
                System.out.println("V " + "------- typedef: " + typedef.getName());
            }

            return 3;
        }

        public int visit(IASTTypeId typeId) {
            System.out.println("V " + "typeId: " + typeId.getRawSignature());
            return 3;
        }

        public int visit(IASTStatement statement) {
            System.out.println("V " + "statement: " + statement.getRawSignature());
            return 3;
        }

        public int visit(IASTAttribute attribute) {
            return 3;
        }
    };

    @Test
    public void testDumpStruct() throws CoreException {
        String inFileName = "src/test/resources/morrow/str_9052.h";
        //String inFileName = "src/test/resources/test-1/TestFile.cpp";
        IASTTranslationUnit translationUnit = getIastTranslationUnit(inFileName);

        IASTPreprocessorIncludeStatement[] includes = translationUnit.getIncludeDirectives();
        System.out.println("- INCLUDES ----------------------------------------------------");
        for (IASTPreprocessorIncludeStatement include : includes) {
            System.out.println("include - " + include.getName());
        }
        System.out.println("- printTree ---------------------------------------------------");
        StructProcessor.dumpStructs(translationUnit, 1);
    }

    @Ignore
    @Test
    public void testHowToUseExample() throws CoreException {
        String inFileName = "src/test/resources/morrow/str_9052.h";
        //String inFileName = "src/test/resources/test-1/TestFile.cpp";
        IASTTranslationUnit translationUnit = getIastTranslationUnit(inFileName);

        IASTPreprocessorIncludeStatement[] includes = translationUnit.getIncludeDirectives();
        System.out.println("- INCLUDES ----------------------------------------------------");
        for (IASTPreprocessorIncludeStatement include : includes) {
            System.out.println("include - " + include.getName());
        }
        System.out.println("- printTree ---------------------------------------------------");
        ASTHelper.printTree(translationUnit, 1);

        System.out.println("- visitor walk ------------------------------------------------");
        // Configure visitor
        visitor.shouldVisitNames = true;
        visitor.shouldVisitDeclarations = false;
        visitor.shouldVisitDeclarators = true;
        visitor.shouldVisitAttributes = true;
        visitor.shouldVisitStatements = false;
        visitor.shouldVisitTypeIds = true;
        translationUnit.accept(visitor);
    }

    @Test
    public void testStructRead() throws CoreException {
        String inFileName = "src/test/resources/morrow/str_9052.h";
        IASTTranslationUnit translationUnit = getIastTranslationUnit(inFileName);

        List<String> structNames = StructProcessor.getStructNames(translationUnit);
        structNames.forEach(System.out::println);
        assert (structNames.size() > 0);
        assert (structNames.contains("SET9052"));
    }

    @Ignore
    @Test
    public void testPrintOffsets() throws CoreException {
        String inFileName = "src/test/resources/morrow/str_9052.h";
        IASTTranslationUnit translationUnit = getIastTranslationUnit(inFileName);

        Map<String, FieldInfo> structInfo = StructProcessor.getStructInfo(translationUnit, "SET9052");
        structInfo.keySet().forEach(si -> System.out.println(si + ", " + structInfo.get(si).getSpecifier()));

        Map<String, Integer> dataSizes = createDataSizeMap();

        System.out.println("------------------------------------------------");
        int offset = 0;
        for (String si : structInfo.keySet()) {
            FieldInfo fieldInfo = structInfo.get(si);
            offset = StructProcessor.recalcOffset(dataSizes.get(fieldInfo.getSpecifier()), offset);
            fieldInfo.setOffset(offset);

            int fieldSize = getFieldSize(dataSizes, si, fieldInfo);
            offset += fieldSize;
            System.out.println(si + ", " + fieldInfo.getSpecifier() + " offset: " + fieldInfo.getOffset());
        }

        //assert(structNames.size() > 0);
        //assert(structNames.contains("SET9052"));
    }

    @Test
    public void testDumpSolutions() throws CoreException, IOException {
        String inFileName = "src/test/resources/morrow/str_9052.h";
        IASTTranslationUnit translationUnit = getIastTranslationUnit(inFileName);

        Map<String, FieldInfo> structInfo = StructProcessor.getStructInfo(translationUnit, "SET9052");
        //structInfo.keySet().forEach(si -> System.out.println(si + ", " + structInfo.get(si).getSpecifier()));

        Map<String, Integer> dataSizes = createDataSizeMap();

        int offset = 0;
        for (String si : structInfo.keySet()) {
            FieldInfo fieldInfo = structInfo.get(si);
            offset = StructProcessor.recalcOffset(dataSizes.get(fieldInfo.getSpecifier()), offset);
            fieldInfo.setOffset(offset);

            int fieldSize = getFieldSize(dataSizes, si, fieldInfo);

            //System.out.println(si + ", " + fieldInfo.getSpecifier() + " offset: " + fieldInfo.getOffset());
            // (int16_t *)(a1 + 204) -> a1->func_status_code

            writeSedLines(offset, si, fieldInfo, System.out);
            offset += fieldSize;
        }
    }

}


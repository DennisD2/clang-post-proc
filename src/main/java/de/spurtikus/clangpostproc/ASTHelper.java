package de.spurtikus.clangpostproc;

import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTranslationUnit;

public class ASTHelper {

    static void printTree(IASTNode node, int index) {
        IASTNode[] children = node.getChildren();

        boolean printContents = true;

        if ((node instanceof CPPASTTranslationUnit)) {
            printContents = false;
        }

        String offsetString = "";
        try {
            offsetString = node.getSyntax() != null ? " (offset: " + node.getFileLocation().getNodeOffset() + ", len: "
                    + node.getFileLocation().getNodeLength() + ")" : "";
            //printContents = true; //node.getFileLocation().getNodeLength() < 30;
        } catch (ExpansionOverlapsBoundaryException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            offsetString = "UnsupportedOperationException";
        }

        // create indent string with spaces
        String indentString = String.format(new StringBuilder("%1$").append(index * 2).append("s").toString(),
                new Object[]{"- "});
        String objectClassName = node.getClass().getSimpleName();
        String rawSignature = node.getRawSignature();
        //String rawSignature = (printContents ? node.getRawSignature()
        // .replaceAll("\n", " \\ ") : node.getRawSignature().subSequence(0, 5))
        System.out.println(indentString + objectClassName + offsetString + " -> " + rawSignature);

        for (IASTNode iastNode : children)
            printTree(iastNode, index + 1);
    }
}

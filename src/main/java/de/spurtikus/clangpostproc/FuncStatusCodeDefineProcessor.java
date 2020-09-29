package de.spurtikus.clangpostproc;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class FuncStatusCodeDefineProcessor {
    static String[] funcStatusCodeIgnores = {"IE_TRUE", "IE_FALSE", "IE_ON", "IE_OFF", "IE_SPACE",
            "IE_CONTINUE", "IE_EXITPROG"
    };

    /**
     * true if value is used by SetFuncStatusCode()
     * @param code
     * @param ignores
     * @return
     */
    public static boolean isFuncStatusCode(IASTPreprocessorMacroDefinition code, String[] ignores) {
        String c = code.getName().toString();
        return (c.startsWith("IE_") && !c.contains("_ENG_") && !c.contains("_OPMODE_")
                && Arrays.stream(ignores).noneMatch(x -> x.equals(c)));
    }

    public static void processFuncStatusCodeDefines(IASTTranslationUnit translationUnit,
                                                    OutputStream ostream) throws IOException {
        // s/SetFuncStatusCode\(([a-z]*[0-9]), ([0-9,-]+)\)/SetFuncStatusCode\(\1, \/\* W9902 \2 \*\/ \)/
        for (IASTPreprocessorMacroDefinition d : translationUnit.getMacroDefinitions()) {
            boolean isFuncStatusCode = isFuncStatusCode(d, funcStatusCodeIgnores);
            if (isFuncStatusCode) {
                createSedLine(d, ostream);
            }
        }
    }

    private static void createSedLine(IASTPreprocessorMacroDefinition d, OutputStream ostream) throws IOException {
        //System.out.println(d.getName().toString() + " " + d.getExpansion());
        StringBuilder sb = new StringBuilder();
        sb.append("s/SetFuncStatusCode\\(([a-z]*[0-9]), ");
        sb.append(d.getExpansion());
        sb.append("\\)/SetFuncStatusCode\\(\\1, ");
        sb.append(d.getName().toString());
        sb.append(" \\/\\* W9902 ");
        sb.append(d.getExpansion());
        sb.append(" \\*\\/ \\)/\n");
        StreamHelper.write(ostream, sb);
    }
}

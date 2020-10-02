package de.spurtikus.clangpostproc;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class FuncArgsProcessor {

    // Initial list of well known functions
    private static String[] initialKnownFunctions = { "GetFuncStatusCode", "GetErrorStatus",
            "ClearFuncStatusCode","TestFuncStatusAndPtr", "RdEngOption"};
    // Increasing list of functions "known", i.e. they use known functions and thus are
    // also considered "known"
    private static List<String> knownFunctions = new ArrayList<>();
    private static int ctrChanged;
    private static int ctrUnknown;

    public static void processFunctions(IASTTranslationUnit translationUnit, OutputStream ostream, String structToAnalyze)
            throws ExpansionOverlapsBoundaryException, IOException {
        initializeKnownFunctions();
        IASTDeclaration[] decl = translationUnit.getDeclarations();
        //log.debug(decl.length);
        ctrChanged = 0;
        ctrUnknown = 0;
        for (IASTDeclaration d: decl) {
            //log.debug(d.getChildren().length);
            IASTNode[] childs = d.getChildren();
            Class<? extends IASTDeclaration> dClass = d.getClass();
            //log.debug(dClass.getCanonicalName());

            if (d instanceof CPPASTFunctionDefinition) {
                CPPASTFunctionDefinition function = (CPPASTFunctionDefinition) d;
                //log.debug(function.getDeclarator().getName());
                //if (function.getDeclarator().getName().toString().equals("SetFuncStatusCode")) {
                //log.debug(function.getRawSignature());
                processFunction(function, ostream, structToAnalyze);
                //}
            }
        }
        log.info("Changed {} functions", ctrChanged);

        // Round 2
        for (IASTDeclaration d: decl) {
            IASTNode[] childs = d.getChildren();
            Class<? extends IASTDeclaration> dClass = d.getClass();

            if (d instanceof CPPASTFunctionDefinition) {
                CPPASTFunctionDefinition function = (CPPASTFunctionDefinition) d;
                if (!knownFunctions.contains(function.getDeclarator().getName().toString())) {
                    processFunction(function, ostream, structToAnalyze);
                }
            }
        }
        log.info("Changed {} functions (round 2)", ctrChanged);

        // dump unknown functions
        log.info("List of functions still unknown");
        for (IASTDeclaration d: decl) {
            IASTNode[] childs = d.getChildren();
            Class<? extends IASTDeclaration> dClass = d.getClass();

            if (d instanceof CPPASTFunctionDefinition) {
                CPPASTFunctionDefinition function = (CPPASTFunctionDefinition) d;
                if (!knownFunctions.contains(function.getDeclarator().getName().toString())) {
                    log.info(function.getDeclarator().getName().toString());
                    ctrUnknown++;
                }
            }
        }
        log.info("{} unknown functions in total", ctrUnknown);
    }

    private static void initializeKnownFunctions() {
        Arrays.stream(initialKnownFunctions).forEach( f -> knownFunctions.add(f));
    }

    public static void processFunction(CPPASTFunctionDefinition f, OutputStream ostream, String structToAnalyze)
            throws ExpansionOverlapsBoundaryException, IOException {
        int index = 0;

        IASTFunctionDeclarator declarator = f.getDeclarator();
        //log.debug(declarator.getRawSignature());
        List<String> functionArgs = new ArrayList<>();
        for (IASTNode c: declarator.getChildren()) {
            //log.debug(c.getClass());
            if (c instanceof CPPASTParameterDeclaration) {
                CPPASTParameterDeclaration cc = (CPPASTParameterDeclaration) c;
                //log.debug(cc.getDeclSpecifier().getRawSignature());
                functionArgs.add(cc.getDeclarator().getName().toString());
            }
        }
        log.debug("Function has these args: {}", declarator.getName() );
        functionArgs.forEach(arg -> log.debug( arg + ", "));
        log.debug("");
        String firstArg = functionArgs.get(0);
        String argumentName = getFirstArgumentChangeable(f, index, firstArg);

        if (argumentName != null && argumentName != "null") {
            log.debug("Changeable arg found: {}", argumentName);
            writeSedLine(f, declarator, argumentName, ostream, structToAnalyze);

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
        //log.debug(/*indent + ": " +*/ f.getClass().getSimpleName());
        if (f instanceof CPPASTFunctionCallExpression) {
            CPPASTFunctionCallExpression function = (CPPASTFunctionCallExpression) f;
            log.debug(f.getRawSignature());
            String funcName = function.getSyntax().toString();
            //log.debug("funcName: " + funcName);
            IASTInitializerClause[] args = function.getArguments();
            for (IASTInitializerClause arg: args) {
                //log.debug(arg.getClass());
                if (arg instanceof CPPASTIdExpression) {
                    CPPASTIdExpression a = (CPPASTIdExpression) arg;
                    String callArg = a.getName().toString();
                    //log.debug(callArg);
                    if (firstArg.equals(callArg)) {
                        boolean isKnown = knownFunctions.stream().anyMatch(fu -> fu.equals(funcName));
                        if (isKnown) {
                            //log.debug("Function argument '" + callArg + "' reused in known method call");
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
                                     String argumentName, OutputStream ostream,
                                     String structToAnalyze) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("s/");
        sb.append(f.getDeclarator().getName());
        sb.append("\\(");
        createArgumentPart(declarator, sb);
        sb.append("\\)/");
        sb.append(f.getDeclarator().getName());
        sb.append("\\(");
        createArgumentPartChanged(declarator, sb, argumentName, structToAnalyze);
        sb.append("\\)/\n");
        ostream.write(sb.toString().getBytes());
    }

}

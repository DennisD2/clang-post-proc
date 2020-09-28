package de.spurtikus.clangpostproc;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

import java.util.*;

@Slf4j
public class HeaderProcessor {

    /**
     * Check visibility of a node
     * @param node
     * @return
     */
    static boolean isVisible(IASTNode node) {
        IASTNode declator = node.getParent().getParent();
        IASTNode[] children = declator.getChildren();

        for (IASTNode iastNode : children) {
            if ((iastNode instanceof ICPPASTVisibilityLabel)) {
                return 1 == ((ICPPASTVisibilityLabel) iastNode).getVisibility();
            }
        }
        return false;
    }

    /**
     * Dumps out an AST declaration entity
     * @param declaration
     */
    static void dumpDeclaration(IASTDeclaration declaration) {
        if (declaration instanceof CPPASTSimpleDeclaration) {
            String specifier = getSpecifierString(declaration);
            String declarator = getDeclaratorString(declaration);
            System.out.println(specifier + "\t" + declarator);
        }
        if (declaration instanceof CPPASTProblemDeclaration) {
            CPPASTProblemDeclaration pd = (CPPASTProblemDeclaration) declaration;
            System.out.println("ERROR in declaration: " + pd.getRawSignature());
        }
    }

    /**
     * Gets Specifier for a declaration. For "int var" this would be: "int".
     * @param declaration
     * @return
     */
    static String getSpecifierString(IASTDeclaration declaration) {
        String result = "?";
        if (declaration instanceof CPPASTSimpleDeclaration) {
            CPPASTSimpleDeclaration sd = (CPPASTSimpleDeclaration) declaration;
            IASTDeclSpecifier ds = sd.getDeclSpecifier(); // int16
            result = ds.getRawSignature();
        }
        return result;
    }

    /**
     * Gets Declarator for a declaration. For "int var" this would be: "var".
     * @param declaration
     * @return
     */
    static String getDeclaratorString(IASTDeclaration declaration) {
        StringBuilder result = new StringBuilder();
        if (declaration instanceof CPPASTSimpleDeclaration) {
            CPPASTSimpleDeclaration sd = (CPPASTSimpleDeclaration) declaration;
            IASTDeclarator[] d = sd.getDeclarators(); // op_mode
            Arrays.stream(d).forEach(decl -> result.append(decl.getName()));
        }
        return result.toString();
    }

    /**
     * Dumps all Structs found to STDOUT
     * @param node
     * @param index
     */
    static void dumpStructs(IASTNode node, int index) {
        IASTNode[] children = node.getChildren();

        String offsetString = "";
        try {
            offsetString = node.getSyntax() != null ? " (offset: " + node.getFileLocation().getNodeOffset() + ", len: "
                    + node.getFileLocation().getNodeLength() + ")" : "";
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
        //System.out.println("R>" + rawSignature);

        if (node instanceof CPPASTSimpleDeclaration) {
            CPPASTSimpleDeclaration n = (CPPASTSimpleDeclaration) node;

            IASTNode[] childs = n.getChildren();
            if (childs.length == 2) {
                if (childs[0] instanceof CPPASTElaboratedTypeSpecifier && childs[1] instanceof CPPASTDeclarator) {
                    CPPASTElaboratedTypeSpecifier typeSpecifier = (CPPASTElaboratedTypeSpecifier) childs[0];
                    CPPASTDeclarator declarator = (CPPASTDeclarator) childs[1];
                    System.out.print("[T0]" + typeSpecifier.getRawSignature() + "\t");
                    System.out.println(declarator.getName());
                } else {
                    if (childs[0] instanceof CPPASTCompositeTypeSpecifier && childs[1] instanceof CPPASTDeclarator) {
                        CPPASTCompositeTypeSpecifier typeSpecifier = (CPPASTCompositeTypeSpecifier) childs[0];
                        CPPASTDeclarator declarator = (CPPASTDeclarator) childs[1];
                        System.out.println("[T1]" + n.getDeclSpecifier() + "\t" + declarator.getName());

                        IASTDeclaration[] declarations = typeSpecifier.getDeclarations(true);
                        Arrays.stream(declarations).forEach(d -> dumpDeclaration(d));
                    } else {
                        System.out.println("[X0] Ignoring node with class " + n.getClass().getSimpleName()
                                + " and specifier " + n.getDeclSpecifier());
                    }
                }
            } else {
                System.out.println("[X1]" + ", sig: " + n.getRawSignature());
                IASTDeclarator[] d = n.getDeclarators();
                IASTDeclSpecifier ds = n.getDeclSpecifier();
                IASTAttribute[] a = n.getAttributes();
                IASTAttributeSpecifier[] as = n.getAttributeSpecifiers();
                IASTNode[] c = n.getChildren();
                System.out.println("Children: " + c.length);
            }
        }

        for (IASTNode iastNode : children)
            dumpStructs(iastNode, index + 1);
    }

    /**
     * Returns list of all structs found
     * @param node
     * @return
     */
    public static List<String> getStructNames(IASTNode node) {
        ArrayList structs = new ArrayList<>();
        int index = 1;
        getStructNamesInternal(node, index, structs);
        return structs;
    }

    protected static void getStructNamesInternal(IASTNode node, int index, ArrayList structs) {
        IASTNode[] children = node.getChildren();

        String offsetString = "";
        try {
            offsetString = node.getSyntax() != null ? " (offset: " + node.getFileLocation().getNodeOffset() + ", len: "
                    + node.getFileLocation().getNodeLength() + ")" : "";
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
        //System.out.println("R>" + rawSignature);

        if (node instanceof CPPASTSimpleDeclaration) {
            CPPASTSimpleDeclaration n = (CPPASTSimpleDeclaration) node;

            IASTNode[] childs = n.getChildren();
            if (childs.length == 2) {
                if (childs[0] instanceof CPPASTElaboratedTypeSpecifier && childs[1] instanceof CPPASTDeclarator) {
                    CPPASTElaboratedTypeSpecifier typeSpecifier = (CPPASTElaboratedTypeSpecifier) childs[0];
                    CPPASTDeclarator declarator = (CPPASTDeclarator) childs[1];
                    log.info("[T0]" + typeSpecifier.getRawSignature() + "\t" + declarator.getName());
                    structs.add(typeSpecifier.getRawSignature());
                    structs.add(declarator.getName().toString());
                } else {
                    if (childs[0] instanceof CPPASTCompositeTypeSpecifier && childs[1] instanceof CPPASTDeclarator) {
                        CPPASTCompositeTypeSpecifier typeSpecifier = (CPPASTCompositeTypeSpecifier) childs[0];
                        CPPASTDeclarator declarator = (CPPASTDeclarator) childs[1];
                        log.info(("[T1]" + n.getDeclSpecifier() + "\t" + declarator.getName()));

                        IASTDeclaration[] declarations = typeSpecifier.getDeclarations(true);
                        //Arrays.stream(declarations).forEach(d -> dumpDeclaration(d));
                        structs.add(typeSpecifier.getName().toString());
                        structs.add(declarator.getName().toString());
                    } else {
                        log.debug("[X0] Ignoring node with class " + n.getClass().getSimpleName()
                                + " and specifier " + n.getDeclSpecifier());
                    }
                }
            } else {
                log.debug("[X1]" + ", sig: " + n.getRawSignature());
                IASTDeclarator[] d = n.getDeclarators();
                IASTDeclSpecifier ds = n.getDeclSpecifier();
                IASTAttribute[] a = n.getAttributes();
                IASTAttributeSpecifier[] as = n.getAttributeSpecifiers();
                IASTNode[] c = n.getChildren();
                log.debug("Children: " + c.length);
            }
        }

        for (IASTNode iastNode : children)
            getStructNamesInternal(iastNode, index + 1, structs);
    }

    /**
     * Returns Node pointing to a AST entity for a struct. Struct is defined by its name.
     * @param node
     * @param structName
     * @return
     */
    protected static IASTNode findStruct(IASTNode node, String structName) {
        IASTNode[] children = node.getChildren();

        if (node instanceof CPPASTSimpleDeclaration) {
            CPPASTSimpleDeclaration n = (CPPASTSimpleDeclaration) node;

            IASTNode[] childs = n.getChildren();
            if (childs.length == 2) {
                /*if (childs[0] instanceof CPPASTElaboratedTypeSpecifier && childs[1] instanceof CPPASTDeclarator) {
                    CPPASTElaboratedTypeSpecifier typeSpecifier = (CPPASTElaboratedTypeSpecifier) childs[0];
                    CPPASTDeclarator declarator = (CPPASTDeclarator) childs[1];
                    log.info("[T0]" + typeSpecifier.getRawSignature() + "\t" + declarator.getName());
                } else {*/
                    if (childs[0] instanceof CPPASTCompositeTypeSpecifier && childs[1] instanceof CPPASTDeclarator) {
                        CPPASTCompositeTypeSpecifier typeSpecifier = (CPPASTCompositeTypeSpecifier) childs[0];
                        CPPASTDeclarator declarator = (CPPASTDeclarator) childs[1];
                        log.info(("[CHECKING]" + n.getDeclSpecifier() + "\t" + declarator.getName()));
                        if (structName.equals(declarator.getName().toString())) {
                            log.info(("[FOUND]" + n.getDeclSpecifier() + "\t" + declarator.getName()));
                            return childs[0] ;
                        }
                    } else {
                        log.debug("[X0] Ignoring node with class " + n.getClass().getSimpleName()
                                + " and specifier " + n.getDeclSpecifier());
                    }
                /*}*/
            } else {
                log.debug("[X1]" + ", sig: " + n.getRawSignature());
                IASTDeclarator[] d = n.getDeclarators();
                IASTDeclSpecifier ds = n.getDeclSpecifier();
                IASTAttribute[] a = n.getAttributes();
                IASTAttributeSpecifier[] as = n.getAttributeSpecifiers();
                IASTNode[] c = n.getChildren();
                log.debug("Children: " + c.length);
            }
        }

        for (IASTNode iastNode : children) {
            IASTNode found = findStruct(iastNode, structName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Returns all fields of a struct with declarator and specifier.
     * For e.g. "int var;", specifier is "int" and declarator is "var".
     *
     * @param translationUnit
     * @param structName
     * @return
     */
    public static Map<String, FieldInfo> getStructInfo(IASTTranslationUnit translationUnit, String structName) {
        IASTNode node = findStruct(translationUnit, structName);
        LinkedHashMap<String, FieldInfo> map = new LinkedHashMap<>();
        if (node instanceof CPPASTCompositeTypeSpecifier) {
            CPPASTCompositeTypeSpecifier n = (CPPASTCompositeTypeSpecifier) node;
            Arrays.stream(n.getDeclarations(true)).forEach(declaration -> {
                FieldInfo fieldInfo = new FieldInfo(getSpecifierString(declaration), 0);
                map.put(getDeclaratorString(declaration), fieldInfo);
            });
        }
        return map;
    }

    /**
     * Align datatype to correct byte boundary
     * @param dataTypeSize
     * @param offset
     * @return
     */
    static int recalcOffset(int dataTypeSize, int offset) {
        if (dataTypeSize==0)
            return offset;

        if (offset % dataTypeSize == 0) {
            return offset;
        }
        int newOffset = (offset/dataTypeSize+1)*(dataTypeSize);
        log.debug("Fixing offset " + offset +
                " to " + newOffset + " due to data type boundary " + dataTypeSize);
        return newOffset;
    }
}

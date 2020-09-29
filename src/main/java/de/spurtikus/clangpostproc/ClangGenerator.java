package de.spurtikus.clangpostproc;


import lombok.extern.slf4j.Slf4j;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.core.runtime.CoreException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ClangGenerator {

      public static void main(String[] args)  {
          if (args.length != 2) {
              log.error("Please provide C input file and sed output file name.");
              log.error("Example:");
              log.error("java -jar <farfile>.jar str_9052.h struct2.sed");
              log.error("Exiting");
              return;
          }
          log.info("C Input file: " + args[0]);
          log.info("sed Output file: " + args[1]);
          try {
              processFile( args[0], args[1]);
          } catch (CoreException e) {
              e.printStackTrace();
              log.error("Core Execption", e);
          } catch (IOException e) {
              log.error("IO Execption", e);
              e.printStackTrace();
          }
      }

    /**
     * Analyzes file and creates sed commands in a file
     *
     * @param inFileName input CLang file
     * @param outFileName output file with sed commands
     * @throws CoreException
     * @throws IOException
     */
    protected static void processFile(String inFileName, String outFileName) throws CoreException, IOException {
        // Create translation unit
        IASTTranslationUnit translationUnit = getIastTranslationUnit(inFileName);

        // create data type sizes map (CPU dependent)
        Map<String, Integer> dataSizes = createDataSizeMap();

        // process struct
        String structToAnalyze = "SET9052";
        processStruct(structToAnalyze, outFileName, translationUnit, dataSizes);
    }

    /**
     * Creates sed command file for a struct.
     *
     * @param outFileName
     * @param translationUnit
     * @throws IOException
     */
    private static void processStruct(String structName, String outFileName,
                                      IASTTranslationUnit translationUnit,
                                      Map<String, Integer> dataSizes) throws IOException {
        Map<String,FieldInfo> structInfo = HeaderProcessor.getStructInfo(translationUnit, structName);

        OutputStream ostream = null;
        try {
            ostream = new FileOutputStream(outFileName);
        } catch (FileNotFoundException e) {
            System.out.println("Cannot write file");
            e.printStackTrace();
        }

        int offset = 0;
        for (String si : structInfo.keySet()) {
            FieldInfo fieldInfo = structInfo.get(si);
            offset = HeaderProcessor.recalcOffset(dataSizes.get(fieldInfo.getSpecifier()), offset);
            fieldInfo.setOffset(offset);

            int fieldSize = getFieldSize(dataSizes, si, fieldInfo);
            printSedLines(offset, si, fieldInfo, ostream);
            offset += fieldSize;
        }
        ostream.flush();
        ostream.close();
    }

    /**
     * Get translation unit from file content
     *
     * @param inFileName file to scan
     * @return
     * @throws CoreException
     */
    public static IASTTranslationUnit getIastTranslationUnit(String inFileName) throws CoreException {
        FileContent fileContent = FileContent.createForExternalFileLocation(inFileName);
        Map definedSymbols = new HashMap();
        String[] includePaths = new String[0];
        IScannerInfo info = new ScannerInfo(definedSymbols, includePaths);
        IParserLogService log = new DefaultLogService();
        IncludeFileContentProvider emptyIncludes = IncludeFileContentProvider.getEmptyFilesProvider();
        int opts = 8;
        IASTTranslationUnit translationUnit = GPPLanguage.getDefault().getASTTranslationUnit(fileContent,
                info, emptyIncludes, null, opts, log);
        return translationUnit;
    }

    /**
     * Get size of a struct field
     * Uses dataSizes map as input for basic data type sizes
     *
     * TODO: should be read from file to allow user to change it
     * @param dataSizes
     * @param si
     * @param fieldInfo
     * @return
     */
    public static int getFieldSize(Map<String, Integer> dataSizes, String si, FieldInfo fieldInfo) {
        int fieldSize;
        if (si.equals("serialErrs")) {
            fieldSize = 2*6*4;
        } else {
            fieldSize = dataSizes.get(fieldInfo.getSpecifier());
        }

        if (fieldSize==0) {
            if (si.equals("sessionString")) {
                fieldSize=256;
            }
            if (si.equals("commPhoneNum")) {
                fieldSize=50;
            }
            if (si.equals("commInitString")) {
                fieldSize=50;
            }
            if (si.equals("baseAddr")) {
                fieldSize=4;
            }
        }
        return fieldSize;
    }

    /**
     * Create map for sizes of data types
     *
     * TODO: should be read from file to allow user to change it
     *
     * @return
     */
    public static Map<String, Integer> createDataSizeMap() {
        Map<String, Integer> dataSizes = new HashMap<>();
        dataSizes.put("char", 0); // n chars are n bytes
        dataSizes.put("int16", 2);
        dataSizes.put("uint16", 2);
        dataSizes.put("int", 4);
        dataSizes.put("int32", 4);
        dataSizes.put("uint32", 4);
        dataSizes.put("void", 4); // void* = 4 bytes
        dataSizes.put("USECS", 4);
        dataSizes.put("SET9052LIB", 4); // assumption
        dataSizes.put("double", 8);
        dataSizes.put("FREQ8500", 8);

        dataSizes.put("int32_t", 4);
        dataSizes.put("uint32_t", 4);
        dataSizes.put("int16_t", 4);
        dataSizes.put("uint32_t", 4);
        return dataSizes;
    }

    /**
     *   sed -E -e "s/\((.[0-9]) \+ ([0-9]*)\)/\&\(\1->\2\) \/\* W4321-01 \1\+\2 \*\//"
     *   <src/test/resources/morrow/mtcsa32.dll.c |grep W4321|more
     *
     * @param offset
     * @param si
     * @param fieldInfo
     * @param outputStream
     * @throws IOException
     */
    public static void printSedLines(int offset, String si, FieldInfo fieldInfo,
                                     OutputStream outputStream) throws IOException {
        StringBuilder sb = new StringBuilder();

            sb.append("s/\\(([a-z0-9]*[0-9]) \\+ ");
            sb.append(offset);
            sb.append("\\)");
            sb.append("/\\&\\(\\1->");
            sb.append(si);
            sb.append("\\) \\/\\* W9901 \\1\\+");
            sb.append(offset);
            sb.append(" \\*\\//g\n");
            outputStream.write(sb.toString().getBytes());
    }

}

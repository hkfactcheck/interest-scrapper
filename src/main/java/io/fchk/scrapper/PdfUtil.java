package io.fchk.scrapper;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PdfUtil {

    static {
        System.setProperty("org.apache.commons.logging.Log",
                           "org.apache.commons.logging.impl.NoOpLog");
    }

    public static void main(String[] args) throws Exception {

        ((ch.qos.logback.classic.LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.apache").setLevel(Level.OFF);
        Workbook workbook = WorkbookFactory.create(new File("out.xlsx"));
        Iterator<Sheet> sheetIterator = workbook.sheetIterator();
        Map<String, Declaration> declarations = new HashMap<>();
        while (sheetIterator.hasNext()) {

            Sheet sheet = sheetIterator.next();
            String region = sheet.getSheetName();
            for (Row row : sheet) {
                if(row.getCell(3).getStringCellValue().contains("文字版本")) {
                    String name = row.getCell(1).getStringCellValue();
                    String url = row.getCell(4).getStringCellValue();
                    log.info(url);
                    try {
                        Declaration declaration = parsePdf(url);
                        declaration.setPersonName(name);
                        declaration.setRegion(region);
                        if (null != declaration) {
                            declarations.put(url, declaration);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }

            }
        }
        new ObjectMapper().writeValue(new File("out.json"), declarations);

    }

    private static Declaration parsePdf(String url) throws Exception {

        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();
        File pdfOut = new File("out.pdf");
        FileUtils.copyURLToFile(new URL(url), pdfOut);
        PDFParser parser = new PDFParser(new RandomAccessFile(pdfOut, "r"));
        parser.parse();

        PDFTextStripper pdfStripper = new PDFTextStripper();
        Declaration declaration;
        try(COSDocument cosDoc = parser.getDocument();
            PDDocument pdDoc = new PDDocument(cosDoc)){
            Splitter splitter = new Splitter();
            List<PDDocument> documentPages = splitter.split(pdDoc);
            ArrayListMultimap<Integer, Pair<String, PDDocument>> pages = ArrayListMultimap.create();
            for (PDDocument p : documentPages) {
                String parsedText = pdfStripper.getText(p);
                Integer pageNumber = getPageNumber(parsedText);
                if (null != pageNumber) {
                    pages.put(pageNumber, Pair.of(parsedText, p));
                }else{
                    pages.put(0, Pair.of(parsedText, p));
                }
            }
            declaration = parsePages(pages);
            for(PDDocument p: documentPages){
                p.close();
            }
        }
        return declaration;


    }

    private static Declaration parsePages(ArrayListMultimap<Integer, Pair<String, PDDocument>> pages){
        Declaration declaration = new Declaration();
        outerLoop:
        for(Map.Entry<Integer, Pair<String, PDDocument>> entry: pages.entries()){
            for (PageParser parser: PageParser.values()){
                if(parser.recognize(entry.getValue())){
                    parser.parse(entry.getValue(), declaration);
                    continue outerLoop;
                }
            }
            log.warn("Unable to find parser for page " + entry.getKey());
        }
        return declaration;
    }

    private static Integer getPageNumber(String input) {

        Pattern pageNoPattern = Pattern.compile("-\\s*(\\d+)\\s*-", Pattern.DOTALL);
        Matcher m = pageNoPattern.matcher(input);
        if (m.find()) {
            return Integer.valueOf(m.group(1));
        }
        return null;
    }
}

package io.fchk.scrapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to extract tabular data from a PDF.
 * Works by making a first pass of the page to group all nearby text items
 * together, and then inferring a 2D grid from these regions. Each table cell
 * is then extracted using a PDFTextStripperByArea object.
 * <p>
 * Works best when
 * headers are included in the detected region, to ensure representative text
 * in every column.
 * <p>
 * Based upon DrawPrintTextLocations PDFBox example
 * (https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/util/DrawPrintTextLocations.java)
 *
 * @author Beldaz
 */
public class PDFTableStripper extends PDFTextStripper {

    private double c1x;
    private double c2x;
    private boolean btm=false;
    private double currenty;
    private String leftColumnHeader;
    private String rightColumnHeader;
    private String bottomText;
    private List<Pair<String,String>> cellText = new ArrayList<>();

    /*
     *  Used in methods derived from DrawPrintTextLocations
     */
    private AffineTransform flipAT;
    private AffineTransform rotateAT;

    /**
     * Regions updated by calls to writeString
     */
    private Set<Rectangle2D> boxes;


    /**
     * Region in which to find table (otherwise whole page)
     */
    private Rectangle2D regionArea;

    /**
     * Number of rows in inferred table
     */
    private int nRows = 0;

    /**
     * Number of columns in inferred table
     */
    private int nCols = 0;

    /**
     * This is the object that does the text extraction
     */
    private PDFTextStripperByArea regionStripper;

    /**
     * 1D intervals - used for calculateTableRegions()
     *
     * @author Beldaz
     */
    public static class Interval {
        double start;
        double end;

        public Interval(double start, double end) {

            this.start = start;
            this.end = end;
        }

        public void add(Interval col) {

            if (col.start < start)
                start = col.start;
            if (col.end > end)
                end = col.end;
        }

        public static void addTo(Interval x, LinkedList<Interval> columns) {

            int p = 0;
            Iterator<Interval> it = columns.iterator();
            // Find where x should go
            while (it.hasNext()) {
                Interval col = it.next();
                if (x.end >= col.start) {
                    if (x.start <= col.end) { // overlaps
                        x.add(col);
                        it.remove();
                    }
                    break;
                }
                ++p;
            }
            while (it.hasNext()) {
                Interval col = it.next();
                if (x.start > col.end)
                    break;
                x.add(col);
                it.remove();
            }
            columns.add(p, x);
        }

    }


    public PDFTableStripper(String leftColumnHeader, String rightColumnHeader, String bottomText) throws IOException {

        super.setShouldSeparateByBeads(false);
        regionStripper = new PDFTextStripperByArea();
        regionStripper.setSortByPosition(true);
        this.leftColumnHeader = leftColumnHeader;
        this.rightColumnHeader = rightColumnHeader;
        this.bottomText = bottomText;
    }

    /**
     * Define the region to group text by.
     *
     * @param rect The rectangle area to retrieve the text from.
     */
    public void setRegion(Rectangle2D rect) {

        regionArea = rect;
    }

    public int getRows() {

        return nRows;
    }

    public int getColumns() {

        return nCols;
    }

    public List<Pair<String,String>> getCellText() {
        return cellText.stream().filter(p->!(StringUtils.isBlank(p.getLeft()) && StringUtils.isBlank(p.getRight()))).collect(Collectors.toList());
    }

    /**
     * Get the text for the region, this should be called after extractTable().
     *
     * @return The text that was identified in that region.
     */
    public String getText(int row, int col) {

        return regionStripper.getTextForRegion("el" + col + "x" + row);
    }

    public void extractTable(PDPage pdPage) throws IOException {

        setStartPage(getCurrentPageNo());
        setEndPage(getCurrentPageNo());

        boxes = new HashSet<Rectangle2D>();
        // flip y-axis
        flipAT = new AffineTransform();
        flipAT.translate(0, pdPage.getBBox().getHeight());
        flipAT.scale(1, -1);

        // page may be rotated
        rotateAT = new AffineTransform();
        int rotation = pdPage.getRotation();
        if (rotation != 0) {
            PDRectangle mediaBox = pdPage.getMediaBox();
            switch (rotation) {
                case 90:
                    rotateAT.translate(mediaBox.getHeight(), 0);
                    break;
                case 270:
                    rotateAT.translate(0, mediaBox.getWidth());
                    break;
                case 180:
                    rotateAT.translate(mediaBox.getWidth(), mediaBox.getHeight());
                    break;
                default:
                    break;
            }
            rotateAT.rotate(Math.toRadians(rotation));
        }
        // Trigger processing of the document so that writeString is called.
        try (Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream())) {
            super.output = dummy;
            super.processPage(pdPage);
        }


    }



    private StringBuilder l = new StringBuilder();
    private StringBuilder r = new StringBuilder();
    private double lastY;

    /**
     * Register each character's bounding box, updating boxes field to maintain
     * a list of all distinct groups of characters.
     * <p>
     * Overrides the default functionality of PDFTextStripper.
     * Most of this is taken from DrawPrintTextLocations.java, with extra steps
     * at end of main loop
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        if(string.length()>=1) {
            if (btm) {
                //Table end is found
                return;
            }
            if (string.replace(" ","").contains(leftColumnHeader)) {
                c1x = textPositions.get(0).getX()-3;
                return;
            }
            if (string.replace(" ","").contains(rightColumnHeader)) {
                int pos = string.indexOf(rightColumnHeader);
                c2x = textPositions.get(pos).getX()-3;
                return;
            }
            if (string.replace(" ","").contains(bottomText)) {
                btm = true;
                cellText.add(Pair.of(l.toString().trim(), r.toString().trim()));
                return;
            }
            if (c1x > 0 && c2x > 0) {

                for (TextPosition p : textPositions) {
                    if(lastY==0){
                        lastY = p.getY();
                    }else if (p.getY() - lastY >10){
                        //new line
                        cellText.add(Pair.of(l.toString().trim(), r.toString().trim()));
                        l = new StringBuilder();
                        r = new StringBuilder();
                        lastY = p.getY();
                    }
                    if(p.getX()<c2x){
                        l.append(p.getUnicode());
                    }else{
                        r.append(p.getUnicode());
                    }
                }
//                if (l.length() > 0 || r.length() > 0) {
//                    if(l.length()==0 && cellText.get(cellText.size() - 1).getRight().isEmpty()){
//                        Pair<String, String> pair = cellText.remove(cellText.size() - 1);
//                        cellText.add( Pair.of(pair.getLeft(), r.toString()));
//                    }else{
//                        cellText.add(Pair.of(l.toString(), r.toString()));
//                    }
//
//                }
            }
        }
    }

    /**
     * This method does nothing in this derived class, because beads and regions are incompatible. Beads are
     * ignored when stripping by area.
     *
     * @param aShouldSeparateByBeads The new grouping of beads.
     */
    @Override
    public final void setShouldSeparateByBeads(boolean aShouldSeparateByBeads) {

    }

    /**
     * Adapted from PDFTextStripperByArea
     * {@inheritDoc}
     */
    @Override
    protected void processTextPosition(TextPosition text) {

        if (regionArea != null && !regionArea.contains(text.getX(), text.getY())) {
            // skip character
        } else {
            super.processTextPosition(text);
        }
    }
}
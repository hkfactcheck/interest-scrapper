package io.fchk.scrapper;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public enum PageParser {

    Q11(new String[][]{{"第1類", "受薪東主、合夥人或董事職位", "你有否擔任公共或私營公司的受薪東主"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern ynQuestion = Pattern.compile("合夥人或董事職位？(.*?)請在合適空格內劃", Pattern.DOTALL);
            Matcher m = ynQuestion.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                String answer = RegExUtils.removeAll(m.group(1), " ");
                if (answer.contains("有" + TICK)) {
                    declaration.setQ11Yn(true);
                } else if (answer.contains("否" + TICK)) {
                    declaration.setQ11Yn(false);
                }
            }

            Pattern companyPositionPattern = Pattern.compile("公司名稱(.*?)該公司的業務性質(.*?)身份(.*?)該公司的所有控權公司.*?如有的話(.*?)" + "\\(若你有更多受薪東主", Pattern.DOTALL);
            Matcher m1 = companyPositionPattern.matcher(removeLineBreak(content.getLeft()));
            if (m1.find()) {
                Declaration.CompanyPosition cp = new Declaration.CompanyPosition();
                cp.setCompanyName(removeNonWord(m1.group(1)));
                if (StringUtils.isBlank(cp.getCompanyName())) {
                    return;
                }
                cp.setNature(removeNonWord(m1.group(2)));
                String answer = RegExUtils.removeAll(m1.group(3), " ");
                List<String> positions = new ArrayList<>();
                if (answer.contains(TICK + "東主")) {
                    positions.add("東主");
                } else if (answer.contains(TICK + "合夥人")) {
                    positions.add("合夥人");
                } else if (answer.contains(TICK + "董事")) {
                    positions.add("董事");
                } else if (answer.contains(TICK + "其他(請註明)")) {
                    positions.add("其他: " + answer.split("其他\\(請註明\\)")[1]);
                }
                cp.setPosition(positions);
                cp.setHolding(removeNonWord(m1.group(4)));
                declaration.getQ11Table().add(cp);
            }
        }
    }, Q11_2(new String[][]{{"詳細資料", "公司名稱", "該公司的業務性質", "若你有更多受薪東主、合夥人或董事職位須登記"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern companyPositionPattern = Pattern.compile("公司名稱(.*?)該公司的業務性質(.*?)身份(.*?)該公司的所有控權公司.*?如有的話(.*?)" + "\\(若你有更多受薪東主", Pattern.DOTALL);
            Matcher m1 = companyPositionPattern.matcher(removeLineBreak(content.getLeft()));
            if (m1.find()) {
                Declaration.CompanyPosition cp = new Declaration.CompanyPosition();
                cp.setCompanyName(removeNonWord(m1.group(1)));
                if (StringUtils.isBlank(cp.getCompanyName())) {
                    return;
                }
                cp.setNature(removeNonWord(m1.group(2)));
                String answer = StringUtils.removeAll(m1.group(3), " ");
                List<String> positions = new ArrayList<>();
                if (answer.contains(TICK + "東主")) {
                    positions.add("東主");
                } else if (answer.contains(TICK + "合夥人")) {
                    positions.add("合夥人");
                } else if (answer.contains(TICK + "董事")) {
                    positions.add("董事");
                } else if (answer.contains(TICK + "其他(請註明)")) {
                    positions.add("其他: " + answer.split("其他\\(請註明\\)")[1]);
                }
                cp.setPosition(positions);
                cp.setHolding(removeNonWord(m1.group(4)));
                declaration.getQ11Table().add(cp);
            }
        }
    }, Q11_3(new String[][]{{"第1類", "受薪東主、合夥人或董事職位", "續上頁", "如有需要，請影印本頁並在每頁簽署"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern companyPositionPattern = Pattern.compile("公司名稱(.*?)該公司的業務性質(.*?)身份(.*?)該公司的所有控權公司.*?如有的話(.*?)"
                                                             + "公司名稱(.*?)該公司的業務性質(.*?)身份(.*?)該公司的所有控權公司.*?如有的話(.*?)"
                                                             + "公司名稱(.*?)該公司的業務性質(.*?)身份(.*?)該公司的所有控權公司.*?如有的話(.*?)"
                                                             + "公司名稱(.*?)該公司的業務性質(.*?)身份(.*?)該公司的所有控權公司.*?如有的話(.*?)"
                                                             + "\\(如有需要，請影印本頁並在每頁簽署", Pattern.DOTALL);
            Matcher m1 = companyPositionPattern.matcher(removeLineBreak(content.getLeft()));
            if (m1.find()) {

                for (int i = 0; i < 4; i++) {
                    Declaration.CompanyPosition cp = new Declaration.CompanyPosition();
                    cp.setCompanyName(removeNonWord(m1.group(i * 4 + 1)));
                    if (StringUtils.isBlank(cp.getCompanyName())) {
                        continue;
                    }
                    cp.setNature(removeNonWord(m1.group(i * 4 + 2)));
                    String answer = StringUtils.removeAll(m1.group(i * 4 + 3), " ");
                    List<String> positions = new ArrayList<>();
                    if (answer.contains(TICK + "東主")) {
                        positions.add("東主");
                    } else if (answer.contains(TICK + "合夥人")) {
                        positions.add("合夥人");
                    } else if (answer.contains(TICK + "董事")) {
                        positions.add("董事");
                    } else if (answer.contains(TICK + "其他(請註明)")) {
                        positions.add("其他: " + answer.split("其他\\(請註明\\)")[1]);
                    }
                    cp.setPosition(positions);
                    cp.setHolding(removeNonWord(m1.group(i * 4 + 4)));
                    declaration.getQ11Table().add(cp);
                }

            }
        }
    }, Q12(new String[][]{{"第1類", "受薪東主、合夥人或董事職位", "如你在本屆任期內終止擔任任何已登記公司的受薪東主"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern allText = Pattern.compile("公司名稱(.*?)如有需要，請影印本頁", Pattern.DOTALL);
            Matcher m = allText.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                declaration.setQ12FreeText(Strings.nullToEmpty(declaration.getQ12FreeText()) + removeNonWord(m.group(1)));
            }
        }
    }, Q21_1(new String[][]{{"第2類", "受薪工作及職位等", "你有否從事受薪的工作，包括所有獲得薪金"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern ynQuestion = Pattern.compile("區會議員一職除外(.*?)請在合適空格內劃", Pattern.DOTALL);
            Matcher m = ynQuestion.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                String answer = StringUtils.removeAll(m.group(1), " ");
                if (answer.contains("有" + TICK)) {
                    declaration.setQ21Yn(true);
                } else if (answer.contains("否" + TICK)) {
                    declaration.setQ21Yn(false);
                }
            }
            try {
                PDFTableStripper stpr = new PDFTableStripper("受薪工作、職位、行業或專業的名稱", "公司的業務性質", "若你有更多受薪的工作");
                stpr.setSortByPosition(true);
                stpr.setRegion(new Rectangle(30, 392, 536, 300));
                stpr.extractTable(content.getRight().getPage(0));
                declaration.getQ21Table().addAll(stpr.getCellText());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }, Q21_2(new String[][]{{"第2類", "受薪工作及職位等", "續上頁", "如有需要，請影印本頁並在每頁簽署"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            try {
                PDFTableStripper stpr = new PDFTableStripper("受薪工作、職位、行業或專業的名稱", "公司的業務性質", "如有需要，請影印本頁並在每頁簽署");
                stpr.setSortByPosition(true);
                stpr.setRegion(new Rectangle(26, 51, 549, 706));
                stpr.extractTable(content.getRight().getPage(0));
                declaration.getQ21Table().addAll(stpr.getCellText());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }, Q22(new String[][]{{"第2類", "受薪工作及職位等", "如你在本屆任期內終止從事任何已登記的受薪工作"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern allText = Pattern.compile("受薪工作、職位、行業或專業的名稱(.*?)如有需要，請影印本頁", Pattern.DOTALL);
            Matcher m = allText.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                declaration.setQ22FreeText(Strings.nullToEmpty(declaration.getQ22FreeText()) + removeNonWord(m.group(1)));
            }
        }
    }, Q31(new String[][]{{"第3類", "股份", "有否持有任何在香港註冊登記的公司或其他團體的"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern ynQuestion = Pattern.compile("股份總數的百分之一(.*?)請在合適空格內劃", Pattern.DOTALL);
            Matcher m = ynQuestion.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                String answer = StringUtils.removeAll(m.group(1), " ");
                if (answer.contains("有" + TICK)) {
                    declaration.setQ31SharesYn(true);
                } else if (answer.contains("否" + TICK)) {
                    declaration.setQ31SharesYn(false);
                }
            }
            try {
                PDFTableStripper stpr = new PDFTableStripper("公司名稱", "公司業務性質", "若你有更多股份須登記");
                stpr.setSortByPosition(true);
                stpr.setRegion(new Rectangle(19, 373, 557, 323));
                stpr.extractTable(content.getRight().getPage(0));
                declaration.getQ31Table().addAll(stpr.getCellText());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }, Q31_2(new String[][]{{"第3類", "股份", "續上頁", "如有需要，請影印本頁並在每頁簽署"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            try {
                PDFTableStripper stpr = new PDFTableStripper("公司名稱", "公司業務性質", "如有需要，請影印本頁並在每頁簽署");
                stpr.setSortByPosition(true);
                stpr.setRegion(new Rectangle(26, 51, 549, 706));
                stpr.extractTable(content.getRight().getPage(0));
                declaration.getQ31Table().addAll(stpr.getCellText());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }, Q32(new String[][]{{"第3類", "股份", "如你在本屆任期內終止擁有或持有任何已登記公司或團體的股份"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern allText = Pattern.compile("公司名稱(.*?)如有需要，請影印本頁", Pattern.DOTALL);
            Matcher m = allText.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                declaration.setQ32FreeText(Strings.nullToEmpty(declaration.getQ32FreeText()) + removeNonWord(m.group(1)));
            }
        }
    }, Q4(new String[][]{{"第4類", "財政贊助", "來自任何人士或組織的財政贊助"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern ynQuestion = Pattern.compile("其配偶的實惠或實利(.*?)請在合適空格內劃", Pattern.DOTALL);
            Matcher m = ynQuestion.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                String answer = StringUtils.removeAll(m.group(1), " ");
                if (answer.contains("有" + TICK)) {
                    declaration.setQ4Yn(true);
                } else if (answer.contains("否" + TICK)) {
                    declaration.setQ4Yn(false);
                }
            }
            Pattern allText = Pattern.compile("若有的話，請列明詳情。(.*?)簽署", Pattern.DOTALL);
            Matcher m1 = allText.matcher(removeLineBreak(content.getLeft()));
            if (m1.find()) {
                declaration.setQ4FreeText(Strings.nullToEmpty(declaration.getQ4FreeText()) + removeNonWord(m1.group(1)));
            }
        }
    }, Q5(new String[][]{{"第5類", "海外訪問", "旅遊的費用並非全數由該議員"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern ynQuestion = Pattern.compile("其配偶的實惠或實利(.*?)請在合適空格內劃", Pattern.DOTALL);
            Matcher m = ynQuestion.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                String answer = StringUtils.removeAll(m.group(1), " ");
                if (answer.contains("有" + TICK)) {
                    declaration.setQ5Yn(true);
                } else if (answer.contains("否" + TICK)) {
                    declaration.setQ5Yn(false);
                }
            }
            Pattern allText = Pattern.compile("詳細資料.*?贊助人姓名(.*?)訪問日期(.*?)訪問的國家.*?地方(.*?)訪問目的(.*?)參加訪問的理由(.*?)收受利益的性質.*?或膳宿津貼有關(.*?)簽署",
                                              Pattern.DOTALL);
            Matcher m1 = allText.matcher(removeLineBreak(content.getLeft()));
            if (m1.find()) {
                declaration.setQ5SponsorName(removeNonWord(m1.group(1)));
                declaration.setQ5Date(removeNonWord(m1.group(2)));
                declaration.setQ5Region(removeNonWord(m1.group(3)));
                declaration.setQ5Purpose(removeNonWord(m1.group(4)));
                declaration.setQ5Reason(removeNonWord(m1.group(5)));
                declaration.setQ5Benefit(removeNonWord(m1.group(6)));
            }
        }
    }, Q6(new String[][]{{"第6類", "土地及物業", "你在香港是否直接或間接地擁有土地或物業"}, {"你在香港是否直接或間接地擁有土地或物業", "議員/委員會成員只須登記所擁有的土地或物業的一般性質"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern ynQuestion = Pattern.compile("直接或間接地擁有土地或物業(.*?)請在合適空格內劃", Pattern.DOTALL);
            Matcher m = ynQuestion.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                String answer = StringUtils.removeAll(m.group(1), " ");
                if (answer.contains("有" + TICK)) {
                    declaration.setQ6Yn(true);
                } else if (answer.contains("否" + TICK)) {
                    declaration.setQ6Yn(false);
                }
            }
            Pattern allText = Pattern.compile("無需予以登記。(.*?)簽署", Pattern.DOTALL);
            Matcher m1 = allText.matcher(removeLineBreak(content.getLeft()));
            if (m1.find()) {
                declaration.setQ6FreeText(Strings.nullToEmpty(declaration.getQ6FreeText()) + removeNonWord(m1.group(1)));
            }
        }
    }, Q71_1(new String[][]{{"第7類", "客戶", "委員會成員身分或以任何方式與該身分有關而向客戶"}, {"委員會成員身分或以任何方式與該身分有關而向客戶", "並因此收受該客戶付予的薪金、酬金、津貼或其他實惠"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern ynQuestion = Pattern.compile("薪金、酬金、津貼或其他實惠(.*?)請在合適空格內劃", Pattern.DOTALL);
            Matcher m = ynQuestion.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                String answer = StringUtils.removeAll(m.group(1), " ");
                if (answer.contains("有" + TICK)) {
                    declaration.setQ71Yn(true);
                } else if (answer.contains("否" + TICK)) {
                    declaration.setQ71Yn(false);
                }
            }
            try {
                PDFTableStripper stpr = new PDFTableStripper("客戶名稱", "客戶業務性質", "若你有更多客戶須登記");
                stpr.setSortByPosition(true);
                stpr.setRegion(new Rectangle(32, 528, 522, 200));
                stpr.extractTable(content.getRight().getPage(0));
                declaration.getQ71Table().addAll(stpr.getCellText());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }, Q71_2(new String[][]{{"第7類", "客戶", "續上頁", "如有需要，請影印本頁並在每頁簽署"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            try {
                PDFTableStripper stpr = new PDFTableStripper("客戶名稱", "客戶業務性質", "如有需要，請影印本頁並在每頁簽署");
                stpr.setSortByPosition(true);
                stpr.setRegion(new Rectangle(31, 38, 531, 684));
                stpr.extractTable(content.getRight().getPage(0));
                declaration.getQ71Table().addAll(stpr.getCellText());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }, Q72(new String[][]{{"第7類", "客戶", "如你在本屆任期內終止了任何在此類別下的已登記的工作"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern allText = Pattern.compile("公司名稱(.*?)如有需要，請影印本頁", Pattern.DOTALL);
            Matcher m = allText.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                declaration.setQ72FreeText(Strings.nullToEmpty(declaration.getQ72FreeText()) + removeNonWord(m.group(1)));
            }
        }
    }, Q8_1(new String[][]{{"第8類", "其他可供申報的利益", "根據登記個人利益須知所述的目的及兩層申報利益制度指引", "並把有關文件退回"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern allText = Pattern.compile("詳細資料(.*?)簽署", Pattern.DOTALL);
            Matcher m = allText.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                declaration.setQ8FreeText(Strings.nullToEmpty(declaration.getQ8FreeText()) + removeNonWord(m.group(1)));
            }
        }
    }, Q8_2(new String[][]{{"第8類", "其他可供申報的利益", "續上頁", "如有需要，請影印本頁"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern allText = Pattern.compile("委員會成員姓名(.*?)如有需要，請影印本頁並", Pattern.DOTALL);
            Matcher m = allText.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                declaration.setQ8FreeText(Strings.nullToEmpty(declaration.getQ8FreeText()) + removeNonWord(m.group(1)));
            }
        }
    }, Q8_3(new String[][]{{"第8類", "其他", "根據登記個人利益須知所述的目的及兩層申報利益制度指引", "請在下面提供有關詳情"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

            Pattern allText = Pattern.compile("請在下面提供有關詳情。(.*?)簽署", Pattern.DOTALL);
            Matcher m = allText.matcher(removeLineBreak(content.getLeft()));
            if (m.find()) {
                declaration.setQ8FreeText(Strings.nullToEmpty(declaration.getQ8FreeText()) + removeNonWord(m.group(1)));
            }
        }
    }, INSTRUCTION_1(new String[][]{{"填寫登記表格之前，請參閱以下個人利益登記須知。"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

        }
    }, INSTRUCTION_2(new String[][]{{"委員會成員應遵從登記個人利益的規定，登記必須登記的個人利益應被視為最低的合理規定"}}) {
        @Override
        public void parse(Pair<String, PDDocument> content, Declaration declaration) {

        }
    };

    PageParser(String[][] containing) {

        this.containing = containing;
    }


    private String[][] containing;

    public abstract void parse(Pair<String, PDDocument> content, Declaration declaration);

    public boolean recognize(Pair<String, PDDocument> content) {
        String trimmed = removeLineBreak(content.getLeft()).replaceAll(" ", "");
        for (String[] c : containing) {
            boolean matched = true;
            for (String s : c) {
                if (!trimmed.contains(s)) {
                    matched = false;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private static String removeLineBreak(String content) {

        return content.replaceAll("\\n", "").replaceAll("\\r", "");
    }


    public static final String TICK = "\uf0fc";

    private static String removeNonWord(String content) {

        return content.replaceFirst("^[ \\-\\)]+", "").replaceFirst("[ \\-\\(]+$", "");
    }
}

package io.fchk.scrapper;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class Declaration {
    private String region;
    private String personName;
    private Boolean q11Yn;

    List<CompanyPosition> q11Table = new ArrayList<>();

    private String q12FreeText;

    private Boolean q21Yn;

    private List<Pair<String, String>> q21Table = new ArrayList<>();

    private String q22FreeText;

    private Boolean q31SharesYn;
    private List<Pair<String, String>> q31Table = new ArrayList<>();

    private String q32FreeText;

    private Boolean q4Yn;

    private String q4FreeText;

    private Boolean q5Yn;

    private String q5SponsorName;

    private String q5Date;

    private String q5Region;

    private String q5Purpose;

    private String q5Reason;

    private String q5Benefit;


    private Boolean q6Yn;

    private String q6FreeText;

    private Boolean q71Yn;

    private List<Pair<String, String>> q71Table = new ArrayList<>();

    private String q72FreeText;

    private String q8FreeText;





    @Data
    @Accessors(chain = true)
    public static class CompanyPosition{
        String companyName;
        String nature;
        List<String> position;
        String holding;
    }
}

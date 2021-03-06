# interest-scrapper

A simple java program to get and scrap interest declaration PDFs.

## Input 
`out.xlsx`, a spreadsheet file containing links to PDF docs

## Output 
`out.json`, the serialized json array of scrapped data

## For dev
Checkout `PageParser.java`. This is an Enum class with each Enum value be instructed to
1. Recognize a page based on multiple strings
2. Parse the page and populate the `Declaration` object.

With the above, the development work is around improving each Enum implementations to correctly identify each page, allowing variations (positions of element, versions, etc), and accurately extract information.

For example, if the below appears in the log
```
[main] INFO io.fchk.scrapper.PdfUtil - https://www.districtcouncils.gov.hk/ytm/doc/2016_2019/reg_member/888/CHOW_Chunfai_18.01.2016_T.pdf
[main] WARN io.fchk.scrapper.PdfUtil - Unable to find parser for page 14
``` 
It means that no parser is configured well enough to recognize page 14. 

## Parsing strategies
There are mainly 4 types of parsing strategy.
1. Yes/No checkbox - Extract using nearby text, and check if 有[v] or 否[v] exist.
2. Table with static left column - Remove line breaks and use regex to extract the right column fields in one go.
3. Table with both left and right free text column - Use `PDFTableStripper` to find the coordinate of left and right column. Extract and merge text base on coordinates.
4. Free text - Regex to match the text before and after. 

## Format json output
Format json output for easier comparison
```
cat out.json | jq '.' > out_formatted.json
```
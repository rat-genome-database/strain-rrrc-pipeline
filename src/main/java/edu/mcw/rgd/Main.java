package edu.mcw.rgd;

import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author mtutaj
 * @since 08/24/2022
 */
public class Main {

    private Dao dao = new Dao();
    private String version;

    Logger log = LogManager.getLogger("status");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Main manager = (Main) (bf.getBean("manager"));

        try {
            manager.run();
        }catch (Exception e) {
            Utils.printStackTrace(e, manager.log);
            throw e;
        }
    }

    public void run() throws Exception {

        long time0 = System.currentTimeMillis();

        log.info(getVersion());
        log.info("   "+dao.getConnectionInfo());
        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(new Date(time0)));

        String mainTableContent = getDataTableHtml();
        if( mainTableContent!=null ) {
            List<RrrcRecord> records = parseRecords(mainTableContent);
        }

        log.info("OK -- time elapsed: "+Utils.formatElapsedTime(time0, System.currentTimeMillis()));
    }

    String getDataTableHtml() throws Exception {

        String url = "https://www.rrrc.us/search/?strainStatus=available&num=all";
        FileDownloader fd = new FileDownloader();
        fd.setExternalFile(url);
        fd.setLocalFile("data/available_strains.html");
        String localFile = fd.downloadNew();

        String html = Utils.readFileAsString(localFile);
        log.info("downloaded available strains file "+url+";  size "+html.length());

        String startTableTag = "<table class=\"submissions2\"";
        String endTableTag = "</table>";
        int startTableTagPos = html.indexOf(startTableTag);
        int endTableTagPos = html.indexOf(endTableTag, startTableTagPos);
        if( startTableTagPos<0 || endTableTagPos<=startTableTagPos ) {
            log.error("ERROR: unexpected file content -- exiting");
            return null;
        }
        String mainTableContent = html.substring(startTableTagPos, endTableTagPos);
        log.info("-- html data table size: "+mainTableContent.length());
        return mainTableContent;
    }

    List<RrrcRecord> parseRecords(String htmlTable) {

        List<RrrcRecord> results = new ArrayList<>();

        // extract trs
        List<String> trs = new ArrayList<>();
        int startPos = 0, endPos = 0;
        while( true ) {

            startPos = htmlTable.indexOf("<tr", endPos);
            if( startPos<0 ) {
                break;
            }
            endPos = htmlTable.indexOf("</tr", startPos);
            String tr = htmlTable.substring(startPos, endPos);
            trs.add(tr);
        }
        log.info("-- trs in table: "+trs.size());

        // extract tds
        for( String tr: trs ) {
            RrrcRecord rec = parseTr(tr);
            if( rec!=null ) {
                results.add(rec);
            }
        }

        log.info("-- extracted records: "+results.size());

        dump(results);

        return results;
    }

    RrrcRecord parseTr(String tr) {

        // extract tds
        List<String> tds = new ArrayList<>();
        int startPos = 0, endPos = 0;
        while( true ) {

            startPos = tr.indexOf("<td", endPos);
            if( startPos<0 ) {
                break;
            }
            endPos = tr.indexOf("</td", startPos);
            String td = tr.substring(startPos, endPos);
            td = td.replace("&nbsp;", "").replace("<td>", "").replace("<td >", "");
            tds.add(td);
        }

        // there must be exactly five tds
        if( tds.size()!=5 ) {
            return null;
        }

        RrrcRecord rec = new RrrcRecord();

        // TD1: parse RRRC ID
        // <td>42&nbsp;</td>
        String html = tds.get(0);
        if( html.length()>20 ) {
            // not a valid RRRC ID td
            return null;
        }
        // typical html to parse:
        // <td>946
        rec.setRrrcId(html);

        // TD2: parse strain symbol: text within <a></a>
        // <td><span style="font-size:12px;"><strong>
        // <a href="../Strain/?x=946" title="Common Name: F344-Tg(CAG-<em>hACE2</em>)057Bryd" onclick="document.location =  'https://www.rrrc.us/Strain/?x=946&log=yes'; return false;">F344-Tg(CAG-<em>hACE2</em>)057Bryd</a>
        // </strong></span></td>
        html = tds.get(1);
        int openAPos = html.indexOf("<a");
        if( openAPos<0 ) { return null; }
        int endAPos = html.indexOf(">", openAPos);
        int endAPos2 = html.indexOf("</a>", endAPos);
        String strainName = html.substring(endAPos+1, endAPos2);
        int pos3 = strainName.lastIndexOf("\">");
        if( pos3>0 ) {
            strainName = strainName.substring(pos3+2);
        }
        rec.setStrainName(strainName);

        // TD3: parse gene symbol
        // a) RGD gene
        // <td><a href="https://rgd.mcw.edu/rgdweb/report/gene/main.html?id=620854" target="_blank"><strong>Dusp5</strong></a></td>
        html = tds.get(2);
        openAPos = html.indexOf("<a");
        if( openAPos>=0 ) {
            endAPos = html.indexOf(">", openAPos);
            endAPos2 = html.indexOf("</a>", endAPos);
            if( endAPos2>=0 ) {
                String geneSymbol = html.substring(endAPos+1, endAPos2);
                geneSymbol = geneSymbol.replace("<strong>", "").replace("</strong>", "").trim();
                rec.setGeneSymbol(geneSymbol);

                endAPos2 = html.indexOf("href=\"", openAPos);
                if( endAPos2>0 ) {
                    int dblQuotePos1 = endAPos2 + 6;
                    int dblQuotePos2 = html.indexOf("\"", dblQuotePos1);
                    rec.setGeneUrl(html.substring(dblQuotePos1, dblQuotePos2));
                }

                // extract gene rgd id
                String rgdTemplate = "rgd.mcw.edu/rgdweb/report/gene/main.html?id=";
                endAPos = html.indexOf(rgdTemplate, openAPos);
                if( endAPos>0 ) {
                    String geneRgdId = extractNumber(html,endAPos+rgdTemplate.length());
                    rec.setGeneRgdId(Integer.parseInt(geneRgdId));
                }
            }
        } else {
            // simpler case:  <td>&nbsp;</td>
            String geneSymbol = html.trim();
            rec.setGeneSymbol(geneSymbol);
        }

        // TD4: parse availability
        html = tds.get(3);
        String availability = html.trim();
        pos3 = availability.lastIndexOf("\">");
        if( pos3>=0 ) {
            availability = availability.substring(pos3+2).trim();
        }
        rec.setAvailability(availability);

        // TD5: parse donor
        html = tds.get(4);
        String donor = html.trim();
        pos3 = donor.lastIndexOf(">");
        if( pos3>=0 ) {
            donor = donor.substring(pos3+1).trim();
        }
        rec.setDonor(donor);

        return rec;
    }

    String extractNumber(String txt, int startPos) {

        int pos;
        for( pos=startPos; pos<txt.length(); pos++ ) {
            char c = txt.charAt(pos);
            if( !Character.isDigit(c) ) {
                break;
            }
        }
        return txt.substring(startPos, pos);
    }

    void dump(List<RrrcRecord> list) {

        log.info("===");
        for(int i=1; i<=list.size(); i++ ) {
            RrrcRecord r = list.get(i-1);
            log.info(i+".\t"+r.getRrrcId()+"\t"+r.getGeneSymbol()+"\t"+r.getGeneRgdId()
                    +"\t"+r.getAvailability()+"\t"+r.getDonor()+"\t"+r.getGeneUrl());
        }
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}


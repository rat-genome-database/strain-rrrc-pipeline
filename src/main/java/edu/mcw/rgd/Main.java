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
            tds.add(td);
        }

        // there must be exactly five tds
        if( tds.size()!=5 ) {
            return null;
        }

        RrrcRecord rec = new RrrcRecord();

        // TD1: parse RRRC ID
        // <td>42&nbsp;</td>
        String html = tds.get(0).replace("&nbsp;", "");

        return rec;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}


package edu.mcw.rgd;

public class RrrcRecord {

    private String rrrcId;
    private String strainName;
    private String geneSymbol;
    private int geneRgdId;
    private String geneUrl;
    private String availability;
    private String donor;

    public String getRrrcId() {
        return rrrcId;
    }

    public void setRrrcId(String rrrcId) {
        this.rrrcId = rrrcId;
    }

    public String getStrainName() {
        return strainName;
    }

    public void setStrainName(String strainName) {
        this.strainName = strainName;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public void setGeneSymbol(String geneSymbol) {
        this.geneSymbol = geneSymbol;
    }

    public int getGeneRgdId() {
        return geneRgdId;
    }

    public void setGeneRgdId(int geneRgdId) {
        this.geneRgdId = geneRgdId;
    }

    public String getGeneUrl() {
        return geneUrl;
    }

    public void setGeneUrl(String geneUrl) {
        this.geneUrl = geneUrl;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getDonor() {
        return donor;
    }

    public void setDonor(String donor) {
        this.donor = donor;
    }
}

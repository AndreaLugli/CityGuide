package denis.semanticcityguide;

/**
 * Created by Denis on 10/07/2014.
 */
public class Place {
    private String label;
    private String linkDBpedia;
    private double distance;

    public String getLabel() {
        return label;
    }
    public String getLinkDBpedia(){
        return linkDBpedia;
    }
    public double getDistance(){
        return distance;
    }

    public Place(String label, String linkDBpedia, double distance){

        this.label = label;
        this.linkDBpedia = linkDBpedia;
        this.distance = distance;
    }
}

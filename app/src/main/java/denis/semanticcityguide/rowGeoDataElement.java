package denis.semanticcityguide;

/**
 * Created by Denis on 23/07/2014.
 */
public class rowGeoDataElement {
    private String name;
    private String drawableResource;

    public rowGeoDataElement(String name, String drawableResource){
        this.name = name;
        this.drawableResource = drawableResource;
    }

    public String getName(){
        return name;
    }
    public String getDrawableResource(){
        return drawableResource;
    }
}

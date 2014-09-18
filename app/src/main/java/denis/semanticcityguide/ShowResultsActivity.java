package denis.semanticcityguide;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;
import android.widget.TabHost;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;


public class ShowResultsActivity extends FragmentActivity{

    private GoogleMap map = null;
    private MyListFragment elenco = null;
    private HashMap myMarkers;
    private String lang;
    private String miaLati;
    private String miaLongi;
    private String from;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Rimuovo la titlebar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.show_results);

        //Creo il TabHost per inserire 2 fragment: mappa ed elenco
        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        //Primo tab
        TabHost.TabSpec tabSpec = tabHost.newTabSpec("map");
        tabSpec.setContent(R.id.tabMap);
        tabSpec.setIndicator("Map");
        tabHost.addTab(tabSpec);

        //Secondo tab
        tabSpec = tabHost.newTabSpec("elenco");
        tabSpec.setContent(R.id.tabList);
        tabSpec.setIndicator("Elenco");
        tabHost.addTab(tabSpec);

        //Ricavo le coordinate dall'intent
        Bundle intentExtras = getIntent().getExtras();
        ArrayList<String> preferenze = intentExtras.getStringArrayList("preferenze");
        double lati = intentExtras.getDouble("lati");
        double longi = intentExtras.getDouble("longi");
        String km = Integer.toString(intentExtras.getInt("km"));
        lang = intentExtras.getString("lang");
        from = intentExtras.getString("from");

        //Ricavo l'istanza della mappa e dell'elenco per poter richiamare i metodi definiti nel fragment
        map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();
        elenco = ((MyListFragment) getFragmentManager()
                .findFragmentById(R.id.elenco));


        //posiziono il centro della mappa sul punto d'interesse
        map.setMyLocationEnabled(true);
        LatLng currentLocation = new LatLng(lati, longi);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15)); //Posiziono il focus a uno zoom di 15

        //Scarico i dati da DBPedia e riempio dinamicamente la mappa e l'elenco
        DBPediaLoaderTask loadData = new DBPediaLoaderTask();

        //Preparo la query con i filtri richiesti dall'utente
        String query = createSparqlQuery(preferenze,lati,longi, km, lang);

        //Avvio l'esecuzione del Task Asincrono a cui passo l'URL all'end-point DBPedia con la query endecodizzata
        loadData.execute(new String[] {query});

    }

    private class DBPediaLoaderTask extends AsyncTask<String, Void, String> {
        ProgressDialog progDailog = null;
        protected void onPreExecute() {
            progDailog = new ProgressDialog(ShowResultsActivity.this);
            progDailog.setMessage(getResources().getString(R.string.caricamento)); //Assegno il messaggio da visualizzare direttamente dalle resource
            progDailog.setIndeterminate(true);
            progDailog.setCancelable(false); //Obbliga l'utente ad attendere il termine del ProgressDialog
            progDailog.show();
        }

        @Override
        protected String doInBackground(String... urls) {
            String response = "";
            for (String url : urls) {
                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);
                try {
                    HttpResponse execute = client.execute(httpGet);
                    InputStream content = execute.getEntity().getContent();

                    //Salvo tutti i risultati ottenuti in una stringa, in questo caso avrà la forma di un JSON con le informazioni da parsare
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                    String s = "";
                    while ((s = buffer.readLine()) != null) {
                        response += s;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            //Posiziono i markers sulla mappa e riempio l'elenco
            showResults(result);

            if(this.progDailog.isShowing())
            {
                this.progDailog.dismiss();
            }
        }
    }

    /*
    Metodo che posiziona i marker in riferimento ai risultati ottenuti da DBpedia
     */
    private void showResults(String jsonResults){
        try {
            JSONObject obj = new JSONObject(jsonResults);
            JSONObject results = obj.getJSONObject("results");
            JSONArray bindings = results.getJSONArray("bindings");

            //HashMap per tenere in memoria i riferimenti ai marker sulla mappa, serve per tenere in memoria le informazioni necessarie al click
            myMarkers = new HashMap<String,String>();
            //Creo un Arraylist di luoghi da passare al ListFragment per settare poi il CustumArrayAdapter
            ArrayList<Place> listaPlaces = new ArrayList<Place>();

            for (int i = 0; i < bindings.length(); i++) {
                JSONObject figlio = bindings.getJSONObject(i); //prendo il figlio diretto di posizione i di bindings
                JSONObject f = figlio.getJSONObject("f");
                String linkDBpedia = f.getString("value");
                JSONObject latitudine = figlio.getJSONObject("latitudine");
                double lati = latitudine.getDouble("value");
                JSONObject longitudine = figlio.getJSONObject("longitudine");
                double longi = longitudine.getDouble("value");
                JSONObject label = figlio.getJSONObject("label");
                String titolo = label.getString("value");
                JSONObject distanza = figlio.getJSONObject("callret-4");
                double dist = distanza.getDouble("value");

                Place myPlace = new Place(titolo,linkDBpedia,dist);
                listaPlaces.add(myPlace); //Riempio l'ArrayList con i luoghi ordinati direttamente in base alla distanza


                //Creo un oggeto LatLng da passare al marker con la posizione
                LatLng posizione = new LatLng(lati, longi);

                Marker marker = map.addMarker(new MarkerOptions()
                        .title(titolo)
                        .position(posizione)
                        .snippet("Click qui per maggiori informazioni")
                        //.icon(BitmapDescriptorFactory.fromResource(R.drawable.busstop))
                );
                myMarkers.put(marker.getId(), linkDBpedia); //Chiave: il marker appena creato, valore: un oggetto qualsiasi che vogliamo tenere in memoria
            }
            map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                @Override
                public void onInfoWindowClick(Marker x) {
                    String DBLink = (String)myMarkers.get(x.getId());
                    Intent openGuideActivity = new Intent(getApplicationContext(), GuideActivity.class);
                    openGuideActivity.putExtra("link", DBLink);
                    openGuideActivity.putExtra("lang",lang);
                    openGuideActivity.putExtra("lati",miaLati);
                    openGuideActivity.putExtra("longi",miaLongi);
                    openGuideActivity.putExtra("from", from);
                    startActivity(openGuideActivity);
                }
            });

            //Passo l'ArrayList al fragment e setto l'adapter
            elenco.passArrayList(listaPlaces, lang, miaLati, miaLongi, from);
            elenco.setAdapter();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String createSparqlQuery(ArrayList<String> requests, double lati, double longi, String km, String lang){

        miaLati = Double.toString(lati);
        miaLongi = Double.toString(longi);

        String intro = "http://dbpedia.org/sparql?default-graph-uri=";

        String defaultGraphUri = "http://dbpedia.org";
        String endecodeDefaulGraphUri = "";
        try {
            endecodeDefaulGraphUri = URLEncoder.encode(defaultGraphUri,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String space = "&query=";
        String queryBase = "SELECT DISTINCT (SAMPLE(?f) as ?f) ?latitudine ?longitudine ?label" +
                "(bif:st_distance(?geo, bif:st_point (" + miaLongi + ", " + miaLati + "),"+ km +"))"+
                "WHERE"+
                "{"+
                "?f geo:lat ?latitudine."+
                "?f geo:long ?longitudine."+
                "?f rdfs:label ?label."+
                "?f dcterms:subject ?termine."+
                "?f geo:geometry ?geo."+
                "FILTER (bif:st_intersects (?geo, bif:st_point (" + miaLongi + ", " +miaLati +")," + km + "))."+
                "FILTER (lang(?label) = \""+lang+"\")."+
                "FILTER ( "; //"FILTER ( '+stringa+' )."

        //Filtri sulle richieste dell'utente

        String filtroTrasporti = "REGEX(STR(?termine), \"Transport\", \"i\") || REGEX(STR(?termine), \"Airports\", \"i\") || REGEX(STR(?termine), \"Railway\", \"i\")";
        String filtroReligione = "REGEX(STR(?termine), \"Basilica\", \"i\") || REGEX(STR(?termine), \"Cathedrals\", \"i\") || REGEX(STR(?termine), \"Mosques\", \"i\") || REGEX(STR(?termine), \"Mausoleums\", \"i\") || REGEX(STR(?termine), \"Cemeteries\", \"i\") || REGEX(STR(?termine), \"Churches\", \"i\")";
        String filtroSport = "REGEX(STR(?termine), \"Sport\", \"i\")";
        String filtroArchitettura = "REGEX(STR(?termine), \"Hotels\", \"i\") || REGEX(STR(?termine), \"Buildings_and_structures\", \"i\") || REGEX(STR(?termine),"+
                                    "\"Skyscrapers\", \"i\") || REGEX(STR(?termine), \"Palaces\", \"i\") || REGEX(STR(?termine), \"Bridges\", \"i\") || REGEX(STR(?termine),"+
                                    "\"Piazzas\", \"i\") || REGEX(STR(?termine), \"Towers\", \"i\") || REGEX(STR(?termine), \"Gates\", \"i\") || REGEX(STR(?termine), \"Domes\","+
                                    "\"i\") || REGEX(STR(?termine), \"Castles\", \"i\") || REGEX(STR(?termine), \"Triumphal_arche\", \"i\") || REGEX(STR(?termine), \"Fountains\", \"i\")";
        String filtroNatura = "REGEX(STR(?termine), \"Astronomical\", \"i\") || REGEX(STR(?termine), \"Planetaria\", \"i\") || REGEX(STR(?termine), \"Aquaria\", \"i\") ||"+
                              "REGEX(STR(?termine), \"Zoo\", \"i\") || REGEX(STR(?termine), \"Lakes\", \"i\") || REGEX(STR(?termine), \"Gardens\", \"i\") || REGEX(STR(?termine),"+
                              "\"Parks\", \"i\")";
        String filtroEducazione = "REGEX(STR(?termine), \"Universities\", \"i\") || REGEX(STR(?termine), \"Museums\", \"i\") || REGEX(STR(?termine), \"Theatres\", \"i\") ||"+
                                  "REGEX(STR(?termine), \"Outdoor_sculptures\", \"i\") || REGEX(STR(?termine), \"Libraries\", \"i\") || REGEX(STR(?termine), \"Paintings\", \"i\")";

        //Prendo il primo elemento e carico subito il primo filtro
        //Ciclo le richieste dell'utente per aggiungere filtri alla query sparql
        for(int i = 0; i<requests.size(); i++){
            String preferenza = requests.get(i);

            //Se ci sono più preferenze devo ricordarmi di inserire la clausola (OR ||) prima del filtro successivo
            if(i>0){
                queryBase += " || ";
            }
            if(preferenza.equals("trasporti")){
                queryBase += filtroTrasporti;
            }
            else if(preferenza.equals("religione")){
                queryBase += filtroReligione;
            }
            else if(preferenza.equals("sport")){
                queryBase += filtroSport;
            }
            else if(preferenza.equals("architettura")){
                queryBase += filtroArchitettura;
            }
            else if(preferenza.equals("natura")){
                queryBase += filtroNatura;
            }
            else if(preferenza.equals("educazione")){
                queryBase += filtroEducazione;
            }
        }

        String query = queryBase + ").} ORDER BY ASC 5 LIMIT 500"; //Chiudo la query con i filtri da passare all'encoder


        String encodeQueryBase = "";
        try {
            encodeQueryBase = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String spaceFormat = "&format=";

        String formato = "application/sparql-results+json";
        String encodeFormato ="";
        try {
            encodeFormato = URLEncoder.encode(formato, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String finalQuery = intro + endecodeDefaulGraphUri + space + encodeQueryBase + spaceFormat + encodeFormato;
        return finalQuery;
    }
}
package denis.semanticcityguide;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

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
import java.util.HashMap;

public class LinkedGeoDataGuide extends Activity {

    private GoogleMap map = null;
    private HashMap myMarkers;
    private String from;
    private double lati;
    private double longi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linked_geo_data_guide);

        //Ricavo le coordinate dall'intent
        Bundle intentExtras = getIntent().getExtras();
        lati = intentExtras.getDouble("lati");
        longi = intentExtras.getDouble("longi");
        String filtro = intentExtras.getString("filtro");
        from = intentExtras.getString("from");

        map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();
        //posiziono il centro della mappa sul punto d'interesse
        map.setMyLocationEnabled(true);
        LatLng currentLocation = new LatLng(lati, longi);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15)); //Posiziono il focus a uno zoom di 15

        //Scarico i dati da DBPedia e riempio dinamicamente la mappa e l'elenco
        LinkedGeoDataLoaderTask loadData = new LinkedGeoDataLoaderTask();

        //Preparo la query con i filtri richiesti dall'utente
        String query = createSparqlQuery(lati,longi, filtro);

        //Avvio l'esecuzione del Task Asincrono a cui passo l'URL all'end-point DBPedia con la query endecodizzata
        loadData.execute(new String[] {query});
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.linked_geo_data_guide, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class LinkedGeoDataLoaderTask extends AsyncTask<String, Void, String> {
        ProgressDialog progDailog = null;
        protected void onPreExecute() {
            progDailog = new ProgressDialog(LinkedGeoDataGuide.this);
            progDailog.setMessage(getResources().getString(R.string.caricamento)); //Assegno il messaggio da visualizzare direttamente dalle resource
            progDailog.setIndeterminate(true);
            progDailog.setCancelable(true); //Obbliga l'utente ad attendere il termine del ProgressDialog
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

            showResults(result);//Posiziono i markers sulla mappa

            if(this.progDailog.isShowing())
            {
                this.progDailog.dismiss();
            }
        }
    }

    /*
    Metodo che posiziona i marker in riferimento ai risultati ottenuti da LinkedGeoData
     */
    private void showResults(String jsonResults){
        try {
            JSONObject obj = new JSONObject(jsonResults);
            JSONObject results = obj.getJSONObject("results");
            JSONArray bindings = results.getJSONArray("bindings");
            //HashMap per tenere in memoria i riferimenti ai marker sulla mappa, serve per tenere in memoria le informazioni necessarie al click
            myMarkers = new HashMap<String,LatLng>();
            for (int i = 0; i < bindings.length(); i++) {
                JSONObject figlio = bindings.getJSONObject(i); //prendo un figlio diretto di bindings
                JSONObject latitudine = figlio.getJSONObject("latitudine");
                double lati = latitudine.getDouble("value");
                JSONObject longitudine = figlio.getJSONObject("longitudine");
                double longi = longitudine.getDouble("value");
                JSONObject label = figlio.getJSONObject("label");
                String nomePosto = label.getString("value");
                JSONObject tipo = figlio.getJSONObject("tipo");
                String tipoPosto = tipo.getString("value");

                JSONObject distanza = figlio.getJSONObject("callret-4");
                double dist = distanza.getDouble("value"); //per adesso non la usiamo

                //Creo un oggeto LatLng da passare al marker con la posizione
                LatLng posizione = new LatLng(lati, longi);

                Marker marker = map.addMarker(new MarkerOptions()
                        .title(nomePosto)
                        .position(posizione)
                        .snippet("Avvia il navigatore")
                );
                myMarkers.put(marker.getId(), posizione); //Chiave: il marker appena creato, valore: un oggetto qualsiasi che vogliamo tenere in memoria
            }
            map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                @Override
                public void onInfoWindowClick(Marker x) {
                    LatLng posizione = (LatLng)myMarkers.get(x.getId());
                    if(from.equalsIgnoreCase("currentLocation")){
                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                                Uri.parse("google.navigation:q=" + posizione.latitude + "," + posizione.longitude + "&mode=w"));
                        startActivity(intent);
                    }
                    else{
                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                                Uri.parse("http://maps.google.com/maps?saddr="+ lati +","+ longi + "&daddr="+ posizione.latitude +"," +posizione.longitude));
                        startActivity(intent);
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String createSparqlQuery(double lati, double longi, String filtro){

        String miaLati = Double.toString(lati);
        String miaLongi = Double.toString(longi);

        String intro = "http://linkedgeodata.org/sparql?default-graph-uri=";

        String defaultGraphUri = "http://linkedgeodata.org";
        String endecodeDefaulGraphUri = "";
        try {
            endecodeDefaulGraphUri = URLEncoder.encode(defaultGraphUri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String space = "&query=";

        String queryBase = "Prefix ogc: <http://www.opengis.net/ont/geosparql#>\n" +
                "Prefix geom: <http://geovocab.org/geometry#>\n" +
                "Prefix lgdo: <http://linkedgeodata.org/ontology/>\n" +
                "\n" +
                "Select distinct ?label ?tipo ?latitudine ?longitudine bif:st_distance(?g, bif:st_point ("+miaLongi+", "+miaLati+")) {\n" +
                "\n" +
                "   ?s rdf:type ?type.\n" +
                "   ?s rdfs:label ?label.\n" +
                "   ?s geo:lat ?latitudine.\n"+
                "   ?s geo:long ?longitudine.\n"+
                "   ?s geom:geometry [\n" +
                "      ogc:asWKT ?g\n" +
                "    ] .\n" +
                "\n" +
                "    ?s rdf:type [\n" +
                "      rdfs:label ?tipo\n" +
                "    ].\n" +
                "\n" +
                "    FILTER (lang(?tipo) = \"it\"). \n" +
                "    Filter(bif:st_intersects (?g, bif:st_point ("+longi+", "+lati+"), 5)) .\n";

        //Filtro sulle richieste dell'utente
        queryBase += filtro;

        String query = queryBase + ").\n} ORDER BY ASC 5"; //Chiudo la query con i filtri da passare all'encoder

        String encodeQueryBase = "";
        try {
            encodeQueryBase = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String spaceFormat = "&format=";

        String formato = "application/sparql-results+json&timeout=0&debug=on";
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
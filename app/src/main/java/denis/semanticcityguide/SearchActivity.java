package denis.semanticcityguide;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SearchActivity extends Activity {

    private double lati = 0.0;
    private double longi = 0.0;
    private double latigeo;
    private double longigeo;
    private ToggleButton religionButton, sportButton, archButton, natureButton, eduButton, traspButton;
    private Button btnRicerca;
    private Geocoder geocoder = new Geocoder(this);
    private TextView geoTextView;
    private Button changeLocationButton;
    private AutoCompleteTextView autoCompView;
    private SeekBar seekBarKm;
    private TextView kmView;
    private InputMethodManager imm; //Utilizzato per nascondere la keyboard in determinati contesti
    private Spinner spinLang;
    private String language = "it";

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    private String[] mGeoDataFilters;
    private MyAdapter myAdapter;
    private SpinnerAdapter mySpinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        //Gestione del navigation drawer

        mGeoDataFilters = getResources().getStringArray(R.array.linked_geo_data_filters);
        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        myAdapter = new MyAdapter(this);
        mDrawerList.setAdapter(myAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility, per persone disabili*/
        ) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                hideSoftKeyboard();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true); //Permette all'actionbar di essere cliccata
        getActionBar().setHomeButtonEnabled(true);

        //Inizializzo l'InputMethodManager
        imm =(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        autoCompView = (AutoCompleteTextView) findViewById(R.id.autocomplete);
        autoCompView.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.places_adapter));
        autoCompView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String str = (String) adapterView.getItemAtPosition(position);
                //convertAddress(str);
                hideSoftKeyboard();
                //Toast.makeText(SearchActivity.this, "Location changed", Toast.LENGTH_SHORT).show();
            }
        });


        //Recupero i riferimenti alle view del layout
        changeLocationButton = (Button) findViewById(R.id.changeLocation);
        geoTextView = (TextView) findViewById(R.id.geotextView);
        btnRicerca = (Button) findViewById(R.id.search);
        religionButton = (ToggleButton)findViewById(R.id.religionButton);
        sportButton = (ToggleButton)findViewById(R.id.sportButton);
        archButton = (ToggleButton) findViewById(R.id.archButton);
        natureButton = (ToggleButton) findViewById(R.id.natureButton);
        eduButton = (ToggleButton) findViewById(R.id.eduButton);
        traspButton = (ToggleButton) findViewById(R.id.traspButton);
        seekBarKm = (SeekBar) findViewById(R.id.seekBar);
        kmView = (TextView) findViewById(R.id.kmView);
        spinLang = (Spinner) findViewById(R.id.spinnerlang);
        mySpinnerAdapter = new SpinnerAdapter(this);
        spinLang.setAdapter(mySpinnerAdapter);



        //Assegno ai button visibili la possibilità di nascondere la tastiera nel caso di autocomplete aperta
        religionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideSoftKeyboard();
            }
        });
        sportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideSoftKeyboard();
            }
        });
        natureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideSoftKeyboard();
            }
        });
        archButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideSoftKeyboard();
            }
        });


        kmView.setText("Km " + seekBarKm.getProgress());
        spinLang.setOnItemSelectedListener(new CustomOnItemSelectedListener());

        seekBarKm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                kmView.setText("Km " + i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        /*
        AsyncTask che blocca la possibilità di interagire con l'activity per recuperare la posizione dell'utente
         */
        GeoTask task = new GeoTask();
        task.execute();

        //Button per cambiare località
        changeLocationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(geoTextView.getVisibility() == View.VISIBLE){
                    geoTextView.setVisibility(View.INVISIBLE);
                    autoCompView.setVisibility(View.VISIBLE);
                    autoCompView.requestFocus();
                    imm.showSoftInput(autoCompView, 0);
                    changeLocationButton.setBackground(getResources().getDrawable(R.drawable.geo_2));
                }
                else{
                    //Risetto lati e longi a default della posizione attuale perché al click del button
                    //le variabili, nel caso di autCompView, vengono riconvertite
                    hideSoftKeyboard();
                    lati = latigeo;
                    longi = longigeo;
                    geoTextView.setVisibility(View.VISIBLE);
                    autoCompView.setVisibility(View.GONE);
                    changeLocationButton.setBackground(getResources().getDrawable(R.drawable.geo_1));
                }
            }
        });

        //Button per avviare la ricerca. Attiva una view per la visualizzazione delle informazioni
        btnRicerca.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ArrayList<String> preferenze = checkPreferences();
                if(autoCompView.getVisibility() == View.VISIBLE && autoCompView.getText().toString().isEmpty()){
                    Toast.makeText(getApplicationContext(), "Seleziona una località", Toast.LENGTH_SHORT).show();
                }
                else if(preferenze == null ){
                    Toast.makeText(getApplicationContext(), "Seleziona almeno una categoria", Toast.LENGTH_SHORT).show();
                }
                else{
                    //Se la TextView non è visibile, vuol dire che l'utente ha scelto di cambiare località
                    //Devo quindi cambiare latitudine e longitudine rispetto alla località dell'autocomplete text view
                    String from = "currentLocation";
                    if(geoTextView.getVisibility() == View.INVISIBLE){
                        convertAddress(autoCompView.getText().toString());
                        from = "changedLocation";
                    }
                    Intent nuovaPagina = new Intent(getApplicationContext(), ShowResultsActivity.class);
                    nuovaPagina.putExtra("preferenze", preferenze);
                    nuovaPagina.putExtra("lati", lati);
                    nuovaPagina.putExtra("longi", longi);
                    nuovaPagina.putExtra("km", seekBarKm.getProgress());
                    nuovaPagina.putExtra("lang", language);
                    nuovaPagina.putExtra("from", from);
                    startActivity(nuovaPagina);
                }
            }
        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        return super.onPrepareOptionsMenu(menu);
    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //Se la TextView non è visibile, vuol dire che l'utente ha scelto di cambiare località
            //Cambio latitudine e longitudine rispetto alla località dell'autocomplete text view
            String from = "currentLocation";
            if(geoTextView.getVisibility() == View.INVISIBLE){
                convertAddress(autoCompView.getText().toString());
                from = "changedLocation";
            }
            Intent nuovaPagina = new Intent(getApplicationContext(), LinkedGeoDataGuide.class);
            nuovaPagina.putExtra("lati", lati);
            nuovaPagina.putExtra("longi", longi);
            nuovaPagina.putExtra("filtro",mGeoDataFilters[position]);
            nuovaPagina.putExtra("from", from);
            startActivity(nuovaPagina);
        }
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(mDrawerToggle.onOptionsItemSelected(item)){
            return true; //return true significa che l'evento è stato catturato correttamente dal navigation drawer
        }
        return super.onOptionsItemSelected(item); //Altrimenti chiama il metodo di default della superclasse
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public class GeoTask extends AsyncTask<String, Integer, String> {
        ProgressDialog progDailog = null;
        private LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        private VeggsterLocationListener mVeggsterLocationListener = new VeggsterLocationListener();
        private Geocoder gcd = new Geocoder(SearchActivity.this, Locale.getDefault());

        @Override
        protected void onPreExecute() {

            /*  METODO DI CONTROLLO GPS ATTIVO
            if(mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == false){
                Toast.makeText(SearchActivity.this,
                        "Attivare il gps",
                        Toast.LENGTH_LONG).show();
            }*/

            //Registro il mio LocationListener nel LocationManager per ricevere aggiornamenti della posizione
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 300000, 0, mVeggsterLocationListener); //5 minuti tra un update della posizione e l'altro

            /*
            Paremetri in input:
            - LocationManager.NETWORK_PROVIDER: nome del provider con cui registrarsi
            - minTime 0: tempo minimo in millisecondi tra due richieste di location update
            - minDistance 0: distanza minima in metri per ricevere un location update
            - listener: un oggetto LocationListener i cui metodi vengono implementati vengono richiamati ad ogni update
             */

            progDailog = new ProgressDialog(SearchActivity.this);
            progDailog.setMessage(getResources().getString(R.string.relPosition)); //Assegno il messaggio da visualizzare direttamente dalle resource
            progDailog.setIndeterminate(true);
            progDailog.setCancelable(false); //Obbliga l'utente ad attendere il termine del ProgressDialog
            progDailog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progDailog.dismiss();
            /*Toast.makeText(SearchActivity.this,
                    "LATITUDE :" + lati + " LONGITUDE :" + longi,
                    Toast.LENGTH_LONG).show();*/
        }

        @Override
        protected String doInBackground(String... params) {
            //Attendo il rilevamento della posizione
            while (lati == 0.0) {

            }
            return null;
        }

        private class VeggsterLocationListener implements LocationListener {

            @Override
            public void onLocationChanged(Location location) {

                try {
                    lati = location.getLatitude();
                    longi = location.getLongitude();
                    latigeo = lati;
                    longigeo = longi;

                } catch (Exception e) {
                    progDailog.dismiss();
                    Toast.makeText(getApplicationContext(),"Impossibile recuperare la posizione"
                            , Toast.LENGTH_LONG).show();
                }

                try{
                    List<Address> addresses = gcd.getFromLocation(lati, longi, 1);
                    if (addresses.size() > 0)
                        geoTextView.setText(addresses.get(0).getLocality());
                }catch(IOException e){
                    Toast.makeText(SearchActivity.this,
                            "Impossibile trovare la posizione",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.i("OnProviderDisabled", "OnProviderDisabled");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.i("onProviderEnabled", "onProviderEnabled");
            }

            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
                Log.i("onStatusChanged", "onStatusChanged");
            }
        }
    }

    private ArrayList<String> checkPreferences(){
        ArrayList<String> preferenze = new ArrayList<String>();
        if(religionButton.isChecked()){
            preferenze.add("religione");
        }
        if(sportButton.isChecked()){
            preferenze.add("sport");
        }
        if(archButton.isChecked()){
            preferenze.add("architettura");
        }
        if(natureButton.isChecked()){
            preferenze.add("natura");
        }
        if(eduButton.isChecked()){
            preferenze.add("educazione");
        }
        if(traspButton.isChecked()){
            preferenze.add("trasporti");
        }
        if(preferenze.size() == 0){
            return null;
        }

        return preferenze;
    }

    private class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private ArrayList<String> resultList;

        public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    Filter.FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    }
                    else {
                        notifyDataSetInvalidated();
                    }
                }};
            return filter;
        }
    }

    private static final String LOG_TAG = "Semantic City Guide";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";

    //Browser api key personale
    private static final String API_KEY = "AIzaSyAk2Dj0eocl7jXkX613gSSoM1_EQTdZObw";

    private ArrayList<String> autocomplete(String input) {
        ArrayList<String> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            //sb.append("&components=country:uk"); Tolto perché faceva riferimento solo agli UK
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            //Carica i risultati in uno string builder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            //Crea oggetto JSON a partire dai risultati
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            //Estrae l'oggetto descritto dal risultato
            resultList = new ArrayList<String>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }
        return resultList;
    }

    /*
    Metodo per per ottenere latitudine e longitudine da un indirizzo
    void perché vado a modificare direttamente lati e longi passate all'activity ShowResultActivity
     */
    public void convertAddress(String address) {
        if (address != null && !address.isEmpty()) {
            try {
                List<Address> addressList = geocoder.getFromLocationName(address, 1);
                if (addressList != null && addressList.size() > 0) {
                    lati = addressList.get(0).getLatitude();
                    longi = addressList.get(0).getLongitude();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void hideSoftKeyboard(){
        if (imm.isAcceptingText()) {
            imm.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
        }
    }

    private class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
            String lingua = parent.getItemAtPosition(pos).toString();
            if(lingua.equals("Italiano")){
                language = "it";
            }
            else if(lingua.equals("Tedesco")){
                language = "de";
            }
            else if(lingua.equals("Francese")){
                language = "fr";
            }
            else if(lingua.equals("Spagnolo")){
                language = "es";
            }
            else if(lingua.equals("Russo")){
                language = "ru";
            }
            else if(lingua.equals("Cinese")){
                language = "zh";
            }
            else if(lingua.equals("Olandese")){
                language = "nl";
            }
            else{
                language = "en";
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }

    }


    //Creo un custom adapter per popolare il navigation drawer. Il layout delle righe è definito in custom_row.xml
    class MyAdapter extends BaseAdapter {
        private Context context;
        private String[] mGeoDataPlaces; //Array di stringhe contenente gli elementi definiti in navigation_array.xml
        int[] images = { R.drawable.caffe,R.drawable.supermercato, R.drawable.cinema,R.drawable.pharmacy
                        ,R.drawable.banca, R.drawable.parcheggio, R.drawable.shopping ,R.drawable.nightclub};
        public MyAdapter(Context context){
            this.context = context;
            mGeoDataPlaces = getResources().getStringArray(R.array.geodata_array); //Riempe un array dalle risorse
        }
        @Override
        public int getCount() {
            return mGeoDataPlaces.length;
        }

        @Override
        public Object getItem(int i) {
            return mGeoDataPlaces[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View row = null;
            if(view == null){
                LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.custom_row, viewGroup, false);
            }else{
                row = view;

            }
            TextView titleTextView = (TextView) row.findViewById(R.id.navigation_text);
            ImageView titleImageView = (ImageView) row.findViewById(R.id.navigation_img);
            titleTextView.setText(mGeoDataPlaces[i]);
            titleImageView.setImageResource(images[i]);
            //Alterno il colore delle righe
            if(i % 2 != 0){
                row.setBackgroundColor(Color.parseColor("#ceddf3"));
            }
            return row;
        }
    }
    //Creo un custom adapter per popolare il navigation drawer. Il layout delle righe è definito in custom_row.xml
    class SpinnerAdapter extends BaseAdapter {
        private Context context;
        private String[] mLanguages; //Array di stringhe contenente gli elementi definiti in navigation_array.xml
        int[] images = { R.drawable.italy,R.drawable.germany, R.drawable.spain,R.drawable.france
                ,R.drawable.russia, R.drawable.china, R.drawable.netherlands ,R.drawable.unitedkingdom};
        public SpinnerAdapter(Context context){
            this.context = context;
            mLanguages = getResources().getStringArray(R.array.country_arrays); //Riempe un array dalle risorse
        }
        @Override
        public int getCount() {
            return mLanguages.length;
        }

        @Override
        public Object getItem(int i) {
            return mLanguages[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View row = null;
            if(view == null){
                LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.custom_spinner_row, viewGroup, false);
            }else{
                row = view;

            }
            TextView titleTextView = (TextView) row.findViewById(R.id.spinner_text);
            ImageView titleImageView = (ImageView) row.findViewById(R.id.spinner_img);
            titleTextView.setText(mLanguages[i]);
            titleImageView.setImageResource(images[i]);
            return row;
        }
    }
}
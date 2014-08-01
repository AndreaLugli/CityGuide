package denis.semanticcityguide;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


public class GuideActivity extends Activity {

    private TextView titleView;
    private TextView abstractView;
    private TextView wikiLinkView;
    private Button imageButton;
    private Button navigationButton;
    private Bitmap mIcon = null;
    private int bitmapHeight;
    private int bitmapWidth;
    private String lang;
    private String linkWiki;
    private String lati;
    private String longi;
    private String miaLati;
    private String miaLongi;
    private String from;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        //Recupero le view per dal layout
        titleView = (TextView)findViewById(R.id.titoloGuida);
        imageButton = (Button)findViewById(R.id.seeImageButton);
        abstractView = (TextView)findViewById(R.id.abstractGuide);
        wikiLinkView = (TextView)findViewById(R.id.wikiLinkGuide);
        navigationButton = (Button) findViewById(R.id.navButton);


        Bundle linkRicevuto = getIntent().getExtras();
        String linkDBPedia = linkRicevuto.getString("link");
        lang = linkRicevuto.getString("lang");
        miaLati = linkRicevuto.getString("lati");
        miaLongi = linkRicevuto.getString("longi");
        from = linkRicevuto.getString("from");

        //Modifico l'url ricevuto per gestire i dati in XML con XPath
        String str = linkDBPedia.replace("resource", "data");
        String parsedLink = str + ".xml";
        DBPediaLoaderTask loadData = new DBPediaLoaderTask();
        loadData.execute(new String[] {parsedLink});

    }

    private class ImageDownloader extends AsyncTask<String, Void, Bitmap> {
        ProgressDialog progDialog = null;

        protected void onPreExecute() {
            if(mIcon == null){
                progDialog = new ProgressDialog(GuideActivity.this);
                progDialog.setMessage(getResources().getString(R.string.caricamento)); //Assegno il messaggio da visualizzare direttamente dalle resource
                progDialog.setIndeterminate(true);
                progDialog.setCancelable(true); //Obbliga l'utente ad attendere il termine del ProgressDialog
                progDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface progDialog)
                    {
                        // cancel AsyncTask
                        cancel(true);
                    }
                });
                progDialog.show();
            }
        }
        protected Bitmap doInBackground(String... urls) {
            //Controllo se l'immagine è già stata scaricata, sennò provedo al download e al resize
            if(mIcon == null){
                String url = urls[0];
                try {
                    InputStream in = new java.net.URL(url).openStream();
                    mIcon = BitmapFactory.decodeStream(in);
                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                }
                Display display = GuideActivity.this.getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int screenWidth = size.x;
                int screenHeight = size.y;

                // Get target image size
                try{
                    bitmapHeight = mIcon.getHeight();
                    bitmapWidth = mIcon.getWidth();
                }catch(Exception e){
                    this.cancel(true);
                }


                // Scale the image down to fit perfectly into the screen
                // The value (250 in this case) must be adjusted for phone/tables displays
                while(bitmapHeight > (screenHeight - 30) || bitmapWidth > (screenWidth - 30)) {
                    bitmapHeight = bitmapHeight - 35;
                    bitmapWidth = bitmapWidth - 35;
                }
            }
            if(this.progDialog != null && this.progDialog.isShowing())
            {
                this.progDialog.dismiss();
            }
            return mIcon;
        }

        protected void onPostExecute(Bitmap result) {
            // Create resized bitmap image
            BitmapDrawable resizedBitmap = new BitmapDrawable(GuideActivity.this.getResources(), Bitmap.createScaledBitmap(result, bitmapWidth, bitmapHeight, false));
            // Create dialog
            Dialog dialog = new Dialog(GuideActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialogimage_layout);

            ImageView image = (ImageView) dialog.findViewById(R.id.immagineGuida);

            // !!! Do here setBackground() instead of setImageDrawable() !!! //
            image.setBackground(resizedBitmap);

            // Without this line there is a very small border around the image (1px)
            // In my opinion it looks much better without it, so the choice is up to you.
            dialog.getWindow().setBackgroundDrawable(null);

            // Show the dialog
            dialog.show();

        }
    }

    private class DBPediaLoaderTask extends AsyncTask<String, Void, String> {
        ProgressDialog progDailog = null;
        protected void onPreExecute() {
            progDailog = new ProgressDialog(GuideActivity.this);
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
            //Qui riempire i campi del layout con i dati ricevuto
            try {
                XMLParse(result);
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
            if(this.progDailog.isShowing())
            {
                this.progDailog.dismiss();
            }
        }
    }

    private void XMLParse(String xmlResults) throws XPathExpressionException {

        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {

            DocumentBuilder db = dbf.newDocumentBuilder();

            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xmlResults));
            doc = db.parse(is);

        } catch (ParserConfigurationException e) {
            Log.e("Error: ", e.getMessage());
        } catch (SAXException e) {
            Log.e("Error: ", e.getMessage());
        } catch (IOException e) {
        }
        // Create XPathFactory object
        XPathFactory xpathFactory = XPathFactory.newInstance();

        // Create XPath object
        XPath xpath = xpathFactory.newXPath();
        XPathExpression exprTitle = xpath.compile("//*[local-name()='label'][@lang='"+lang+"']/text()");
        String titolo = (String) exprTitle.evaluate(doc, XPathConstants.STRING);

        XPathExpression exprAbstract = xpath.compile("//*[local-name()='abstract'][@lang='"+lang+"']/text()");
        String descrizione = (String) exprAbstract.evaluate(doc, XPathConstants.STRING);

        XPathExpression exprThumbnail = xpath.compile("//*[local-name()='depiction']/@*[local-name()='resource']");
        final String uriImg = (String) exprThumbnail.evaluate(doc, XPathConstants.STRING);

        XPathExpression exprLinkWiki = xpath.compile("//*[local-name()='isPrimaryTopicOf']/@*[local-name()='resource']");
        linkWiki = (String) exprLinkWiki.evaluate(doc, XPathConstants.STRING); //link wiki en

        XPathExpression exprLat = xpath.compile("//*[local-name()='lat']/text()");
        lati = (String) exprLat.evaluate(doc, XPathConstants.STRING);

        XPathExpression exprLong = xpath.compile("//*[local-name()='long']/text()");
        longi = (String) exprLong.evaluate(doc, XPathConstants.STRING);

        navigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(from.equalsIgnoreCase("currentLocation")){
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("google.navigation:q=" + lati + "," + longi + "&mode=w"));
                    startActivity(intent);
                }
                else{
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("http://maps.google.com/maps?saddr="+ miaLati +","+ miaLongi + "&daddr="+ lati +"," + longi));
                    startActivity(intent);
                }
            }
        });

        if(lang.equals("en")){
            wikiLinkView.setText(linkWiki);

        }
        else{
            String linkConverted = "http://en.wikipedia.org/w/api.php?action=query&prop=langlinks&format=xml&lllimit=100&lllang="+lang+"&titles=";
            Matcher matcher = Pattern.compile("(?<=wiki/).*").matcher(linkWiki);
            if (matcher.find())
            {
                linkConverted += matcher.group();
                linkConverterTask linkTask = new linkConverterTask();
                linkTask.execute(new String[] {linkConverted});
            }
        }

        //Se c'è un'immagine disponibile
        if(Patterns.WEB_URL.matcher(uriImg).matches()){ //Ritorna true se è un uri potenzialmente valido
            imageButton.setVisibility(View.VISIBLE);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImageDownloader loadImage = new ImageDownloader();
                    loadImage.execute(uriImg);
                }
            });

        }

        titleView.setText(titolo);
        abstractView.setText(descrizione);
    }

    private class linkConverterTask extends AsyncTask<String, Void, String> {

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
            try{
                parseXMLLink(result);//Posiziono i markers sulla mappa
            }catch (XPathExpressionException e){

            }
        }
    }

    private void parseXMLLink(String xmlResults) throws XPathExpressionException {

        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xmlResults));
            doc = db.parse(is);

        } catch (ParserConfigurationException e) {
            Log.e("Error: ", e.getMessage());
        } catch (SAXException e) {
            Log.e("Error: ", e.getMessage());
        } catch (IOException e) {
        }
        // Create XPathFactory object
        XPathFactory xpathFactory = XPathFactory.newInstance();

        // Create XPath object
        XPath xpath = xpathFactory.newXPath();

        XPathExpression exprLink = xpath.compile("//langlinks/ll/text()");
        String tailLink = (String) exprLink.evaluate(doc, XPathConstants.STRING);
        if(tailLink.length()==0){
            wikiLinkView.setVisibility(View.GONE);
        }else{
            String stringReplaced = tailLink.replaceAll(" ", "_");
            String linkCorretto = "http://"+ lang +".wikipedia.org/wiki/" + stringReplaced;
            wikiLinkView.setText(linkCorretto);
        }
    }
}
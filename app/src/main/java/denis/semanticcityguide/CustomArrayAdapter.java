package denis.semanticcityguide;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Denis on 10/07/2014.
 */
public class CustomArrayAdapter extends ArrayAdapter<Place> {
    Context context;
    int layoutResourceID;
    ArrayList<Place> data = null;
    String lang;

    public CustomArrayAdapter(Context c, int layoutResourceID, ArrayList<Place> data, String lang){
        super(c,layoutResourceID,data);
        this.context = c;
        this.layoutResourceID = layoutResourceID;
        this.data = data;
        this.lang = lang;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView;
        PlaceHolder holder = null;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceID, parent, false);

            holder = new PlaceHolder();
            holder.titolo = (TextView)row.findViewById(R.id.località);
            holder.distanza = (TextView)row.findViewById(R.id.distanza);
            row.setTag(holder);

        }
        else
        {
            holder = (PlaceHolder)row.getTag();
        }

        Place posto = data.get(position);
        holder.titolo.setText(posto.getLabel()); //Setto il nome della località nel titolo della riga
        double value = posto.getDistance();
        double rounded = (double) Math.round(value * 100) / 100;
        holder.distanza.setText(Double.toString(rounded) + " km"); //Setto la distanza
        //holder.titolo.setOnClickListener(new OnItemClickListener(position));
        return row;
    }

    static class PlaceHolder
    {
        TextView titolo;
        TextView distanza;
    }
    private class OnItemClickListener implements View.OnClickListener {
        private int mPosition;
        OnItemClickListener(int position){
            mPosition = position;
        }
        @Override
        public void onClick(View v) {
            String DBLink = data.get(mPosition).getLinkDBpedia();
            Intent openGuideActivity = new Intent(context, GuideActivity.class);
            openGuideActivity.putExtra("link", DBLink);
            openGuideActivity.putExtra("lang", lang);
            v.getContext().startActivity(openGuideActivity);
        }
    }
}

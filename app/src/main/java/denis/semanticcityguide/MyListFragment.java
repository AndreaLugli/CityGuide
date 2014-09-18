package denis.semanticcityguide;

import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.util.ArrayList;

/**
 * Created by Denis on 09/07/2014.
 */
public class MyListFragment extends ListFragment implements AdapterView.OnItemClickListener {
    private ArrayList<Place> place_data;
    private String lang;
    private String from;
    private String miaLati;
    private String miaLongi;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment_layout, container, false);
    }

    public void passArrayList(ArrayList<Place> place_data, String lang, String miaLati, String miaLongi, String from){
        this.place_data = place_data;
        this.lang = lang;
        this.from = from;
        this.miaLati = miaLati;
        this.miaLongi = miaLongi;
    }
    public void setAdapter(){
        CustomArrayAdapter adapter = new CustomArrayAdapter(getActivity(),
                R.layout.elenco_layout, place_data, lang);
        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) { //int i è la posizione dell'elemento sulla lista
        String DBLink = place_data.get(i).getLinkDBpedia();

        Intent openGuideActivity = new Intent(getActivity(), GuideActivity.class);
        openGuideActivity.putExtra("link", DBLink);
        openGuideActivity.putExtra("lang", lang);
        openGuideActivity.putExtra("lati",miaLati);
        openGuideActivity.putExtra("longi",miaLongi);
        openGuideActivity.putExtra("from", from);
        startActivity(openGuideActivity);
    }
}

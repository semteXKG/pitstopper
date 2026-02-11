package at.semmal.pitstopper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

/**
 * Custom spinner adapter for SpeedHive cars.
 * Shows car number and driver name.
 */
public class CarSpinnerAdapter extends ArrayAdapter<SpeedHiveCar> {

    private final List<SpeedHiveCar> cars;
    private final LayoutInflater inflater;

    public CarSpinnerAdapter(Context context, List<SpeedHiveCar> cars) {
        super(context, android.R.layout.simple_spinner_item, cars);
        this.cars = cars;
        this.inflater = LayoutInflater.from(context);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Use system layout for closed view
        View view = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        TextView textView = view.findViewById(android.R.id.text1);
        
        SpeedHiveCar car = cars.get(position);
        textView.setText(car.getDisplayName());
        
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        // Use system layout for dropdown view
        View view = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        TextView textView = view.findViewById(android.R.id.text1);
        
        SpeedHiveCar car = cars.get(position);
        textView.setText(car.getDisplayName());
        
        return view;
    }
}
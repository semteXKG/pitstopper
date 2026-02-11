package at.semmal.pitstopper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

/**
 * Custom spinner adapter for SpeedHive events.
 * Shows event name, location, and a LIVE badge for active events.
 */
public class EventSpinnerAdapter extends ArrayAdapter<SpeedHiveEvent> {

    private final List<SpeedHiveEvent> events;
    private final LayoutInflater inflater;

    public EventSpinnerAdapter(Context context, List<SpeedHiveEvent> events) {
        super(context, android.R.layout.simple_spinner_item, events);
        this.events = events;
        this.inflater = LayoutInflater.from(context);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Use system layout for closed view - this works
        View view = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        TextView textView = view.findViewById(android.R.id.text1);
        
        SpeedHiveEvent event = events.get(position);
        String displayText = event.getName();
        if (event.isLive()) {
            displayText = "🔴 " + displayText;
        }
        textView.setText(displayText);
        
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        // Dropdown view - detailed layout
        View view = inflater.inflate(R.layout.spinner_event_item, parent, false);
        
        SpeedHiveEvent event = events.get(position);

        TextView nameText = view.findViewById(R.id.textEventName);
        TextView locationText = view.findViewById(R.id.textEventLocation);
        TextView liveBadge = view.findViewById(R.id.textLiveBadge);

        nameText.setText(event.getName());

        String location = event.getLocationDisplay();
        if (!location.isEmpty()) {
            locationText.setText(location);
            locationText.setVisibility(View.VISIBLE);
        } else {
            locationText.setVisibility(View.GONE);
        }

        liveBadge.setVisibility(event.isLive() ? View.VISIBLE : View.GONE);

        return view;
    }
}

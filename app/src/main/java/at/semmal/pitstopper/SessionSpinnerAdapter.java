package at.semmal.pitstopper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

/**
 * Custom spinner adapter for SpeedHive sessions.
 * Shows session name, info, and a LIVE badge for active sessions.
 */
public class SessionSpinnerAdapter extends ArrayAdapter<SpeedHiveSession> {

    private final List<SpeedHiveSession> sessions;
    private final LayoutInflater inflater;

    public SessionSpinnerAdapter(Context context, List<SpeedHiveSession> sessions) {
        super(context, android.R.layout.simple_spinner_item, sessions);
        this.sessions = sessions;
        this.inflater = LayoutInflater.from(context);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }
    
    @Override
    public int getCount() {
        // Add 1 for AUTO option
        return sessions.size() + 1;
    }
    
    @Override
    public SpeedHiveSession getItem(int position) {
        if (position == 0) {
            // Return a special AUTO session object
            return new SpeedHiveSession(PitWindowPreferences.SPEEDHIVE_SESSION_AUTO, "", "AUTO", "", 
                                       0, "", "", "", 0, false);
        }
        return sessions.get(position - 1);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Use system layout for closed view - this works
        View view = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        TextView textView = view.findViewById(android.R.id.text1);
        
        if (position == 0) {
            textView.setText("AUTO");
        } else {
            SpeedHiveSession session = sessions.get(position - 1);
            String displayText = session.getDisplayName();
            if (session.isActive()) {
                displayText = "🔴 " + displayText;
            }
            textView.setText(displayText);
        }
        
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (position == 0) {
            // AUTO option - use simple layout
            View view = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            TextView textView = view.findViewById(android.R.id.text1);
            textView.setText("AUTO (detect latest session with car)");
            return view;
        }
        
        // Dropdown view - detailed layout for regular sessions
        View view = inflater.inflate(R.layout.spinner_session_item, parent, false);
        
        SpeedHiveSession session = sessions.get(position - 1);

        TextView nameText = view.findViewById(R.id.textSessionName);
        TextView infoText = view.findViewById(R.id.textSessionInfo);
        TextView liveBadge = view.findViewById(R.id.textLiveBadge);

        nameText.setText(session.getDisplayName());

        // Build info line: laps + best lap time
        StringBuilder info = new StringBuilder();
        if (session.getLaps() > 0) {
            info.append(session.getLaps()).append(" laps");
        }
        if (session.getBestLapTime() != null && !session.getBestLapTime().isEmpty()) {
            if (info.length() > 0) info.append(" · ");
            info.append("Best: ").append(session.getBestLapTime());
        }

        if (info.length() > 0) {
            infoText.setText(info.toString());
            infoText.setVisibility(View.VISIBLE);
        } else {
            infoText.setVisibility(View.GONE);
        }

        liveBadge.setVisibility(session.isActive() ? View.VISIBLE : View.GONE);

        return view;
    }
}

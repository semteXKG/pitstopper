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
    public View getView(int position, View convertView, ViewGroup parent) {
        // Use system layout for closed view - this works
        View view = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        TextView textView = view.findViewById(android.R.id.text1);
        
        SpeedHiveSession session = sessions.get(position);
        String displayText = session.getDisplayName();
        if (session.isActive()) {
            displayText = "🔴 " + displayText;
        }
        textView.setText(displayText);
        
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        // Dropdown view - detailed layout
        View view = inflater.inflate(R.layout.spinner_session_item, parent, false);
        
        SpeedHiveSession session = sessions.get(position);

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

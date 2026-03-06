package at.semmal.pitstopper.model;

public class ChatMessage {

    private final String from;
    private final String text;
    private final long receivedAt;
    private final boolean isNotification;
    private final boolean isAlert;

    public ChatMessage(String from, String text, long receivedAt) {
        this(from, text, receivedAt, false, false);
    }

    public ChatMessage(String from, String text, long receivedAt, boolean isNotification, boolean isAlert) {
        this.from = from;
        this.text = text;
        this.receivedAt = receivedAt;
        this.isNotification = isNotification;
        this.isAlert = isAlert;
    }

    public String getFrom()           { return from; }
    public String getText()           { return text; }
    public long getReceivedAt()       { return receivedAt; }
    public boolean isNotification()   { return isNotification; }
    public boolean isAlert()          { return isAlert; }
}

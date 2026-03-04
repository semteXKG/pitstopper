package at.semmal.pitstopper.model;

public class ChatMessage {

    private final String from;
    private final String text;
    private final long receivedAt;

    public ChatMessage(String from, String text, long receivedAt) {
        this.from = from;
        this.text = text;
        this.receivedAt = receivedAt;
    }

    public String getFrom()      { return from; }
    public String getText()      { return text; }
    public long getReceivedAt()  { return receivedAt; }
}

package at.semmal.pitstopper.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.model.ChatMessage;

public class ChatModule extends CenterModule {

    private final RecyclerView recycler;
    private final TextView textNoMessages;
    private final ChatAdapter adapter;

    public ChatModule(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.module_chat, this, true);

        recycler       = findViewById(R.id.recyclerChat);
        textNoMessages = findViewById(R.id.textNoMessages);

        adapter = new ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(context);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        recycler.setAdapter(adapter);
    }

    public void addMessage(ChatMessage msg) {
        adapter.addMessage(msg);
        textNoMessages.setVisibility(View.GONE);
        recycler.scrollToPosition(adapter.getItemCount() - 1);
    }

    public RecyclerView getRecyclerView() {
        return recycler;
    }

    @Override
    public void onActivate() {
        setVisibility(View.VISIBLE);
    }

    @Override
    public void onDeactivate() {
        animate().cancel();
        setTranslationY(0);
        setVisibility(View.GONE);
    }
}

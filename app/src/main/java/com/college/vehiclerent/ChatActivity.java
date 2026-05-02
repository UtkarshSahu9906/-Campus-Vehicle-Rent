package com.college.vehiclerent;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.vehiclerent.adapter.ChatAdapter;
import com.college.vehiclerent.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private String receiverId, receiverName;
    private String currentUserId;
    private String chatId;

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private EditText etMessage;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        receiverId = getIntent().getStringExtra("receiverId");
        receiverName = getIntent().getStringExtra("receiverName");
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Create a unique chatId by sorting UIDs
        if (currentUserId.compareTo(receiverId) < 0) {
            chatId = currentUserId + "_" + receiverId;
        } else {
            chatId = receiverId + "_" + currentUserId;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(receiverName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        etMessage = findViewById(R.id.etMessage);
        recyclerView = findViewById(R.id.chatRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messages);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());

        listenForMessages();
    }

    private void listenForMessages() {
        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        messages.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            messages.add(doc.toObject(ChatMessage.class));
                        }
                        adapter.notifyDataSetChanged();
                        if (!messages.isEmpty()) {
                            recyclerView.smoothScrollToPosition(messages.size() - 1);
                        }
                    }
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        ChatMessage message = new ChatMessage(currentUserId, receiverId, text, System.currentTimeMillis());
        etMessage.setText("");

        db.collection("chats").document(chatId).collection("messages")
                .add(message)
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

package com.example.chat_application;

import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.SearchView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChattingSpace extends AppCompatActivity implements TextWatcher {

    private String name;
    private WebSocket webSocket;
    private EditText messageEdit;
    private View sendBtn, pickImgBtn;
    private RecyclerView recyclerView;
    private final int MEDIA_REQUEST_ID = 1;
    private MessageAdapter messageAdapter;

    private FirebaseFirestore db;
    private CollectionReference chatRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting_space);

        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Chat Room");

        name = getIntent().getStringExtra("name");
        db = FirebaseFirestore.getInstance();
        chatRef = db.collection("chats");

        initiateSocketConnection();  // Connect to WebSocket server
        loadChatHistory();           // Load messages from Firestore
    }

    // Establish WebSocket connection using OkHttp
    private void initiateSocketConnection() {
        OkHttpClient client = new OkHttpClient();
        String SERVER_PATH = "ws://192.168.10.2:3000";
        Request request = new Request.Builder().url(SERVER_PATH).build();
        webSocket = client.newWebSocket(request, new SocketListener());
    }

    // Load previous chat messages from Firestore database
    private void loadChatHistory() {
        recyclerView = findViewById(R.id.recyclerView);
        messageAdapter = new MessageAdapter(getLayoutInflater());
        recyclerView.setAdapter(messageAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        chatRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Listen failed.", error);
                        return;
                    }

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Map<String, Object> doc = dc.getDocument().getData();
                        JSONObject jsonObject = new JSONObject();
                        Log.d("Firestore", "Doc added: " + new JSONObject(doc).toString());

                        try {
                            String sender = (String) doc.get("name");

                            jsonObject.put("name", sender);
                            jsonObject.put("timestamp", doc.get("timestamp"));
                            jsonObject.put("read", doc.containsKey("read") && (Boolean) doc.get("read"));

                            // Extract various media or message types if present
                            if (doc.containsKey("message")) jsonObject.put("message", doc.get("message"));
                            if (doc.containsKey("image")) jsonObject.put("image", doc.get("image"));
                            if (doc.containsKey("audio")) jsonObject.put("audio", doc.get("audio"));
                            if (doc.containsKey("video")) jsonObject.put("video", doc.get("video"));
                            if (doc.containsKey("gif")) jsonObject.put("gif", doc.get("gif"));

                            jsonObject.put("isSent", sender.equals(name));
                            messageAdapter.addItem(jsonObject);
                            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    // WebSocket listener class for receiving and handling messages
    private class SocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            runOnUiThread(() -> {
                Toast.makeText(ChattingSpace.this, "Socket Connected", Toast.LENGTH_SHORT).show();
                initializeView();
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            runOnUiThread(() -> {
                try {
                    JSONObject jsonObject = new JSONObject(text);
                    jsonObject.put("isSent", false);
                    messageAdapter.addItem(jsonObject);
                    recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        markMessagesAsRead(); // Update unread messages
    }

    // Set "read" status to true for incoming messages
    private void markMessagesAsRead() {
        chatRef.whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String sender = doc.getString("name");
                        if (!Objects.equals(sender, name)) {
                            doc.getReference().update("read", true);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to mark as read", e));
    }

    // Initialize UI views and button click listeners
    private void initializeView() {
        messageEdit = findViewById(R.id.messageEdit);
        sendBtn = findViewById(R.id.sendBtn);
        pickImgBtn = findViewById(R.id.pickImgBtn);
        messageEdit.addTextChangedListener(this);

        sendBtn.setOnClickListener(v -> {
            String msg = messageEdit.getText().toString();
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("name", name);
                jsonObject.put("message", msg);
                jsonObject.put("timestamp", System.currentTimeMillis());
                webSocket.send(jsonObject.toString());  // Send via WebSocket
                saveToFirestore(jsonObject);           // Save to Firestore
                jsonObject.put("isSent", true);
                jsonObject.put("read", false);
                messageAdapter.addItem(jsonObject);
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                resetMessageEdit();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        // File picker for media (image/audio/video/gif)
        pickImgBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                    "image/*", "audio/*", "video/*","gif/*"
            });
            startActivityForResult(Intent.createChooser(intent, "Select media"), MEDIA_REQUEST_ID);
        });

        // Register FCM token with server
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        JSONObject tokenJson = new JSONObject();
                        try {
                            tokenJson.put("type", "register_token");
                            tokenJson.put("token", token);
                            tokenJson.put("name", name);
                            webSocket.send(tokenJson.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    // Reset message box after sending
    private void resetMessageEdit() {
        messageEdit.removeTextChangedListener(this);
        messageEdit.setText("");
        sendBtn.setVisibility(View.INVISIBLE);
        pickImgBtn.setVisibility(View.VISIBLE);
        messageEdit.addTextChangedListener(this);
    }

    // Save sent message to Firestore
    private void saveToFirestore(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("name", jsonObject.getString("name"));
            if (jsonObject.has("message")) map.put("message", jsonObject.getString("message"));
            if (jsonObject.has("image")) map.put("image", jsonObject.getString("image"));
            if (jsonObject.has("audio")) map.put("audio", jsonObject.getString("audio"));
            if (jsonObject.has("video")) map.put("video", jsonObject.getString("video"));
            if (jsonObject.has("gif")) map.put("gif", jsonObject.getString("gif"));
            map.put("timestamp", System.currentTimeMillis());
            map.put("read", false);
            chatRef.add(map);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // --- TextWatcher methods to handle send button visibility ---
    @Override
    public void afterTextChanged(Editable s) {
        if (s.toString().trim().isEmpty()) {
            resetMessageEdit();
        } else {
            sendBtn.setVisibility(View.VISIBLE);
            pickImgBtn.setVisibility(View.INVISIBLE);
        }
    }
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

    // --- Handling search bar functionality (SearchView) ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        searchIcon.setColorFilter(Color.WHITE);

        EditText searchEdit = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEdit.setTextColor(Color.WHITE);
        searchEdit.setHintTextColor(Color.LTGRAY);
        searchView.setQueryHint("Search messages...");

        // Listen for search input
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                messageAdapter.filter(newText);  // Trigger filtering in adapter
                return true;
            }
        });

        // Reset filter when search is closed
        searchView.setOnCloseListener(() -> {
            messageAdapter.filter("");
            return false;
        });

        return true;
    }

}

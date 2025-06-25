package com.example.parasuratanuser;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class FavoriteActivity extends AppCompatActivity {

    private RecyclerView favoriteRecyclerView;
    private FavoriteAdapter favoriteAdapter;
    private TextView favoriteisempty;
    private ProgressBar loadingIndicator;
    private final List<BookDetails> favoriteList = new ArrayList<>();
    private static final String TAG = "FavoriteActivity";
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorite);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#1C1C1C"));
        }
        loadingIndicator = findViewById(R.id.loadingIndicator);
        db = AppDatabase.getDatabase(this); // Initialize Room DB
        findViewById(R.id.ProgressCheck).setOnClickListener(v -> startActivity(new Intent(this, ProgressActivity.class)));
        findViewById(R.id.backButton).setOnClickListener(v -> startActivity(new Intent(this, LibraryActivity.class)));
        favoriteisempty = findViewById(R.id.favoriteisempty);
        favoriteRecyclerView = findViewById(R.id.FavoriteRecycler);
        favoriteRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        favoriteAdapter = new FavoriteAdapter(favoriteList);
        favoriteRecyclerView.setAdapter(favoriteAdapter);

        loadFavorites();
    }
    @Override
    protected void onResume() {
        super.onResume();
        reloadFavorite(); // Lightweight refresh to update only if needed
    }

    private void reloadFavorite() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<FavoriteEntity> savedItems = db.favoriteDao().getAllFavorites();

            List<BookDetails> updatedList = new ArrayList<>();
            for (FavoriteEntity entity : savedItems) {
                BookDetails b = new BookDetails();
                b.setDocumentId(entity.id);
                b.setTitle(entity.title);
                b.setAuthor(entity.author);
                b.setYear(entity.year);
                b.setPublisher(entity.publisher);
                b.setGenre(entity.genre);
                b.setDescription(entity.description);
                b.setTags(entity.tags);
                b.setImageBase64(entity.imageBase64);
                updatedList.add(b);
            }

            runOnUiThread(() -> {
                favoriteList.clear();
                favoriteList.addAll(updatedList);
                favoriteAdapter.notifyDataSetChanged();
                favoriteisempty.setVisibility(favoriteList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }
    private void loadFavorites() {
        loadingIndicator.setVisibility(View.VISIBLE);

        // Make sure you have a global reference to the DB
        AppDatabase db = AppDatabase.getDatabase(this);
        FirestoreSync.init(db); // Initialize FirestoreSync with DB instance

        // Sync from Firestore, then load from Room DB
        FirestoreSync.syncFavoritesFromFirestore(this, () -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                List<FavoriteEntity> savedItems = db.favoriteDao().getAllFavorites();
                favoriteList.clear();

                for (FavoriteEntity entity : savedItems) {
                    BookDetails b = new BookDetails();
                    b.setDocumentId(entity.id);
                    b.setTitle(entity.title);
                    b.setAuthor(entity.author);
                    b.setYear(entity.year);
                    b.setPublisher(entity.publisher);
                    b.setGenre(entity.genre);
                    b.setDescription(entity.description);
                    b.setTags(entity.tags);
                    b.setImageBase64(entity.imageBase64);
                    favoriteList.add(b);
                }

                runOnUiThread(() -> {
                    favoriteAdapter.notifyDataSetChanged();
                    favoriteisempty.setVisibility(favoriteList.isEmpty() ? View.VISIBLE : View.GONE);
                    loadingIndicator.setVisibility(View.GONE);
                    Log.d(TAG, "✅ Loaded " + favoriteList.size() + " favorites (Room synced with Firestore)");
                });
            });
        });
    }




}

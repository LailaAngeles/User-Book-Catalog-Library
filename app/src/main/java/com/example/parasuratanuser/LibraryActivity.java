package com.example.parasuratanuser;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class LibraryActivity extends AppCompatActivity {

    private RecyclerView libraryRecyclerView;
    private LibraryAdapter libraryAdapter;
    private TextView libraryisempty;
    private ProgressBar loadingIndicator;
    private final List<BookDetails> libraryList = new ArrayList<>();
    private static final String TAG = "LibraryActivity";
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#1C1C1C"));
        }

        db = AppDatabase.getDatabase(this);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        libraryisempty = findViewById(R.id.libraryisempty);
        libraryRecyclerView = findViewById(R.id.libraryRecyclerView);
        libraryRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        libraryAdapter = new LibraryAdapter(libraryList);
        libraryRecyclerView.setAdapter(libraryAdapter);

        // Navigation buttons
        findViewById(R.id.Home).setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.searchbtn).setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.ProgressCheck).setOnClickListener(v -> startActivity(new Intent(this, ProgressActivity.class)));
        findViewById(R.id.Favorite).setOnClickListener(v -> startActivity(new Intent(this, FavoriteActivity.class)));

        loadLibrary(); // Corrected
    }
    //Ito yung method na tinatawag para kunin ang latest data mula Firestore at i-save ito locally gamit Room Database.
    private void loadLibrary() {
        loadingIndicator.setVisibility(View.VISIBLE); // Show loading while syncing

        FirestoreSync.init(AppDatabase.getDatabase(this));

        // Sync Library with Toast messages and proper context
        FirestoreSync.syncLibraryFromFirestore(this, () -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                List<LibraryEntity> savedItems = db.libraryDao().getAll();
                libraryList.clear();

                for (LibraryEntity entity : savedItems) {
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
                    libraryList.add(b);
                }

                runOnUiThread(() -> {
                    libraryAdapter.notifyDataSetChanged();
                    libraryisempty.setVisibility(libraryList.isEmpty() ? View.VISIBLE : View.GONE);
                    loadingIndicator.setVisibility(View.GONE); // Hide after loading is done
                    Log.d(TAG, "✅ Loaded " + libraryList.size() + " library items from Room after sync");
                });
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadLibrary(); // Lightweight refresh to update only if needed
    }

    private void reloadLibrary() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<LibraryEntity> savedItems = db.libraryDao().getAll();

            List<BookDetails> updatedList = new ArrayList<>();
            for (LibraryEntity entity : savedItems) {
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
                libraryList.clear();
                libraryList.addAll(updatedList);
                libraryAdapter.notifyDataSetChanged();
                libraryisempty.setVisibility(libraryList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

}

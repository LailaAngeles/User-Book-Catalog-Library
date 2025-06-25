package com.example.parasuratanuser;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class BookDetailsActivity extends AppCompatActivity {

    private static final String TAG = "BookDetailsActivity";

    // mga view na gagamitin natin
    private TextView titleTv, authorTv, yearTv, publisherTv, genreTv, descriptionTv, tagsTv, BookId;
    private ImageView coverIv, storyIv;
    private Spinner statusSpinner;
    private AppCompatButton libraryBtn;
    private ImageButton favoriteBtn;
    ProgressBar loadingIndicator;

    // para sa recommendations
    private RecyclerView recView;
    private BookRecAdaptor recAdaptor;
    private final List<BookDetails> recList = new ArrayList<>();

    // pang track ng state kung nasa library/favorite na ba
    private String docId;
    private boolean inLibrary = false;
    private boolean inFavorite = false;
    private ArrayAdapter<String> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book);

        // palitan natin yung kulay ng status bar para aesthetic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = getWindow();
            w.setStatusBarColor(Color.parseColor("#1C1C1C"));
        }

        // initialize lahat ng view na ginamit natin
        findViews();

        // kunin yung data galing sa intent at ipakita sa UI
        populateFromIntent();

        // setup para sa status dropdown
        initSpinner();

        // setup para sa mga recommended books
        initRecView();

        // setup para sa mga button like back, favorite, at library
        initButtons();

        // kunin yung recommended books
        fetchRecommendations();

        // check natin kung nasa library/fav na ba yung book
        refreshFlags();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // kapag bumalik ka sa activity, i-refresh ulit status
        refreshFlags();
    }

    // hanapin lahat ng views na nasa XML layout
    private void findViews() {
        BookId = findViewById(R.id.BookId);
        titleTv = findViewById(R.id.storyName);
        authorTv = findViewById(R.id.author);
        yearTv = findViewById(R.id.year);
        publisherTv = findViewById(R.id.publisher);
        genreTv = findViewById(R.id.genre);
        descriptionTv = findViewById(R.id.storyDescription);
        tagsTv = findViewById(R.id.tags);
        coverIv = findViewById(R.id.storycover);
        storyIv = findViewById(R.id.storyImage);
        statusSpinner = findViewById(R.id.bookstatus);
        libraryBtn = findViewById(R.id.libraryButton);
        favoriteBtn = findViewById(R.id.favoriteButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);
    }

    // kunin data na pinasa gamit Intent at ipasok sa UI
    // Kinukuha dito yung mga data na galing sa previous activity gamit 'Intent'
    // Halimbawa: title, author, genre, image, etc.
    // Tapos nilalagay ito sa mga TextView para makita ng user
    // May dagdag check din kung may `docId` na or wala. Kung wala, hahanapin nya sa Firestore gamit yung title + author
    // Para makuha yung tamang dokumento ng book sa Firestore

    private void populateFromIntent() {
        Intent i = getIntent();
        docId = i.getStringExtra("documentId");
        String title = i.getStringExtra("title");
        String author = i.getStringExtra("author");

        // kunin ung id sa firestore
        if (docId == null || docId.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("Documents")
                    .whereEqualTo("title", title)
                    .whereEqualTo("author", author)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            docId = doc.getId();
                            BookId.setText(docId); // Make sure it's visible
                            Log.d(TAG, "✅ Fetched docId from Firestore: " + docId);
                            refreshFlags(); // refresh library/fav state with real docId
                        } else {
                            Log.e(TAG, "❌ No Firestore document found for title-author combo.");
                            Toast.makeText(this, "Book not found in Firestore", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to fetch docId from Firestore", e);
                        Toast.makeText(this, "Error fetching book from Firestore", Toast.LENGTH_SHORT).show();
                    });
        } else {
            BookId.setText(docId); // Already provided from Intent, show it
        }


        // set text sa mga TextView
        BookId.setText("Book ID: " + docId);
        titleTv.setText(title);
        authorTv.setText("by: " + author);
        yearTv.setText("on: " + i.getStringExtra("year"));
        publisherTv.setText("Publisher: " + i.getStringExtra("publisher"));
        genreTv.setText("Genre: " + i.getStringExtra("genre"));
        descriptionTv.setText(i.getStringExtra("description"));
        tagsTv.setText(i.getStringExtra("tags"));

        // kunin image galing shared prefs kung meron
        String b64 = getSharedPreferences("Images", MODE_PRIVATE).getString(title, null);
        loadImageFromBase64(b64);

        // kung may pinasa na base64 image sa intent, gamitin yun
        loadImageFromBase64(i.getStringExtra("imageBase64"));
    }

    // setup ng status spinner (dropdown)
    // Para sa dropdown na nagpapakita ng status ng binabasa mo (To Read, Reading, Done Reading)
    // Naka-disable ito by default, kasi magiging enabled lang siya kapag nasa library or favorite na yung book
    // Kapag pinili mo yung ibang status, i-uupdate niya yung Room Database gamit `updateStatusInRoom()`

    private void initSpinner() {
        ArrayList<String> opts = new ArrayList<>();
        opts.add("To Read");
        opts.add("Reading");
        opts.add("Done Reading");

        spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opts);
        spinnerAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        statusSpinner.setAdapter(spinnerAdapter);
        statusSpinner.setEnabled(false); // di pa enabled hanggang nasa library/fav

        // kapag may pinili sa dropdown
        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean first = true;
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (first) { first = false; return; }
                String newStatus = (String) p.getItemAtPosition(pos);
                updateStatusInRoom(newStatus); // update natin sa Room DB
                Toast.makeText(BookDetailsActivity.this,
                        "📌 Status Updated: " + newStatus, Toast.LENGTH_SHORT).show();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    // setup ng RecyclerView para sa recommendations
    private void initRecView() {
        recView = findViewById(R.id.RandomRecommendation);
        recView.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false));
        recAdaptor = new BookRecAdaptor(recList);
        recView.setAdapter(recAdaptor);
        fetchRecommendations(); // kuha ng data
    }

    // setup ng buttons like back, fav, at library
    private void initButtons() {
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        favoriteBtn.setOnClickListener(v -> {
            if (inFavorite) removeFavoriteItem(); else addFavoriteItem();
        });

        libraryBtn.setOnClickListener(v -> {
            if (inLibrary) removeLibraryItem(); else addLibraryItem();
        });
    }

    // kunin recommended books from Firestore
    private void fetchRecommendations() {
        loadingIndicator.setVisibility(View.VISIBLE);
        String currentGenre = genreTv.getText().toString().replace("Genre: ", "").trim();
        String currentTitle = titleTv.getText().toString().trim();
        String currentAuthor = authorTv.getText().toString().replace("by: ", "").trim();

        FirebaseFirestore.getInstance()
                .collection("Documents")
                .whereEqualTo("genre", currentGenre)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    recList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        BookDetails book = doc.toObject(BookDetails.class);
                        if (book != null) {
                            // wag isama kung siya rin yung current book
                            if (book.getTitle().equalsIgnoreCase(currentTitle) &&
                                    book.getAuthor().equalsIgnoreCase(currentAuthor)) {
                                continue;
                            }
                            recList.add(book);
                        } else {
                            Log.w(TAG, "⚠️ Skipped null book object: " + doc.getId());
                        }
                    }
                    recAdaptor.notifyDataSetChanged();
                    loadingIndicator.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    loadingIndicator.setVisibility(View.GONE);
                    Log.e(TAG, "❌ Failed to fetch recommendations", e);
                    Toast.makeText(this, "Failed to load recommendations", Toast.LENGTH_SHORT).show();
                });
    }

    // check kung nasa Room DB na yung book
    private void refreshFlags() {
        AppDatabase db = AppDatabase.getDatabase(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            LibraryEntity lib = db.libraryDao().getLibraryItemById(docId);
            FavoriteEntity fav = db.favoriteDao().getFavoriteItemById(docId);

            runOnUiThread(() -> {
                inLibrary = lib != null;
                inFavorite = fav != null;

                libraryBtn.setText(inLibrary ? "✅ Added Successfully" : "Add to Library");
                favoriteBtn.setImageResource(inFavorite ? R.drawable.redfav : R.drawable.favorite);
                statusSpinner.setEnabled(inLibrary || inFavorite);

                if (lib != null) syncSpinner(lib.status);
                else if (fav != null) syncSpinner(fav.status);
            });
        });
    }

    // i-sync yung dropdown sa tamang status
    private void syncSpinner(String status) {
        int p = spinnerAdapter.getPosition(status);
        if (p >= 0) statusSpinner.setSelection(p);
    }

    // I-uupdate niya yung status ng book sa Room database kung may pagbabago sa spinner
// Sinisigurado na updated ang status sa local DB (Room) kung naka-fav or nasa library

    private void updateStatusInRoom(String newStatus) {
        AppDatabase db = AppDatabase.getDatabase(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            if (inLibrary) db.libraryDao().updateStatus(docId, newStatus);
            if (inFavorite) db.favoriteDao().updateStatus(docId, newStatus);
        });
    }

    // ------------ ADD/REMOVE FUNCTIONS ------------ //
        // Kapag gusto mong idagdag sa library yung book, dito pumapasok
        // Gumagawa siya ng bagong LibraryEntity object, then sinisave sa Room DB
        // Tapos, nag-uupdate ng UI para ipakitang successful ang pag-add

    private void addLibraryItem() {
        AppDatabase db = AppDatabase.getDatabase(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            LibraryEntity e = new LibraryEntity();
            fillEntityCommon(e);
            db.libraryDao().insert(e);
            runOnUiThread(() -> {
                inLibrary = true;
                libraryBtn.setText("✅ Added Successfully");
                statusSpinner.setEnabled(true);
                Toast.makeText(this, "📚 Added to your Library!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void removeLibraryItem() {
        AppDatabase db = AppDatabase.getDatabase(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            db.libraryDao().deleteById(docId);
            runOnUiThread(() -> {
                inLibrary = false;
                libraryBtn.setText("Add to Library");
                if (!inFavorite) statusSpinner.setEnabled(false);
                Toast.makeText(this, "📕 Removed from your Library", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void addFavoriteItem() {
        AppDatabase db = AppDatabase.getDatabase(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            FavoriteEntity f = new FavoriteEntity();
            fillEntityCommon(f);
            db.favoriteDao().insert(f);
            runOnUiThread(() -> {
                inFavorite = true;
                favoriteBtn.setImageResource(R.drawable.redfav);
                statusSpinner.setEnabled(true);
                Toast.makeText(this, "❤️ Added to Favorites!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void removeFavoriteItem() {
        AppDatabase db = AppDatabase.getDatabase(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            db.favoriteDao().deleteById(docId);
            runOnUiThread(() -> {
                inFavorite = false;
                favoriteBtn.setImageResource(R.drawable.favorite);
                if (!inLibrary) statusSpinner.setEnabled(false);
                Toast.makeText(this, "💔 Removed from Favorites", Toast.LENGTH_SHORT).show();
            });
        });
    }

    // ginagamit natin to para mapunan yung data sa entity bago isave
    private void fillEntityCommon(Object entity) {
        String status = (String) statusSpinner.getSelectedItem();

        if (entity instanceof LibraryEntity) {
            LibraryEntity e = (LibraryEntity) entity;
            e.id = docId; e.status = status;
            e.title = titleTv.getText().toString();
            e.author = authorTv.getText().toString().replace("by: ", "");
            e.description = descriptionTv.getText().toString();
            e.year = yearTv.getText().toString().replace("on: ", "");
            e.publisher = publisherTv.getText().toString().replace("Publisher: ", "");
            e.genre = genreTv.getText().toString().replace("Genre: ", "");
            e.tags = tagsTv.getText().toString();
            e.imageBase64 = getBase64FromImage(coverIv);
        } else if (entity instanceof FavoriteEntity) {
            FavoriteEntity f = (FavoriteEntity) entity;
            f.id = docId; f.status = status;
            f.title = titleTv.getText().toString();
            f.author = authorTv.getText().toString().replace("by: ", "");
            f.description = descriptionTv.getText().toString();
            f.year = yearTv.getText().toString().replace("on: ", "");
            f.publisher = publisherTv.getText().toString().replace("Publisher: ", "");
            f.genre = genreTv.getText().toString().replace("Genre: ", "");
            f.tags = tagsTv.getText().toString();
            f.imageBase64 = getBase64FromImage(coverIv);
        }
    }

    // ------------ IMAGE UTILS ------------ //

    // Kung may base64 string para sa image (cover ng book), i-coconvert niya ito into Bitmap para ma-display sa ImageView
    // May fallback din siya in case walang image

    // decode image from base64 string
    private void loadImageFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return;
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp != null) {
                coverIv.setImageBitmap(bmp);
                storyIv.setImageBitmap(bmp);
            }
        } catch (Exception e) {
            Log.e(TAG, "img decode failed", e);
        }
    }

    // kunin image galing ImageView tapos i-convert to base64
    private String getBase64FromImage(ImageView iv) {
        if (iv == null || iv.getDrawable() == null) return "";
        if (!(iv.getDrawable() instanceof BitmapDrawable)) return "";

        BitmapDrawable bd = (BitmapDrawable) iv.getDrawable();
        Bitmap bmp = bd.getBitmap();
        if (bmp == null) return "";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }
}

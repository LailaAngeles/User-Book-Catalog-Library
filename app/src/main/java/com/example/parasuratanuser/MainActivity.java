package com.example.parasuratanuser;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parasuratanuser.BookDetails;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
public class MainActivity extends AppCompatActivity {
    private TextView realisticFictionEmpty, mythFolkEmpty, educInfoEmpty, fanAdvEmpty, unknownGenreEmpty;

    private RecyclerView recyclerView, fictionRecyclerView, mythFolkRecycler, educInfoRecycler, fanAdvRecycler, unknownGenreRecycler;
    private BookAdaptor bookAdaptor;
    private FictionAdapter fictionAdapter;
    private ProgressBar loadingIndicator;
    private TextView realisticFictionCategory, UnknownGenreChategory,MythFolkChategory, EducInfoChategory,FanAdvChategory;

    private List<BookDetails> bookList = new ArrayList<>();
    private CollectionReference booksRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadingIndicator = findViewById(R.id.loadingIndicator);
        realisticFictionCategory = findViewById(R.id.RealisticFictionChategory);
        UnknownGenreChategory = findViewById(R.id.UnknownGenreChategory);
        MythFolkChategory = findViewById(R.id.MythFolkChategory);
        EducInfoChategory = findViewById(R.id.EducInfoChategory);
        FanAdvChategory = findViewById(R.id.FanAdvChategory);

        recyclerView = findViewById(R.id.contentrecycler);
        fictionRecyclerView = findViewById(R.id.RealisticFiction);
        mythFolkRecycler = findViewById(R.id.MythFolk);
        educInfoRecycler = findViewById(R.id.EducInfo);
        fanAdvRecycler = findViewById(R.id.FanAdv);
        unknownGenreRecycler = findViewById(R.id.UnknownGenre);
        realisticFictionEmpty = findViewById(R.id.RealisticFictionEmpty);
        mythFolkEmpty = findViewById(R.id.mythFolkEmpty);
        educInfoEmpty = findViewById(R.id.educInfoEmpty);
        fanAdvEmpty = findViewById(R.id.fanAdvEmpty);
        unknownGenreEmpty = findViewById(R.id.unknownGenreEmpty);


        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        fictionRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mythFolkRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        educInfoRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        fanAdvRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        unknownGenreRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Set up buttons
        ImageButton searchBtn = findViewById(R.id.searchbtn);
        searchBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchActivity.class)));

        ImageButton libraryBtn = findViewById(R.id.library);
        libraryBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LibraryActivity.class)));

        // Handle insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.write_page), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Change status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#1C1C1C"));
        }

        fetchBooks();
    }

    private void fetchBooks() {
        loadingIndicator.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        booksRef = db.collection("Documents");

        booksRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<BookDetails> allBooks = new ArrayList<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        BookDetails book = document.toObject(BookDetails.class);
                        if (book != null) {
                            allBooks.add(book);
                        }
                    }

                    // Setup adapters
                    bookAdaptor = new BookAdaptor(allBooks);
                    recyclerView.setAdapter(bookAdaptor);

                    setupGenreRecycler(fictionRecyclerView, realisticFictionCategory, realisticFictionEmpty, allBooks, "Realistic Fiction");
                    setupGenreRecycler(mythFolkRecycler, MythFolkChategory, mythFolkEmpty, allBooks, "Mythology/Folklore");
                    setupGenreRecycler(educInfoRecycler, EducInfoChategory, educInfoEmpty, allBooks, "Educational/Informational");
                    setupGenreRecycler(fanAdvRecycler, FanAdvChategory, fanAdvEmpty, allBooks, "Fantasy/Adventure");
                    setupGenreRecycler(unknownGenreRecycler, UnknownGenreChategory, unknownGenreEmpty, allBooks, "Unknown Genre");


                    loadingIndicator.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    loadingIndicator.setVisibility(View.GONE);
                    // lalagay ako error mess wag ko sana makalimutan
                });
    }

    private void setupGenreRecycler(RecyclerView recycler, TextView label, TextView emptyText, List<BookDetails> books, String genre) {
        List<BookDetails> filtered = FictionAdapter.filterByGenre(books, genre);
        FictionAdapter adapter = new FictionAdapter(filtered);
        recycler.setAdapter(adapter);

        // Always show the genre label and RecyclerView
        if (label != null) {
            label.setVisibility(View.VISIBLE);
        }
        recycler.setVisibility(View.VISIBLE); // Ensure recycler is always shown (optional)

        // Show "empty" message if filtered list is empty
        if (emptyText != null) {
            emptyText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }


}

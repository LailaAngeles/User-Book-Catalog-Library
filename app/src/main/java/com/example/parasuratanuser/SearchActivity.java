package com.example.parasuratanuser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private CollectionReference booksRef;
    private List<Book> bookList = new ArrayList<>();
    private List<Book> filteredList = new ArrayList<>();
    private BookAdapter bookAdapter;
    private EditText searchBar;
    private ProgressBar loadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        loadingIndicator = findViewById(R.id.load);
        // Change status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.parseColor("#1C1C1C"));
        }

        ImageButton homeBtn = findViewById(R.id.Home);
        homeBtn.setOnClickListener(v -> {
            startActivity(new Intent(SearchActivity.this, MainActivity.class));
        });

        ImageButton libraryBtn = findViewById(R.id.library);
        if (libraryBtn != null) {
            libraryBtn.setOnClickListener(v -> {
                startActivity(new Intent(SearchActivity.this, LibraryActivity.class));
            });
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        booksRef = db.collection("Documents");

        RecyclerView recyclerView = findViewById(R.id.contentrecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookAdapter = new BookAdapter(filteredList);
        recyclerView.setAdapter(bookAdapter);

        searchBar = findViewById(R.id.search_bar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBooks(s.toString());// Tawagin ang filter function kada type mo ng text
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        /*May TextWatcher sa search bar, kaya kada mag-type ka ng kahit isang letra,
        automatic tatawagin ang filterBooks(query).
Gamit    ang onTextChanged, pinapasa niya ang text na nilagay mo papunta sa filtering logic.*/
        fetchBooks();
    }

    private void fetchBooks() {
        loadingIndicator.setVisibility(View.VISIBLE);
        booksRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Book book = document.toObject(Book.class);
                        if (book != null) {
                            bookList.add(book);
                        }
                    }
                    filteredList.clear();
                    filteredList.addAll(bookList);
                    bookAdapter.notifyDataSetChanged();
                    loadingIndicator.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(SearchActivity.this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void filterBooks(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(bookList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Book book : bookList) {
                if ((book.getTitle() != null && book.getTitle().toLowerCase().contains(lowerCaseQuery)) ||
                        (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(lowerCaseQuery)) ||
                        (book.getGenre() != null && book.getGenre().toLowerCase().contains(lowerCaseQuery)) ||
                        (book.getTags() != null && book.getTags().toLowerCase().contains(lowerCaseQuery))) {
                    filteredList.add(book);
                }
            }
        }
        bookAdapter.notifyDataSetChanged();
    }

    public String getFilePathFromUri(Uri uri) {
        String filePath = null;
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(projection[0]);
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return filePath;
    }

    // Book Model
    public static class Book {
        private String title, author, year, publisher, category, description, tags, imageUrl, imageBase64,id;

        public Book() {}
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public String getYear() { return year; }
        public void setYear(String year) { this.year = year; }

        public String getPublisher() { return publisher; }
        public void setPublisher(String publisher) { this.publisher = publisher; }

        public String getGenre() { return category; }
        public void setGenre(String category) { this.category = category; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getImageBase64() { return imageBase64; }
        public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    }

    // Book Adapter
    public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

        private List<Book> bookList;

        public BookAdapter(List<Book> bookList) {
            this.bookList = bookList;
        }

        @NonNull
        @Override
        public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_list_adaptor, parent, false);
            return new BookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
            Book book = bookList.get(position);
            holder.bookTitle.setText(book.getTitle());
            holder.bookAuthor.setText("by: " + book.getAuthor());
            holder.bookDescription.setText("Description: " + book.getDescription());
            holder.bookTags.setText(book.getTags());

            if (book.getImageBase64() != null && !book.getImageBase64().isEmpty()) {
                try {
                    byte[] decodedBytes = Base64.decode(book.getImageBase64(), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        holder.bookImage.setImageBitmap(bitmap);
                    } else {
                        holder.bookImage.setImageResource(R.drawable.placeholder_image);
                    }
                } catch (IllegalArgumentException e) {
                    // Catch Base64 decode errors
                    holder.bookImage.setImageResource(R.drawable.placeholder_image);
                }
            } else {
                holder.bookImage.setImageResource(R.drawable.placeholder_image);
            }


            holder.itemView.setOnClickListener(v -> {
                Context context = holder.itemView.getContext();

                SharedPreferences prefs = context.getSharedPreferences("Images", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(book.getTitle(), book.getImageBase64());
                editor.apply();

                Intent intent = new Intent(context, BookDetailsActivity.class);
                intent.putExtra("title", book.getTitle());
                intent.putExtra("author", book.getAuthor());
                intent.putExtra("year", book.getYear());
                intent.putExtra("publisher", book.getPublisher());
                intent.putExtra("genre", book.getGenre());
                intent.putExtra("description", book.getDescription());
                intent.putExtra("tags", book.getTags());
                context.startActivity(intent);
            });
        }

        private Bitmap getPlaceholderImage() {
            return BitmapFactory.decodeResource(getResources(), R.drawable.placeholder_image);
        }

        @Override
        public int getItemCount() {
            return bookList.size();
        }

        class BookViewHolder extends RecyclerView.ViewHolder {
            TextView bookTitle, bookAuthor, bookYear, bookPublisher, bookGenre, bookDescription, bookTags;
            ImageView bookImage;

            public BookViewHolder(@NonNull View itemView) {
                super(itemView);
                bookTitle = itemView.findViewById(R.id.book_title);
                bookAuthor = itemView.findViewById(R.id.book_author);
                bookDescription = itemView.findViewById(R.id.book_Description);
                bookTags = itemView.findViewById(R.id.book_tags);
                bookImage = itemView.findViewById(R.id.book_image);
            }
        }
    }
}

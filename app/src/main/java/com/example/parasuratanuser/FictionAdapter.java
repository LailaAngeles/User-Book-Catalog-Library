package com.example.parasuratanuser;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

// (same package declaration...)

public class FictionAdapter extends RecyclerView.Adapter<FictionAdapter.FictionViewHolder> {

    private final List<BookDetails> fictionBookList = new ArrayList<>();

    public FictionAdapter(List<BookDetails> allBooks) {
        filterFictionBooks(allBooks);
    }

    @NonNull
    @Override
    public FictionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_story_adaptor, parent, false);
        return new FictionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FictionViewHolder holder, int position) {
        BookDetails book = fictionBookList.get(position);
        holder.titleTextView.setText(book.getTitle());

        if (book.hasImage()) {
            try {
                byte[] decoded = Base64.decode(book.getImageBase64(), Base64.DEFAULT);
                holder.imageView.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
            } catch (Exception e) {
                holder.imageView.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_image);
        }

        holder.itemView.setOnClickListener(v -> {
            Context context = holder.itemView.getContext();
            SharedPreferences prefs = context.getSharedPreferences("Images", MODE_PRIVATE);
            prefs.edit().putString(book.getTitle(), book.getImageBase64()).apply();

            Intent intent = new Intent(context, BookDetailsActivity.class);
            intent.putExtra("documentId", book.getDocumentId()); // ✅ correct
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

    @Override
    public int getItemCount() {
        return fictionBookList.size();
    }

    private void filterFictionBooks(List<BookDetails> books) {
        fictionBookList.clear();
        for (BookDetails book : books) {
            if (book.getGenre() == null) continue;
            String genre = book.getGenre().trim().toLowerCase();
            if (genre.equals("realistic fiction") ||
                    genre.equals("mythology/folklore") ||
                    genre.equals("educational/informational") ||
                    genre.equals("unknown genre") ||
                    genre.equals("fantasy/adventure")) {
                fictionBookList.add(book);
            }
        }
    }
    public static List<BookDetails> filterByGenre(List<BookDetails> allBooks, String genreName) {
        List<BookDetails> filtered = new ArrayList<>();
        for (BookDetails book : allBooks) {
            if (book.getGenre() == null) continue;
            if (book.getGenre().trim().equalsIgnoreCase(genreName)) {
                filtered.add(book);
            }
        }
        return filtered;
    }


    static class FictionViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;

        FictionViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.book_image);
            titleTextView = itemView.findViewById(R.id.book_title);
        }
    }
}


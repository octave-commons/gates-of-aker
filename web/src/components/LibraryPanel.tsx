import React, { memo } from "react";

type Book = {
  id: string;
  title: string;
  text: string;
  created_at: number;
  created_by: string;
  trace_ids: string[];
  read_count: number;
};

type LibraryPanelProps = {
  books: Record<string, Book>;
  selectedBookId?: string;
  onSelectBook?: (bookId: string) => void;
};

export const LibraryPanel = memo(function LibraryPanel({
  books,
  selectedBookId,
  onSelectBook
}: LibraryPanelProps) {
  const bookList = Object.values(books).sort((a, b) => b.created_at - a.created_at);
  const selectedBook = selectedBookId ? books[selectedBookId] : null;

  return (
    <div style={{
      display: "flex",
      flexDirection: "column",
      gap: 8,
      padding: 8,
      backgroundColor: "#1e1e1e",
      borderRadius: 4,
      border: "1px solid #424242"
    }}>
      <div style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        borderBottom: "1px solid #424242",
        paddingBottom: 8
      }}>
        <strong style={{ color: "#e0e0e0", fontSize: 14 }}>
          Library Books ({bookList.length})
        </strong>
      </div>

      {bookList.length === 0 && (
        <div style={{ color: "#757575", fontStyle: "italic", fontSize: 12, padding: 4 }}>
          No books yet. Build a library to let scribes write the colony's lore.
        </div>
      )}

      {bookList.length > 0 && (
        <div style={{
          maxHeight: 200,
          overflowY: "auto",
          display: "flex",
          flexDirection: "column",
          gap: 4
        }}>
          {bookList.map((book) => (
            <div
              key={book.id}
              onClick={() => onSelectBook?.(book.id)}
              style={{
                padding: 6,
                backgroundColor: selectedBookId === book.id ? "#37474f" : "#263238",
                borderRadius: 3,
                cursor: "pointer",
                border: "1px solid #455a64",
                fontSize: 12
              }}
            >
              <div style={{ color: "#eceff1", fontWeight: "bold", marginBottom: 2 }}>
                {book.title}
              </div>
              <div style={{ color: "#b0bec5", fontSize: 10, display: "flex", gap: 4 }}>
                <span>Tick {book.created_at}</span>
                <span>•</span>
                <span>{book.trace_ids.length} source(s)</span>
                <span>•</span>
                <span>Read {book.read_count}x</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {selectedBook && (
        <div style={{
          marginTop: 8,
          padding: 8,
          backgroundColor: "#263238",
          borderRadius: 3,
          border: "1px solid #455a64"
        }}>
          <div style={{ color: "#eceff1", fontWeight: "bold", fontSize: 12, marginBottom: 4 }}>
            {selectedBook.title}
          </div>
          <div style={{
            color: "#cfd8dc",
            fontSize: 11,
            lineHeight: 1.4,
            maxHeight: 120,
            overflowY: "auto",
            whiteSpace: "pre-wrap"
          }}>
            {selectedBook.text}
          </div>
          <div style={{ color: "#78909c", fontSize: 10, marginTop: 4, display: "flex", gap: 8 }}>
            <span>Created at tick {selectedBook.created_at}</span>
            <span>by agent {selectedBook.created_by.slice(0, 8)}</span>
          </div>
        </div>
      )}
    </div>
  );
});

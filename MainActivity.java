package com.example.todoapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> todoItems;
    private ArrayAdapter<String> adapter;
    private EditText editText;
    private Switch urgentSwitch;
    private TodoDbHelper dbHelper;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new TodoDbHelper(this);
        db = dbHelper.getWritableDatabase();

        todoItems = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, todoItems);

        ListView listView = findViewById(R.id.listView);
        listView.setAdapter(adapter);

        editText = findViewById(R.id.editText);
        urgentSwitch = findViewById(R.id.urgentSwitch);
        Button addButton = findViewById(R.id.addButton);

        addButton.setOnClickListener(v -> addTodoItem());

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteDialog(position);
            return true;
        });

        loadTodoItems();
    }

    private void loadTodoItems() {
        todoItems.clear();
        Cursor cursor = db.query(TodoDbHelper.TABLE_NAME, null, null, null, null, null, null);
        printCursor(cursor);
        while (cursor.moveToNext()) {
            String text = cursor.getString(cursor.getColumnIndex(TodoDbHelper.COLUMN_TEXT));
            boolean urgent = cursor.getInt(cursor.getColumnIndex(TodoDbHelper.COLUMN_URGENT)) == 1;
            todoItems.add(urgent ? "! " + text : text);
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void addTodoItem() {
        String text = editText.getText().toString().trim();
        if (!text.isEmpty()) {
            boolean isUrgent = urgentSwitch.isChecked();
            todoItems.add(isUrgent ? "! " + text : text);
            adapter.notifyDataSetChanged();

            ContentValues values = new ContentValues();
            values.put(TodoDbHelper.COLUMN_TEXT, text);
            values.put(TodoDbHelper.COLUMN_URGENT, isUrgent ? 1 : 0);
            db.insert(TodoDbHelper.TABLE_NAME, null, values);

            editText.setText("");
            urgentSwitch.setChecked(false);
        }
    }

    private void showDeleteDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(getString(R.string.delete_dialog_message, position))
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    String item = todoItems.get(position);
                    todoItems.remove(position);
                    adapter.notifyDataSetChanged();

                    db.delete(TodoDbHelper.TABLE_NAME,
                            TodoDbHelper.COLUMN_TEXT + " = ?",
                            new String[]{item.replace("! ", "")});
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void printCursor(Cursor c) {
        Log.d("TodoApp", "Database version: " + db.getVersion());
        Log.d("TodoApp", "Number of columns: " + c.getColumnCount());
        Log.d("TodoApp", "Column names: " + String.join(", ", c.getColumnNames()));
        Log.d("TodoApp", "Number of results: " + c.getCount());

        c.moveToPosition(-1);
        while (c.moveToNext()) {
            StringBuilder row = new StringBuilder("Row: ");
            for (int i = 0; i < c.getColumnCount(); i++) {
                row.append(c.getString(i)).append(", ");
            }
            Log.d("TodoApp", row.toString());
        }
        c.moveToPosition(-1);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
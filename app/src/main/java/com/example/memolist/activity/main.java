package com.example.memolist.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.memolist.CustomListView;
import com.example.memolist.R;
import com.example.memolist.adapters.ExpandableListAdapter;
import com.example.memolist.adapters.custom_adapter;
import com.example.memolist.data_model;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import static com.example.memolist.data_sort.sortByAlphabet;
import static com.example.memolist.data_sort.sortByDateEdit;
import static com.example.memolist.data_sort.sortByImportance;

public class main extends AppCompatActivity {
    protected SharedPreferences sharedPreferences;
    protected SharedPreferences.Editor editor;
    protected ArrayList<data_model> dataModels;

    protected View mView;
    protected CustomListView listView;
    protected Drawable origBackground;

    protected custom_adapter adapter;
    protected int glob_font_size;
    protected int glob_color_scheme;
    private EditText memoInput;
    protected SeekBar importance_seeker;
    protected TextView importance_seeker_position;
    private Spinner spinner;
    private Spinner input_category_spinner;

    protected AlertDialog dialog;
    protected AlertDialog confirm_dialog;
    protected AlertDialog.Builder mBuilder;
    protected Button confirm_button;

    protected ExpandableListView exListView;
    protected ExpandableListAdapter exListAdapter;
    protected ArrayList<String> categoryHeader;
    protected ArrayList<ArrayList<data_model>> categoryDataModels;
    protected HashMap<String, ArrayList<data_model>> categoryHash;

    protected boolean last_edit_state;

    protected String string;

    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        glob_color_scheme = sharedPreferences.getInt("color scheme", 0);

        if (glob_color_scheme == 0) {
            setTheme(R.style.AppThemeLight);
        } else if (glob_color_scheme == 1) {
            setTheme(R.style.AppThemeDark);
        } else if (glob_color_scheme == 2) {
            setTheme(R.style.AppThemeFluffPink);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (glob_color_scheme == 1) {
            TextView input_category_indicator = findViewById(R.id.input_category_indicator);
            input_category_indicator.setTextColor(Color.LTGRAY);
        }

        buildExListView();
        buildListView();
        buildMemoInput();
        buildSpinner();
        buildImportanceSeeker();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            case R.id.action_info:
                mView = View.inflate(this, R.layout.dialog_info, null);
                createDialog();
                Button ask_donate_button = mView.findViewById(R.id.ask_donate_button);
                createConfirmButton();
                ask_donate_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/softblankie"));
                        startActivity(browserIntent);
                    }
                });
                return true;

            case R.id.action_help:
                mView = getLayoutInflater().inflate(R.layout.dialog_help, null);
                createDialog();
                createConfirmButton();
                return true;

            case R.id.action_settings:
                mView = View.inflate(this, R.layout.dialog_settings, null);
                createDialog();
                CheckBox showLastEdit = mView.findViewById(R.id.show_last_edit_check);
                final TextView fontPosition = mView.findViewById(R.id.font_position);
                SeekBar font_seeker = mView.findViewById(R.id.font_seeker);
                final Spinner color_spinner = mView.findViewById(R.id.spinner);
                Button clear_button = mView.findViewById(R.id.clear_button);
                confirm_button = mView.findViewById(R.id.confirm_button);

                string = "Change Font [" + glob_font_size + "]";
                fontPosition.setText(string);
                font_seeker.setProgress(glob_font_size - 14);

                // Font settings
                font_seeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        glob_font_size =  progress + 14;
                        string = "Change Font [" + glob_font_size + "]";
                        fontPosition.setText(string);
                        saveAndAdapt();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        fontPosition.setText(string);
                        listView.setAdapter(adapter);
                        saveData();
                    }
                });

                // Color settings
                ArrayAdapter<CharSequence> spinner_adapter = ArrayAdapter.createFromResource(this,
                        R.array.color_scheme_list, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                color_spinner.setAdapter(spinner_adapter);
                color_spinner.setSelection(glob_color_scheme);

                // Time settings
                showLastEdit.setChecked(last_edit_state);
                showLastEdit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        last_edit_state = !last_edit_state;
                        // save showLastEdit state
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("last_edit_check", last_edit_state);
                        editor.apply();
                    }
                });

                clear_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mView = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
                        mBuilder = new AlertDialog.Builder(main.this);
                        mBuilder.setView(mView);
                        confirm_dialog = mBuilder.create();
                        confirm_dialog.show();
                        confirm_dialog.setCanceledOnTouchOutside(false);
                        Button yes_button = mView.findViewById(R.id.yes_button);
                        Button no_button = mView.findViewById(R.id.no_button);
                        yes_button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dataModels.clear();
                                saveAndAdapt();
                                confirm_dialog.dismiss();
                            }
                        });
                        no_button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                confirm_dialog.dismiss();
                            }
                        });
                    }
                });

                confirm_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if ((color_spinner.getSelectedItemPosition() == 0) &&
                                (glob_color_scheme != color_spinner.getSelectedItemPosition())) {
                            glob_color_scheme = 0;
                            finish();
                            startActivity(getIntent());
                        } else if ((color_spinner.getSelectedItemPosition() == 1) &&
                                (glob_color_scheme != color_spinner.getSelectedItemPosition())) {
                            glob_color_scheme = 1;
                            finish();
                            startActivity(getIntent());
                        } else if ((color_spinner.getSelectedItemPosition() == 2) &&
                                (glob_color_scheme != color_spinner.getSelectedItemPosition())) {
                            glob_color_scheme = 2;
                            finish();
                            startActivity(getIntent());
                        }
                        saveAndAdapt();
                        dialog.dismiss();
                    }
                });
                return true;

            case R.id.sort_importance:
                sortByImportance(dataModels, categoryDataModels);
                saveAndAdapt();
                return true;

            case R.id.sort_alphabetical:
                sortByAlphabet(dataModels, categoryDataModels);
                saveAndAdapt();
                return true;

            case R.id.sort_date_edited:
                sortByDateEdit(dataModels, categoryDataModels);
                saveAndAdapt();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void buildExListView() {
        exListView = findViewById(R.id.category_view);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        Resources r = getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                75, r.getDisplayMetrics());
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            exListView.setIndicatorBounds(width - px, width);
        } else {
            exListView.setIndicatorBoundsRelative(width - px, width);
        }

        exListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, final View view, final int groupPosition, final int childPosition, long id) {
                listView.setItemChecked(childPosition, true);
                origBackground = view.getBackground();
                view.setBackgroundColor(Color.LTGRAY);

                // edit text
                mView = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
                createDialogMain(main.this);
                final EditText memo_text = mView.findViewById(R.id.memo_text);
                final TextView category_indicator = mView.findViewById(R.id.category_indicator);
                final SeekBar category_seeker = mView.findViewById(R.id.category_seeker);
                final TextView importance_seeker_position = mView.findViewById(R.id.importance_seeker_position);
                final SeekBar importance_seeker = mView.findViewById(R.id.importance_seeker);
                confirm_button = mView.findViewById(R.id.confirm_button);
                memo_text.setText(categoryDataModels.get(groupPosition).get(childPosition).getMessage());
                final String temp = memo_text.getText().toString();
                confirm_button = mView.findViewById(R.id.confirm_button);
                confirm_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // change text in dataModels
                        categoryDataModels.get(groupPosition).get(childPosition).setMessage(memo_text.getText().toString());
                        // change edit date
                        if (!temp.equals(memo_text.getText().toString())) {
                            categoryDataModels.get(groupPosition).get(childPosition).setDateEdited(getDate());
                        }
                        // change importance rating
                        if (importance_seeker_position.getText().toString().matches("0")) {
                            categoryDataModels.get(groupPosition).get(childPosition).setImportance(null);
                        } else {
                            categoryDataModels.get(groupPosition).get(childPosition).setImportance(String.valueOf(importance_seeker.getProgress()));
                        }
                        // change category
                        if (category_seeker.getProgress() == 0) {
                            dataModels.add(categoryDataModels.get(groupPosition).get(childPosition));
                            categoryDataModels.get(groupPosition).remove(childPosition);
                            adapterOverride();
                            listView.setAdapter(adapter);
                        } else {
                            categoryDataModels.get(category_seeker.getProgress()-1).add(categoryDataModels.get(groupPosition).get(childPosition));
                            categoryDataModels.get(groupPosition).remove(childPosition);
                        }
                        view.setBackgroundDrawable(origBackground);
                        adapter.notifyDataSetChanged();
                        exListAdapter.notifyDataSetChanged();
                        saveData();
                        dialog.dismiss();
                    }
                });
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        view.setBackgroundDrawable(origBackground);
                        adapter.notifyDataSetChanged();
                    }
                });
                // Category Seeker
                category_indicator.setText(categoryHeader.get(groupPosition));
                category_seeker.setProgress(groupPosition+1);
                category_seeker.setMax(categoryHeader.size());
                category_seeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (progress == 0) {
                            category_indicator.setText("No Category Chosen");
                        } else {
                            category_indicator.setText(categoryHeader.get(progress-1));
                        }

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
                // Importance seeker
                if (categoryDataModels.get(groupPosition).get(childPosition).getImportance() == null) {
                    importance_seeker_position.setText("0");
                    importance_seeker.setProgress(0);
                } else {
                    importance_seeker_position.setText(categoryDataModels.get(groupPosition).get(childPosition).getImportance());
                    importance_seeker.setProgress(Integer.parseInt(categoryDataModels.get(groupPosition).get(childPosition).getImportance()));
                }
                importance_seeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        importance_seeker_position.setText(String.valueOf(importance_seeker.getProgress()));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
                return false;
            }
        });

        exListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                long packedPosition = exListView.getExpandableListPosition(position);

                int itemType = ExpandableListView.getPackedPositionType(packedPosition);
                final int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
                int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);

                //  if group item clicked
                if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    // confirmation to remove
                    mView = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
                    mBuilder = new AlertDialog.Builder(main.this);
                    mBuilder.setView(mView);
                    final AlertDialog remove_confirm = mBuilder.create();
                    remove_confirm.show();
                    remove_confirm.setCanceledOnTouchOutside(false);
                    Button yes_button = mView.findViewById(R.id.yes_button);
                    Button no_button = mView.findViewById(R.id.no_button);
                    yes_button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // remove the item from the array
                            categoryHeader.remove(position);
                            categoryDataModels.remove(position);
                            categoryHash.remove(groupPosition);
                            exListAdapter.notifyDataSetChanged();
                            saveData();
                            remove_confirm.dismiss();
                        }
                    });
                    no_button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            remove_confirm.dismiss();
                        }
                    });
                }

                /*  if child item clicked */
                else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    // remove the item from the array
                    categoryDataModels.get(groupPosition).remove(childPosition);
                    exListAdapter.notifyDataSetChanged();
                    saveData();
                    return false;
                }

                return false;
            }
        });
    }

    private void buildListView() {
        listView = findViewById(R.id.memo_view);
        listView.setAdapter(adapter);
        listView.setListener(new CustomListView.Listener() {
            @Override
            public void swapElements(int indexOne, int indexTwo) {
                data_model temp = dataModels.get(indexOne);
                dataModels.set(indexOne, dataModels.get(indexTwo));
                dataModels.set(indexTwo, temp);
                saveData();
            }
        });
        loadData();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                listView.setItemChecked(position, true);
                origBackground = view.getBackground();
                view.setBackgroundColor(Color.LTGRAY);

                // edit text
                mView = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
                createDialogMain(main.this);
                final EditText memo_text = mView.findViewById(R.id.memo_text);
                final TextView category_indicator = mView.findViewById(R.id.category_indicator);
                final SeekBar category_seeker = mView.findViewById(R.id.category_seeker);
                final TextView importance_seeker_position = mView.findViewById(R.id.importance_seeker_position);
                final SeekBar importance_seeker = mView.findViewById(R.id.importance_seeker);
                confirm_button = mView.findViewById(R.id.confirm_button);
                memo_text.setText(dataModels.get(position).getMessage());

                final String temp = memo_text.getText().toString();
                confirm_button = mView.findViewById(R.id.confirm_button);
                confirm_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // change text in dataModels
                        dataModels.get(position).setMessage(memo_text.getText().toString());
                        // change edit date
                        if (!temp.equals(memo_text.getText().toString())) {
                            dataModels.get(position).setDateEdited(getDate());
                        }
                        // change importance rating
                        if (importance_seeker_position.getText().toString().matches("0")) {
                            dataModels.get(position).setImportance(null);
                        } else {
                            dataModels.get(position).setImportance(String.valueOf(importance_seeker.getProgress()));
                        }
                        // change category
                        if (category_seeker.getProgress() != 0) {
                            categoryDataModels.get(category_seeker.getProgress()-1).add(dataModels.get(position));
                            dataModels.remove(position);
                            exListAdapter.notifyDataSetChanged();
                            adapterOverride();
                            listView.setAdapter(adapter);
                        }
                        view.setBackgroundDrawable(origBackground);
                        adapter.notifyDataSetChanged();
                        saveData();
                        dialog.dismiss();
                    }
                });
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        view.setBackgroundDrawable(origBackground);
                        adapter.notifyDataSetChanged();
                    }
                });

                // Category Seeker
                category_indicator.setText("No Category Chosen");
                category_seeker.setMax(categoryHeader.size());
                category_seeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (progress == 0) {
                            category_indicator.setText("No Category Chosen");
                        } else {
                            category_indicator.setText(categoryHeader.get(progress-1));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                // Importance seeker
                if (dataModels.get(position).getImportance() == null) {
                    importance_seeker_position.setText("0");
                    importance_seeker.setProgress(0);
                } else {
                    importance_seeker_position.setText(dataModels.get(position).getImportance());
                    importance_seeker.setProgress(Integer.parseInt(dataModels.get(position).getImportance()));
                }
                importance_seeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        importance_seeker_position.setText(String.valueOf(importance_seeker.getProgress()));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // remove the item from the array
                dataModels.remove(position);
                adapter.notifyDataSetChanged();
                saveData();
                return false;
            }
        });
    }

    private void buildMemoInput() {
        memoInput = findViewById(R.id.memo_input);
        Button add_button = findViewById(R.id.add_button);
        add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (memoInput.getText().toString().matches("")) {
                    Toast.makeText(main.this, "Please enter a memo", Toast.LENGTH_SHORT).show();
                } else {
                    if (spinner.getSelectedItemPosition() == 0) { // if selected "topic"
                        String newImportance = null;
                        if ((input_category_spinner != null) && (input_category_spinner.getSelectedItemPosition() != 0)) {
                            if (importance_seeker.getProgress() != 0) {
                                newImportance = String.valueOf(importance_seeker.getProgress());
                            }
                            String newItem = memoInput.getText().toString();
                            categoryDataModels.get(input_category_spinner.getSelectedItemPosition()-1).add(new data_model(newImportance,
                                    newItem, getDate()));
                        } else {
                            if (importance_seeker.getProgress() != 0) {
                                newImportance = String.valueOf(importance_seeker.getProgress());
                            }
                            String newItem = memoInput.getText().toString();
                            dataModels.add(new data_model(newImportance, newItem, getDate()));
                        }
                    } else if (spinner.getSelectedItemPosition() == 1) { // if selected "category"
                        categoryHeader.add(memoInput.getText().toString());
                        categoryDataModels.add(new ArrayList<data_model>());
                        int category_position = categoryHeader.size() - 1;
                        categoryHash.put(categoryHeader.get(category_position), categoryDataModels.get(category_position));
                    }

                    // scrolls to bottom of listView
                    listView.post(new Runnable(){
                        public void run() {
                            listView.setSelection(listView.getCount() - 1);
                        }});

                    adapterOverride();
                    exAdapterOverride();
                    exListView.setAdapter(exListAdapter);
                    listView.setAdapter(adapter);
                    restoreDefault();
                    saveData();
                }
            }
        });
        add_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mView = getLayoutInflater().inflate(R.layout.dialog_input_category, null);
                createDialogMain(main.this);
                input_category_spinner = mView.findViewById(R.id.input_category_spinner);
                final TextView input_category_indicator = findViewById(R.id.input_category_indicator);
                confirm_button = mView.findViewById(R.id.confirm_button);
                categoryHeader.add(0, "Default");
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(main.this, R.layout.input_spinner, categoryHeader);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                input_category_spinner.setAdapter(adapter);
                input_category_spinner.setSelection(0);

                confirm_button = mView.findViewById(R.id.confirm_button);
                confirm_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        input_category_indicator.setText(input_category_spinner.getSelectedItem().toString());
                        categoryHeader.remove(0);
                        dialog.dismiss();
                    }
                });
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        input_category_indicator.setText(input_category_spinner.getSelectedItem().toString());
                        categoryHeader.remove(0);
                    }
                });

                return false;
            }
        });
    }

    private String getDate() {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");

        return df.format(c);
    }

    private void buildSpinner() {
        spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.input_list, R.layout.input_spinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    protected void buildImportanceSeeker() {
        importance_seeker_position = findViewById(R.id.importance_seeker_position);
        importance_seeker = findViewById(R.id.importance_seeker);
        importance_seeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                importance_seeker_position.setText(String.valueOf(importance_seeker.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void restoreDefault() {
        memoInput.setText("");
        importance_seeker_position.setText(String.valueOf(importance_seeker.getProgress()));
        importance_seeker.setProgress(0);
    }

    protected void saveData() {
        sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);

        editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String dataModels_json = gson.toJson(dataModels);
        String categoryHeader_json = gson.toJson(categoryHeader);
        String categoryDataModels_json = gson.toJson(categoryDataModels);
        String categoryHash_json = gson.toJson(categoryHash);
        editor.putInt("color scheme", glob_color_scheme);
        editor.putInt("font size", glob_font_size);
        editor.putString("memo list", dataModels_json);
        editor.putString("category titles", categoryHeader_json);
        editor.putString("category list", categoryDataModels_json);
        editor.putString("category hash", categoryHash_json);
        editor.apply();
    }

    private void loadData() {
        sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        Gson gson = new Gson();
        glob_font_size = sharedPreferences.getInt("font size", 14);
        last_edit_state = sharedPreferences.getBoolean("last_edit_check", false);
        String dataModels_json = sharedPreferences.getString("memo list", null);
        String categoryHeader_json = sharedPreferences.getString("category titles", null);
        String categoryDataModels_json = sharedPreferences.getString("category list", null);
        String categoryHash_json = sharedPreferences.getString("category hash", null);
        Type dataModels_type = new TypeToken<ArrayList<data_model>>() {}.getType();
        Type categoryHeader_type = new TypeToken<ArrayList<String>>() {}.getType();
        Type categoryDataModels_type = new TypeToken<ArrayList<ArrayList<data_model>>>() {}.getType();
        Type categoryHash_type = new TypeToken<HashMap<String, ArrayList<data_model>>>() {}.getType();
        dataModels = gson.fromJson(dataModels_json, dataModels_type);
        categoryHeader = gson.fromJson(categoryHeader_json, categoryHeader_type);
        categoryDataModels = gson.fromJson(categoryDataModels_json, categoryDataModels_type);
        categoryHash = gson.fromJson(categoryHash_json, categoryHash_type);

        if (dataModels == null) {
            dataModels= new ArrayList<>();
        }
        if (categoryDataModels == null) {
            categoryDataModels = new ArrayList<>();
        }
        if (categoryHeader == null) {
            categoryHeader = new ArrayList<>();
        }
        if (categoryHash == null) {
            categoryHash = new HashMap<>();
        } else {
            for (int i = 0; i < categoryHeader.size(); i++) {
                categoryHash.put(categoryHeader.get(i), categoryDataModels.get(i));
            }
        }
        exAdapterOverride();
        exListView.setAdapter(exListAdapter);
        adapterOverride();
        listView.setAdapter(adapter);
    }

    protected void adapterOverride() {
        adapter = new custom_adapter(dataModels, getApplicationContext(), new custom_adapter.Listener() {
            @Override
            public void onGrab(int position, RelativeLayout row) {
                listView.onGrab(position, row);
            }
        }) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                final data_model dataModel = getItem(position);
                ViewHolder viewHolder;
                final View result;

                if (convertView == null) {
                    viewHolder = new custom_adapter.ViewHolder();
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(R.layout.memo_items, parent, false);
                    viewHolder.txtImportance = convertView.findViewById(R.id.importance);
                    viewHolder.txtMessage = convertView.findViewById(R.id.memo_item);
                    viewHolder.txtDateEdited = convertView.findViewById(R.id.last_edit);

                    result=convertView;

                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (custom_adapter.ViewHolder) convertView.getTag();
                    result=convertView;
                }

                String color = null;
                if ((glob_color_scheme == 0) || (glob_color_scheme == 2)) {
                    TypedArray ta = obtainStyledAttributes(R.style.ActivityTheme_Primary_Base_Light, R.styleable.MyCustomView);
                    color = ta.getString(R.styleable.MyCustomView_primaryTextColor);
                } else if (glob_color_scheme == 1) {
                    TypedArray ta = obtainStyledAttributes(R.style.ActivityTheme_Primary_Base_Dark, R.styleable.MyCustomView);
                    color = ta.getString(R.styleable.MyCustomView_primaryTextColor);
                    viewHolder.txtDateEdited.setTextColor(Color.LTGRAY);
                }

                viewHolder.txtImportance.setTextColor(Color.parseColor(color));
                viewHolder.txtMessage.setTextColor(Color.parseColor(color));

                LinearLayout.LayoutParams textViewLayoutParams =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                if (last_edit_state) {
                    viewHolder.txtDateEdited.setText(dataModel.getDateEdited());
                    viewHolder.txtMessage.setLayoutParams(textViewLayoutParams);
                    textViewLayoutParams.setMargins(0,0,0,30);
                    viewHolder.txtMessage.requestLayout();
                } else {
                    viewHolder.txtDateEdited.setText(null);
                }

                viewHolder.txtMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, glob_font_size);
                viewHolder.txtImportance.setText(dataModel.getImportance());
                viewHolder.txtMessage.setText(dataModel.getMessage());

                final RelativeLayout row = convertView.findViewById(R.id.memo_items);

                convertView.findViewById(R.id.imageViewGrab).setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        listener.onGrab(position, row);
                        return false;
                    }
                });

                return convertView;
            }
        };
    }

    protected void exAdapterOverride() {
        exListAdapter = new ExpandableListAdapter(main.this, categoryHeader, categoryHash) {
            @Override
            public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
                                     View convertView, ViewGroup parent) {
                final String importanceText = listHashMap.get(listDataHeader.get(groupPosition)).get(childPosition).getImportance();
                final String memoText = listHashMap.get(listDataHeader.get(groupPosition)).get(childPosition).getMessage();
                final String txtDateEdited = listHashMap.get(listDataHeader.get(groupPosition)).get(childPosition).getDateEdited();
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(R.layout.memo_items,null);
                }
                convertView.findViewById(R.id.imageViewGrab).setVisibility(View.INVISIBLE);
                TextView importance = convertView.findViewById(R.id.importance);
                TextView memo_item = convertView.findViewById(R.id.memo_item);
                TextView date_edited = convertView.findViewById(R.id.last_edit);
                importance.setText(importanceText);
                memo_item.setText(memoText);

                LinearLayout.LayoutParams textViewLayoutParams =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                if (last_edit_state) {
                    date_edited.setText(txtDateEdited);
                    memo_item.setLayoutParams(textViewLayoutParams);
                    textViewLayoutParams.setMargins(0,0,0,30);
                    memo_item.requestLayout();
                } else {
                    date_edited.setText(null);
                }

                return convertView;
            }

        };
    }

    protected void createDialogMain(Context targetContext) {
        mBuilder = new AlertDialog.Builder(targetContext);
        mBuilder.setView(mView);
        dialog = mBuilder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void createDialog() {
        mBuilder = new AlertDialog.Builder(this);
        mBuilder.setView(mView);
        dialog = mBuilder.create();
        dialog.show();
    }

    protected void createConfirmButton() {
        confirm_button = mView.findViewById(R.id.confirm_button);
        confirm_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    public void saveAndAdapt() {
        saveData();
        adapterOverride();
        exAdapterOverride();
        listView.setAdapter(adapter);
        exListView.setAdapter(exListAdapter);
    }
}
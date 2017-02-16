package com.ghalexandru.contacts;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ghalexandru.contacts.database.Contact;
import com.ghalexandru.contacts.database.ContactDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ContactsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private final String WAIT_MSG = "Please wait...";
    private final String SYNC_MSG = " contacts can be sync.\nDo you want to sync them?";
    private final String SYNCING_MSG = "Syncing...";
    private final String WAIT_SYNCING_MSG = "Contacts will be syncing";
    private final String LOADING_MSG = "Loading contacts...";
    private final String SEARCH_HINT_MSG = "Search";
    private static final String ASK_DELETE_MSG = "Are you sure you want to delete all your contacts?";

    //  Used to checking if it's first time when this app started;
    private static boolean onlyAtStart = false;

    private ProgressDialog loadingProgressDialog;
    private Handler handler;
    private ListView listViewContacts;
    private ContactDB contactDB;
    private AdapterList listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadingProgressDialog = ProgressDialog.show(this, WAIT_MSG, LOADING_MSG);
        listViewContacts = (ListView) findViewById(R.id.listViewContacts);

        final SetupDatabaseTask setupDatabaseTask = new SetupDatabaseTask();
        new Thread(setupDatabaseTask).start();
        handler = new Handler(new HandlerCallback(setupDatabaseTask));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final AdapterList adapter = (AdapterList) listViewContacts.getAdapter();
        Contact contact = (Contact) adapter.getItem(position);

        Intent intent = new Intent(ContactsActivity.this, EditContactActivity.class);
        intent.putExtra("contact_result", contact);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        getMenuInflater().inflate(R.menu.contacts_menu, menu);

        setupSearchBarItem(menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setupSearchBarItem(Menu menu) {
        final MenuItem searchItem = menu.findItem(R.id.searchItem);

        if (searchItem != null) {
            SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

            EditText searchPlate = (EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
            searchPlate.setHint(SEARCH_HINT_MSG);

            View searchPlateView = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
            searchPlateView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    listAdapter.filter(newText);
                    return false;
                }
            });

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.addContactItem:
                startActivity(new Intent(ContactsActivity.this, EditContactActivity.class));
                break;

            case R.id.removeAllContactsItem:
                AlertDialog.Builder deleteDialog = new AlertDialog.Builder(ContactsActivity.this);
                deleteDialog.setMessage(ASK_DELETE_MSG);
                deleteDialog.setCancelable(false);
                deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                contactDB.removeAllContacts();
                                handler.sendEmptyMessage(4);
                            }
                        }).start();
                    }
                });

                deleteDialog.setNegativeButton("No", null);
                deleteDialog.show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    //  Handler callback to do some stuff on ui thread
    private class HandlerCallback implements Handler.Callback {

        private SetupDatabaseTask setupDatabaseTask;

        public HandlerCallback(SetupDatabaseTask setupDatabaseTask) {
            this.setupDatabaseTask = setupDatabaseTask;
        }

        @Override
        public boolean handleMessage(Message message) {

            switch (message.what) {
                case 0:
                    loadingProgressDialog.dismiss();
                    break;

                case 1:
                    setupDatabaseTask.setupAlertDialog();
                    break;

                case 2:
                    setupDatabaseTask.setupListAdapter();
                    break;

                case 3:
                    setupDatabaseTask.setupListAdapter();
                    setupDatabaseTask.dismissProgressSyncDialog();
                    break;

                case 4:
                    listAdapter.removeAll();
                    setTitle("0 contacts");
                    break;
            }
            return true;
        }
    }

    //  Get databases and setup list view
    private class SetupDatabaseTask implements Runnable {
        //  Store contacts from sqlite to a list of contacts
        private List<Contact> databaseContacts;

        //  Store contacts that are in the phone agenda and aren't in sqlite database
        private List<Contact> syncContacts;

        private AlertDialog.Builder alertDialog;
        private ProgressDialog progressSyncDialog;

        @Override
        public void run() {
            setupDatabase();
            //  Dismiss progress dialog
            handler.sendEmptyMessage(0);

            if (syncContacts.size() > 0)
                handler.sendEmptyMessage(1);    //  Setup alert dialog
            else
                handler.sendEmptyMessage(2);    //  Setup list adapter

            onlyAtStart = true;
        }

        //  Fill contacts from phone agenda to sqlite database on "yes" and on
        //  "no" it will call setupListAdapter
        private void setupAlertDialog() {
            alertDialog = new AlertDialog.Builder(ContactsActivity.this);
            alertDialog.setMessage(syncContacts.size() + SYNC_MSG);
            alertDialog.setCancelable(false);
            alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    progressSyncDialog = ProgressDialog.show(ContactsActivity.this, SYNCING_MSG, WAIT_SYNCING_MSG, true);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            contactDB.addContact(syncContacts);
                            databaseContacts = contactDB.getAllContacts();
                            handler.sendEmptyMessage(3);    //  Setup list adapter and dismiss progress sync dialog
                        }
                    }).start();

                }
            });

            alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.sendEmptyMessage(2);    //  Setup list adapter
                }
            });

            alertDialog.show();
        }

        private void dismissProgressSyncDialog() {
            progressSyncDialog.dismiss();
        }

        //  Get database from sqlite and if it's first time this app started
        //  it will get also database from phone agenda
        private void setupDatabase() {
            contactDB = new ContactDB(ContactsActivity.this);
            databaseContacts = contactDB.getAllContacts();
            syncContacts = new ArrayList<>();

            //  Check to see if it's first time this activity run and also
            // check if build version if than 23 because i didn't implement
            //  runtime read contact permission
            if (!onlyAtStart && Build.VERSION.SDK_INT <= 22)
                getPhoneContacts();
        }


        //  Get database from phone agenda
        private void getPhoneContacts() {

            List<Contact> phoneContacts = contactDB.getPhoneContacts();

            if (phoneContacts != null && databaseContacts != null) {
                for (Contact contact : phoneContacts)
                    if (!databaseContacts.contains(contact))
                        syncContacts.add(contact);
            }
        }

        //  Create list adapter
        private void setupListAdapter() {
            //  Sort contacts alphabetically
            Collections.sort(databaseContacts, new Comparator<Contact>() {
                @Override
                public int compare(Contact o1, Contact o2) {
                    return o1.getFullName().compareTo(o2.getFullName());
                }
            });
            setTitle(databaseContacts.size() + " contacts");
            listAdapter = new AdapterList(ContactsActivity.this, databaseContacts);
            listViewContacts.setAdapter(listAdapter);
            listViewContacts.setOnItemClickListener(ContactsActivity.this);
        }
    }

    //  Adapter list for listViewContacts
    private class AdapterList extends BaseAdapter {
        private Context context;

        //  List of contacts from database
        private List<Contact> contactList;

        //  List that will be shown on search
        private List<Contact> listToShow;

        public AdapterList(Context context, List<Contact> contactList) {
            this.context = context;
            this.contactList = contactList;
            listToShow = new ArrayList<>();
            listToShow.addAll(contactList);
        }

        @Override
        public int getCount() {
            return contactList == null ? 0 : listToShow.size();
        }

        @Override
        public Object getItem(int position) {
            return listToShow.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {

            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.contact_list_item, parent, false);

                ViewHolder holder = new ViewHolder();
                holder.contactName = (TextView) view.findViewById(R.id.contactName);
                holder.firstName = (TextView) view.findViewById(R.id.firstName);
                holder.lastName = (TextView) view.findViewById(R.id.lastName);
                holder.email = (TextView) view.findViewById(R.id.email);
                holder.phone = (TextView) view.findViewById(R.id.phone);
                holder.country = (TextView) view.findViewById(R.id.country);
                holder.city = (TextView) view.findViewById(R.id.city);
                holder.street = (TextView) view.findViewById(R.id.street);

                holder.firstNameLayout = (LinearLayout) view.findViewById(R.id.firstNameLayout);
                holder.lastNameLayout = (LinearLayout) view.findViewById(R.id.lastNameLayout);
                holder.emailLayout = (LinearLayout) view.findViewById(R.id.emailLayout);
                holder.phoneLayout = (LinearLayout) view.findViewById(R.id.phoneLayout);
                holder.countryLayout = (LinearLayout) view.findViewById(R.id.countryLayout);
                holder.cityLayout = (LinearLayout) view.findViewById(R.id.cityLayout);
                holder.streetLayout = (LinearLayout) view.findViewById(R.id.streetLayout);

                view.setTag(holder);
            }

            ViewHolder holder = (ViewHolder) view.getTag();

            holder.contactName.setText(listToShow.get(position).getFullName());

            if (listToShow.get(position).getFirstName() != null && !listToShow.get(position).getFirstName().isEmpty()) {
                holder.firstName.setText(listToShow.get(position).getFirstName());
                holder.firstNameLayout.setVisibility(View.VISIBLE);
            } else
                holder.firstNameLayout.setVisibility(View.GONE);

            if (listToShow.get(position).getLastName() != null && !listToShow.get(position).getLastName().isEmpty()) {
                holder.lastName.setText(listToShow.get(position).getLastName());
                holder.lastNameLayout.setVisibility(View.VISIBLE);
            } else
                holder.lastNameLayout.setVisibility(View.GONE);

            if (listToShow.get(position).getPhone() != null && !listToShow.get(position).getPhone().isEmpty()) {
                holder.phone.setText(listToShow.get(position).getPhone());
                holder.phoneLayout.setVisibility(View.VISIBLE);

            } else
                holder.phoneLayout.setVisibility(View.GONE);

            if (listToShow.get(position).getEmail() != null && !listToShow.get(position).getEmail().isEmpty()) {
                holder.email.setText(listToShow.get(position).getEmail());
                holder.emailLayout.setVisibility(View.VISIBLE);
            } else
                holder.emailLayout.setVisibility(View.GONE);

            if (listToShow.get(position).getCountry() != null && !listToShow.get(position).getCountry().isEmpty()) {
                holder.country.setText(listToShow.get(position).getCountry());
                holder.countryLayout.setVisibility(View.VISIBLE);
            } else
                holder.countryLayout.setVisibility(View.GONE);

            if (listToShow.get(position).getCity() != null && !listToShow.get(position).getCity().isEmpty()) {
                holder.city.setText(listToShow.get(position).getCity());
                holder.cityLayout.setVisibility(View.VISIBLE);
            } else
                holder.cityLayout.setVisibility(View.GONE);

            if (listToShow.get(position).getStreet() != null && !listToShow.get(position).getStreet().isEmpty()) {
                holder.street.setText(listToShow.get(position).getStreet());
                holder.countryLayout.setVisibility(View.VISIBLE);
            } else
                holder.streetLayout.setVisibility(View.GONE);

            return view;
        }

        //  Remove all contacts
        public void removeAll() {
            contactList.clear();
            listToShow.clear();
            notifyDataSetChanged();
        }

        //  Put into listToShow only contacts that contain characters typed in search acton bar
        public void filter(String searchText) {
            searchText = searchText.toLowerCase();
            listToShow.clear();

            if (searchText.length() == 0)
                listToShow.addAll(contactList);
            else {
                for (Contact contact : contactList) {
                    if (searchText.length() != 0 && contact.getFullName().toLowerCase().contains(searchText))
                        listToShow.add(contact);
                }
            }

            notifyDataSetChanged();
        }

        //  View holder
        private class ViewHolder {
            private TextView contactName, firstName, lastName, phone, email, country, city, street;
            private LinearLayout firstNameLayout, lastNameLayout, phoneLayout, emailLayout, countryLayout, cityLayout, streetLayout;

        }
    }
}






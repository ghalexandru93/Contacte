package com.ghalexandru.contacts;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.ghalexandru.contacts.database.Contact;
import com.ghalexandru.contacts.database.ContactDB;


public class EditContactActivity extends AppCompatActivity {

    private String NOT_ENOUGH_CHAR_MSG = " should have at least 3 characters";
    private String ASK_DELETE_MSG = "Are you sure you want to delete ";

    private EditText firstName, lastName, phone, email, country, city, street;
    private Menu menu;
    private Handler handler;

    private Contact contact;
    private ContactDB contactDB;

    //  If it's true then this activity will be only for add a new contact.
    //  If it's false it will be for edit.
    private boolean onAddContact = false;

    private AlertDialog.Builder deleteDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        contactDB = new ContactDB(getApplicationContext());
        //  Retrieve contact send by ContactsActivity
        contact = (Contact) getIntent().getSerializableExtra("contact_result");

        //  If contact it's null it means that this activity will be only for add a new contact
        if (contact == null)
            onAddContact = true;

        firstName = (EditText) findViewById(R.id.firstName);
        lastName = (EditText) findViewById(R.id.lastName);
        phone = (EditText) findViewById(R.id.phone);
        email = (EditText) findViewById(R.id.email);
        country = (EditText) findViewById(R.id.country);
        city = (EditText) findViewById(R.id.city);
        street = (EditText) findViewById(R.id.street);

        if (!onAddContact) {
            setTitle(contact.getFullName());

            firstName.setText(contact.getFirstName());
            lastName.setText(contact.getLastName());
            phone.setText(contact.getPhone());
            email.setText(contact.getEmail());
            country.setText(contact.getCountry());
            city.setText(contact.getCity());
            street.setText(contact.getStreet());

            makeAllEditable(false);
        } else
            setTitle("Add Contact");

        handler = new Handler(new IncomingHandlerCallback());
    }

    private class IncomingHandlerCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {

            if (msg.what == 1) {
                startActivity(new Intent(EditContactActivity.this, ContactsActivity.class));
                finish();
            }

            return true;
        }
    }

    private void makeAllEditable(boolean isEditable) {
        makeEditable(firstName, isEditable);
        makeEditable(lastName, isEditable);
        makeEditable(phone, isEditable);
        makeEditable(email, isEditable);
        makeEditable(country, isEditable);
        makeEditable(city, isEditable);
        makeEditable(street, isEditable);
    }

    private void makeEditable(EditText editText, boolean isEditable) {
        editText.setClickable(isEditable);
        editText.setCursorVisible(isEditable);
        editText.setFocusable(isEditable);
        editText.setFocusableInTouchMode(isEditable);
    }

    private void addContact() {
        final Contact contact = new Contact();
        if (firstName.getText().toString().length() < 3) {
            Toast.makeText(getApplicationContext(), "First name" + NOT_ENOUGH_CHAR_MSG, Toast.LENGTH_SHORT).show();
            return;
        } else if (phone.getText().toString().length() < 3) {
            Toast.makeText(getApplicationContext(), "Phone number" + NOT_ENOUGH_CHAR_MSG, Toast.LENGTH_SHORT).show();
            return;
        }

        contact.setFirstName(firstName.getText().toString());
        contact.setPhone(phone.getText().toString());

        if (!lastName.getText().toString().isEmpty())
            contact.setLastName(lastName.getText().toString());

        if (!email.getText().toString().isEmpty())
            contact.setEmail(email.getText().toString());

        if (!country.getText().toString().isEmpty())
            contact.setCountry(country.getText().toString());

        if (!city.getText().toString().isEmpty())
            contact.setCity(city.getText().toString());

        if (!street.getText().toString().isEmpty())
            contact.setStreet(street.getText().toString());

        new Thread(new Runnable() {
            @Override
            public void run() {
                contactDB.addContact(contact);
                handler.sendEmptyMessage(1);
            }
        }).start();
    }

    private void updateContact() {
        contact.setFirstName(firstName.getText().toString());
        contact.setLastName(lastName.getText().toString());
        contact.setPhone(phone.getText().toString());
        contact.setEmail(email.getText().toString());
        contact.setCountry(country.getText().toString());
        contact.setCity(city.getText().toString());
        contact.setStreet(street.getText().toString());

        new Thread(new Runnable() {
            @Override
            public void run() {
                contactDB.updateContact(contact);
                handler.sendEmptyMessage(1);
            }
        }).start();
    }

    private void deleteContact() {
        deleteDialog = new AlertDialog.Builder(EditContactActivity.this);
        deleteDialog.setMessage(ASK_DELETE_MSG + contact.getFullName() + " ?");
        deleteDialog.setCancelable(false);
        deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        contactDB.removeContact(contact);
                        handler.sendEmptyMessage(1);
                    }
                }).start();
            }
        });

        deleteDialog.setNegativeButton("No", null);
        deleteDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.edit_contact_menu, menu);
        if (!onAddContact)
            menu.findItem(R.id.saveItem).setVisible(false);
        else {
            menu.findItem(R.id.saveItem).setVisible(true);
            menu.findItem(R.id.editItem).setVisible(false);
            menu.findItem(R.id.deleteItem).setVisible(false);
        }
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.editItem:
                makeAllEditable(true);
                item.setVisible(false);
                menu.findItem(R.id.saveItem).setVisible(true);
                break;

            case R.id.saveItem:
                if (!onAddContact)
                    updateContact();
                else
                    addContact();
                break;

            case R.id.deleteItem:
                deleteContact();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handler.sendEmptyMessage(1);
    }

}

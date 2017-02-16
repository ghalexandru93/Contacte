package com.ghalexandru.contacts.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ghalexandru on 1/10/17.
 */

public class ContactDB {
    private PhoneContacts phoneContacts;
    private SQLiteOpenHelper contactsDB;
    private SQLiteDatabase database;

    public ContactDB(Context context) {
        contactsDB = new ContactDBHelper(context);
        phoneContacts = new PhoneContacts(context.getContentResolver());
    }

    //  Open database
    public void open() {
        database = contactsDB.getWritableDatabase();
    }

    //  Close database
    public void close() {
        contactsDB.close();
    }

    public Contact addContact(Contact contact) {
        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(ContactDBHelper.COLUMN_FIRST_NAME, contact.getFirstName());
        contentValues.put(ContactDBHelper.COLUMN_LAST_NAME, contact.getLastName());
        contentValues.put(ContactDBHelper.COLUMN_PHONE, contact.getPhone());
        contentValues.put(ContactDBHelper.COLUMN_EMAIL, contact.getEmail());
        contentValues.put(ContactDBHelper.COLUMN_COUNTRY, contact.getCountry());
        contentValues.put(ContactDBHelper.COLUMN_CITY, contact.getCity());
        contentValues.put(ContactDBHelper.COLUMN_STREET, contact.getStreet());
        long id = database.insert(ContactDBHelper.TABLE_NAME, null, contentValues);
        contact.setId(id);

        close();
        return contact;
    }

    public List<Contact> addContact(List<Contact> contactsList) {
        for (Contact contact : contactsList)
            addContact(contact);

        return contactsList;
    }

    //  Retrieve a contact using id
    public Contact getContact(long id) {
        open();

        Cursor cursor = database.rawQuery("SELECT * FROM " + ContactDBHelper.TABLE_NAME + " WHERE ID=" + id + "", null);
        if (cursor != null)
            cursor.moveToFirst();

        Contact contact = fillContact(cursor);

        close();
        return contact;
    }

    //  Helper method
    private Contact fillContact(Cursor cursor) {
        Contact contact = new Contact();
        contact.setId(cursor.getLong(cursor.getColumnIndex(ContactDBHelper.COLUMN_ID)));
        contact.setFirstName(cursor.getString(cursor.getColumnIndex(ContactDBHelper.COLUMN_FIRST_NAME)));
        contact.setLastName(cursor.getString(cursor.getColumnIndex(ContactDBHelper.COLUMN_LAST_NAME)));
        contact.setPhone(cursor.getString(cursor.getColumnIndex(ContactDBHelper.COLUMN_PHONE)));
        contact.setEmail(cursor.getString(cursor.getColumnIndex(ContactDBHelper.COLUMN_EMAIL)));
        contact.setCountry(cursor.getString(cursor.getColumnIndex(ContactDBHelper.COLUMN_COUNTRY)));
        contact.setCity(cursor.getString(cursor.getColumnIndex(ContactDBHelper.COLUMN_CITY)));
        contact.setStreet(cursor.getString(cursor.getColumnIndex(ContactDBHelper.COLUMN_STREET)));
        return contact;
    }

    public List<Contact> getAllContacts() {
        open();

        Cursor cursor = database.rawQuery("SELECT * FROM " + ContactDBHelper.TABLE_NAME, null);

        List<Contact> contacts = new ArrayList<>();
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                Contact contact = fillContact(cursor);
                contacts.add(contact);
            }
        }

        close();
        return contacts;
    }

    //  Replace a contact
    public int updateContact(Contact contact) {
        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(ContactDBHelper.COLUMN_FIRST_NAME, contact.getFirstName());
        contentValues.put(ContactDBHelper.COLUMN_LAST_NAME, contact.getLastName());
        contentValues.put(ContactDBHelper.COLUMN_PHONE, contact.getPhone());
        contentValues.put(ContactDBHelper.COLUMN_EMAIL, contact.getEmail());
        contentValues.put(ContactDBHelper.COLUMN_COUNTRY, contact.getCountry());
        contentValues.put(ContactDBHelper.COLUMN_CITY, contact.getCity());
        contentValues.put(ContactDBHelper.COLUMN_STREET, contact.getStreet());

        int update = database.update(ContactDBHelper.TABLE_NAME, contentValues, ContactDBHelper.COLUMN_ID + "=?", new String[]{String.valueOf(contact.getId())});

        close();
        return update;
    }

    public void removeContact(Contact contact) {
        open();

        database.delete(ContactDBHelper.TABLE_NAME, ContactDBHelper.COLUMN_ID + "=" + contact.getId(), null);

        close();
    }

    //  Remove all contacts from application database
    public void removeAllContacts() {
        open();

        database.execSQL("DELETE FROM " + ContactDBHelper.TABLE_NAME);

        close();
    }

    public List<Contact> getPhoneContacts() {
        return phoneContacts.readContacts();
    }

    private class PhoneContacts {
        private ContentResolver contentResolver;

        public PhoneContacts(ContentResolver contentResolver) {
            this.contentResolver = contentResolver;
        }

        //  Retrieve all contacts from phone agenda
        public List<Contact> readContacts() {
            Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null);

            String id, name;
            List<Contact> contactList = new ArrayList<>();

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                        //  If it doesn't have a phone number or a name it will continue the loop
                        boolean hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0;
                        if (name == null || name.isEmpty() || !hasPhoneNumber) continue;

                        Contact contact = new Contact();

                        //  Retrieve first and last name by splitting name.
                        //  If there is no white space to split name then
                        //  it will be no last name
                        if (name.contains(" ")) {
                            String str[] = name.split(" ");
                            contact.setFirstName(str[0]);
                            contact.setLastName(str[1]);
                        } else
                            contact.setFirstName(name);

                        fillPhoneNumber(id, contact);
                        fillEmail(id, contact);
                        fillPostalAddress(id, contact);
                        contactList.add(contact);
                    }
                }
                cursor.close();
            }

            return contactList;
        }

        //  Retrieve phone number
        private void fillPhoneNumber(String id, Contact contact) {
            Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{id}, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    if (phoneNumber != null && !phoneNumber.isEmpty())
                        contact.setPhone(phoneNumber);
                }
                cursor.close();
            }
        }

        //  Retrieve email address
        private void fillEmail(String id, Contact contact) {
            Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                    new String[]{id}, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String email = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                    if (email != null && !email.isEmpty())
                        contact.setEmail(email);
                }
                cursor.close();
            }
        }

        //  Retrieve country, city and street
        private void fillPostalAddress(String id, Contact contact) {
            String addrWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] addrWhereParams = new String[]{id,
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE};

            Cursor cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, addrWhere, addrWhereParams, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String street = cursor.getString(
                            cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
                    String city = cursor.getString(
                            cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
                    String country = cursor.getString(
                            cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));

                    if (street != null && !street.isEmpty())
                        contact.setStreet(street);

                    if (city != null && !city.isEmpty())
                        contact.setCity(city);

                    if (country != null && !country.isEmpty())
                        contact.setCountry(country);
                }
                cursor.close();
            }

        }
    }

}

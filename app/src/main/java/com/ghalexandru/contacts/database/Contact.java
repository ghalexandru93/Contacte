package com.ghalexandru.contacts.database;

import java.io.Serializable;

/**
 * Created by ghalexandru on 1/10/17.
 */
public class Contact implements Serializable {

    private long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;

    private String country;
    private String city;
    private String street;

    public Contact() {
    }

    public String getFullName() {
        if (firstName != null && lastName != null)
            return firstName + " " + lastName;
        else if (firstName != null)
            return firstName;
        else if (lastName != null)
            return lastName;

        return null;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @Override
    //  One contact will be equal to other if will have at least an name, email of phone number in common
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Contact contact = (Contact) o;

        if (getFullName() != null && contact.getFullName() != null && getFullName().equals(contact.getFullName()))
            return true;

        if (phone != null && contact.phone != null && phone.equals(contact.phone))
            return true;

        if (email != null && contact.email != null && email.equals(contact.email))
            return true;

        return false;
    }
}

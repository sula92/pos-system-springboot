package com.idet.pos.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

// JAXB root element name used in the XML response: <customer>...</customer>
@XmlRootElement(name = "customer")
// Field access lets JAXB read/write fields directly without extra annotation noise.
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlCustomerDemoDTO {

    private String id;
    private String name;
    private String email;

    public XmlCustomerDemoDTO() {
        // JAXB needs a default constructor.
    }

    public XmlCustomerDemoDTO(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}


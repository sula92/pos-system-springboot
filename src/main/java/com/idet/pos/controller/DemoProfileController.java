package com.idet.pos.controller;

import com.idet.pos.config.AppProfileMessageProvider;
import com.idet.pos.config.DemoEnvironmentBean;
import com.idet.pos.config.PosConfigurationProperties;
import com.idet.pos.dto.XmlCustomerDemoDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/demo")
public class DemoProfileController {

    private final Environment environment;
    private final AppProfileMessageProvider profileMessageProvider;
    private final DemoEnvironmentBean demoEnvironmentBean;
    private final PosConfigurationProperties posConfigurationProperties;

    @Value("${app.demo.environment:default}")
    private String configuredEnvironment;

    public DemoProfileController(
            Environment environment,
            AppProfileMessageProvider profileMessageProvider,
            DemoEnvironmentBean demoEnvironmentBean,
            PosConfigurationProperties posConfigurationProperties
    ) {
        this.environment = environment;
        this.profileMessageProvider = profileMessageProvider;
        this.demoEnvironmentBean = demoEnvironmentBean;
        this.posConfigurationProperties = posConfigurationProperties;
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfileDemo() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        response.put("configuredEnvironment", configuredEnvironment);
        response.put("profileMessage", profileMessageProvider.getMessage());
        response.put("commonBeanMessage", demoEnvironmentBean.getMessage());
        response.put("storeName", posConfigurationProperties.getStoreName());
        return ResponseEntity.ok(response);
    }

    /**
     * Demo endpoint that returns a JAXB-annotated DTO as XML.
     * Spring uses {@code @XmlRootElement} on {@code XmlCustomerDemoDTO} to build the XML response body.
     */
    @GetMapping(value = "/xml-customer", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<XmlCustomerDemoDTO> getXmlCustomerDemo() {
        // This DTO is annotated with @XmlRootElement, so Spring serializes it as XML automatically.
        XmlCustomerDemoDTO payload = new XmlCustomerDemoDTO("C001", "Nimal", "nimal@example.com");
        return ResponseEntity
                .ok()
                // Force XML in the response to make the content negotiation behavior explicit for the demo.
                .contentType(MediaType.APPLICATION_XML)
                .body(payload);
    }

    @PostMapping(value = "/xml-customer", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<XmlCustomerDemoDTO> postXmlCustomerDemo(@RequestBody XmlCustomerDemoDTO customer) {
        // Echo back the received XML customer data in the response.
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(customer);
    }

    /**
     * Small Spring HATEOAS demo endpoint that returns customer data with discoverable links.
     */
    @GetMapping("/hateoas/customer/{id}")
    public EntityModel<Map<String, Object>> getHateoasCustomerDemo(@PathVariable String id) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        payload.put("name", "Nimal");
        payload.put("email", "nimal@example.com");

        // EntityModel wraps plain data and attaches hypermedia links so clients can discover next actions.
        // Include a self link and related demo links to show navigation options in the response.
        return EntityModel.of(
                payload,
                linkTo(methodOn(DemoProfileController.class).getHateoasCustomerDemo(id)).withSelfRel(),
                linkTo(methodOn(DemoProfileController.class).getProfileDemo()).withRel("profile-demo"),
                linkTo(methodOn(DemoProfileController.class).getXmlCustomerDemo()).withRel("xml-demo")
        );

       /* {
            "id": "C001",
                "name": "Nimal",
                "email": "nimal@example.com",
                "_links": {
            "self": { "href": "http://localhost:8080/demo/hateoas/customer/C001" },
            "profile-demo": { "href": "http://localhost:8080/demo/profile" },
            "xml-demo": { "href": "http://localhost:8080/demo/xml-customer" }
        }*/
    }
}


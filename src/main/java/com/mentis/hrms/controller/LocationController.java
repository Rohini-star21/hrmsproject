package com.mentis.hrms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard/hr")  // Keep this as base
public class LocationController {

    @GetMapping("/locations")  // This matches /dashboard/hr/locations
    public String showLocationsPage() {
        return "locations";
    }

}
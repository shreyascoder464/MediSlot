package com.medislot.app.controller;

import com.medislot.app.entity.Role;
import com.medislot.app.service.DoctorService;
import com.medislot.app.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final NotificationService notificationService;

    public DoctorController(DoctorService doctorService, NotificationService notificationService) {
        this.doctorService = doctorService;
        this.notificationService = notificationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long doctorId = requireDoctor(session);
        if (doctorId == null) return "redirect:/login";
        model.addAttribute("name", session.getAttribute("name"));
        model.addAttribute("profile", doctorService.profile(doctorId));
        model.addAttribute("appointments", doctorService.appointments(doctorId));
        model.addAttribute("notifications", notificationService.forUser(doctorId));
        return "doctor-dashboard";
    }

    @GetMapping("/slots")
    public String slots(HttpSession session, Model model) {
        Long doctorId = requireDoctor(session);
        if (doctorId == null) return "redirect:/login";
        model.addAttribute("slots", doctorService.slots(doctorId));
        model.addAttribute("today", LocalDate.now());
        return "doctor-slots";
    }

    @PostMapping("/slots")
    public String createSlots(
            @RequestParam LocalDate date,
            @RequestParam LocalTime start,
            @RequestParam LocalTime end,
            @RequestParam Integer duration,
            HttpSession session,
            Model model
    ) {
        Long doctorId = requireDoctor(session);
        if (doctorId == null) return "redirect:/login";
        try {
            doctorService.createSlots(doctorId, date, start, end, duration);
            return "redirect:/doctor/slots?created";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("slots", doctorService.slots(doctorId));
            model.addAttribute("today", LocalDate.now());
            return "doctor-slots";
        }
    }

    @GetMapping("/appointments")
    public String appointments(HttpSession session, Model model) {
        Long doctorId = requireDoctor(session);
        if (doctorId == null) return "redirect:/login";
        model.addAttribute("appointments", doctorService.appointments(doctorId));
        return "doctor-appointments";
    }

    @PostMapping("/appointments/{id}/approve")
    public String approve(@PathVariable Long id, HttpSession session) {
        Long doctorId = requireDoctor(session);
        if (doctorId == null) return "redirect:/login";
        doctorService.approveAppointment(id, doctorId);
        return "redirect:/doctor/appointments?approved";
    }

    @PostMapping("/appointments/{id}/reject")
    public String reject(@PathVariable Long id, HttpSession session) {
        Long doctorId = requireDoctor(session);
        if (doctorId == null) return "redirect:/login";
        doctorService.rejectAppointment(id, doctorId);
        return "redirect:/doctor/appointments?rejected";
    }

    @PostMapping("/appointments/{id}/complete")
    public String complete(@PathVariable Long id, HttpSession session) {
        Long doctorId = requireDoctor(session);
        if (doctorId == null) return "redirect:/login";
        doctorService.completeAppointment(id, doctorId);
        return "redirect:/doctor/appointments?completed";
    }

    private Long requireDoctor(HttpSession session) {
        if (session.getAttribute("role") != Role.DOCTOR) {
            return null;
        }
        return (Long) session.getAttribute("userId");
    }
}

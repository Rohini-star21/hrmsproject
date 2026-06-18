package com.mentis.hrms.service;

import com.mentis.hrms.model.Holiday;
import com.mentis.hrms.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class HolidayService {

    @Autowired
    private HolidayRepository holidayRepository;

    // Create new holiday
    @Transactional
    public Holiday createHoliday(Holiday holiday) {
        holiday.setCreatedAt(LocalDate.now().atStartOfDay());
        holiday.setUpdatedAt(LocalDate.now().atStartOfDay());
        return holidayRepository.save(holiday);
    }

    // Get all holidays
    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAll();
    }

    // Get holidays by date
    public List<Holiday> getHolidaysByDate(LocalDate date) {
        return holidayRepository.findByHolidayDate(date);
    }

    // Get holidays by month and year
    public List<Holiday> getHolidaysByMonth(int year, int month) {
        return holidayRepository.findByYearAndMonth(year, month);
    }

    // Get upcoming holidays
    public List<Holiday> getUpcomingHolidays() {
        return holidayRepository.findUpcomingHolidays(LocalDate.now());
    }

    // Update holiday
    @Transactional
    public Holiday updateHoliday(Long id, Holiday holidayDetails) {
        Optional<Holiday> optionalHoliday = holidayRepository.findById(id);
        if (optionalHoliday.isPresent()) {
            Holiday holiday = optionalHoliday.get();
            holiday.setHolidayName(holidayDetails.getHolidayName());
            holiday.setHolidayDate(holidayDetails.getHolidayDate());
            holiday.setHolidayType(holidayDetails.getHolidayType());
            holiday.setDescription(holidayDetails.getDescription());
            holiday.setUpdatedAt(LocalDate.now().atStartOfDay());
            return holidayRepository.save(holiday);
        }
        return null;
    }

    // Delete holiday
    @Transactional
    public boolean deleteHoliday(Long id) {
        if (holidayRepository.existsById(id)) {
            holidayRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Get holiday by ID
    public Optional<Holiday> getHolidayById(Long id) {
        return holidayRepository.findById(id);
    }
}
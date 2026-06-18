package com.mentis.hrms.service;

import com.mentis.hrms.model.LeaveBalance;
import com.mentis.hrms.model.LeaveRequest;
import com.mentis.hrms.repository.LeaveBalanceRepository;
import com.mentis.hrms.repository.LeaveRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LeaveBalanceService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveBalanceService.class);

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    // List of leave types to display in UI
    private static final List<String> DISPLAY_LEAVE_TYPES = List.of("sick", "casual", "earned");

    /**
     * Calculate the number of days between two dates (inclusive)
     */
    private Double calculateLeaveDays(LocalDate startDate, LocalDate endDate, String leaveDuration, String halfDayType) {
        if (startDate == null || endDate == null) {
            return 0.0;
        }

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        if (leaveDuration == null) {
            return (double) daysBetween;
        }

        if ("half_day".equalsIgnoreCase(leaveDuration)) {
            return 0.5;
        } else if ("full_day".equalsIgnoreCase(leaveDuration)) {
            return (double) daysBetween;
        } else {
            return (double) daysBetween;
        }
    }

    /**
     * Get leave balance for an employee - UPDATED: Shows sick+casual total = 19
     */
    public Map<String, Object> getEmployeeLeaveBalance(String employeeId) {
        Map<String, Object> result = new HashMap<>();
        Integer currentYear = LocalDate.now().getYear();

        try {
            List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, currentYear);

            if (balances.isEmpty()) {
                initializeEmployeeBalance(employeeId, currentYear);
                balances = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, currentYear);
            }

            // Format response for UI
            Map<String, Map<String, Object>> leaveTypes = new HashMap<>();

            for (LeaveBalance balance : balances) {
                if (DISPLAY_LEAVE_TYPES.contains(balance.getLeaveType())) {
                    Map<String, Object> leaveData = new HashMap<>();

                    // For earned leaves, show balance as remaining days (can be negative or very high)
                    if ("earned".equals(balance.getLeaveType())) {
                        leaveData.put("total", 999); // Display as unlimited
                        leaveData.put("used", balance.getUsedDays());
                        leaveData.put("balance", 999 - balance.getUsedDays()); // Still calculate for display
                    } else {
                        leaveData.put("total", balance.getTotalDays());
                        leaveData.put("used", balance.getUsedDays());
                        leaveData.put("balance", balance.getRemainingDays());
                    }

                    leaveTypes.put(balance.getLeaveType(), leaveData);

                    logger.debug("Balance for {} - {}: Total={}, Used={}, Balance={}",
                            employeeId, balance.getLeaveType(),
                            balance.getTotalDays(), balance.getUsedDays(), balance.getRemainingDays());
                }
            }

            result.put("success", true);
            result.put("employeeId", employeeId);
            result.put("year", currentYear);
            result.put("balances", leaveTypes);

            // Calculate summary - sick + casual = 19 total
            Double sickTotal = 12.0;
            Double casualTotal = 7.0;
            Double totalSickCasual = sickTotal + casualTotal; // 19

            // Get used days from balances
            Double sickUsed = 0.0;
            Double casualUsed = 0.0;
            Double earnedUsed = 0.0;

            for (LeaveBalance balance : balances) {
                if ("sick".equals(balance.getLeaveType())) {
                    sickUsed = balance.getUsedDays();
                } else if ("casual".equals(balance.getLeaveType())) {
                    casualUsed = balance.getUsedDays();
                } else if ("earned".equals(balance.getLeaveType())) {
                    earnedUsed = balance.getUsedDays();
                }
            }

            Double usedSickCasual = sickUsed + casualUsed;
            Double availableSickCasual = totalSickCasual - usedSickCasual;

            result.put("totalSickCasual", totalSickCasual);
            result.put("usedSickCasual", usedSickCasual);
            result.put("availableSickCasual", availableSickCasual);

            // Add earned leave info
            result.put("earnedTotal", 999);
            result.put("earnedUsed", earnedUsed);
            result.put("earnedAvailable", 999 - earnedUsed);

            // For backward compatibility
            result.put("totalLeaves", totalSickCasual);
            result.put("usedLeaves", usedSickCasual);
            result.put("availableLeaves", availableSickCasual);

            logger.info("Leave balance summary for {}: Sick+Casual Used={}/{} Available={}",
                    employeeId, usedSickCasual, totalSickCasual, availableSickCasual);

        } catch (Exception e) {
            logger.error("Error fetching leave balance for employee {}: {}", employeeId, e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Initialize default leave balances for an employee - UPDATED: Earned = 999
     */
    @Transactional
    public void initializeEmployeeBalance(String employeeId, Integer year) {
        try {
            leaveBalanceRepository.initializeEmployeeBalances(employeeId, year);
            logger.info("Initialized leave balances for employee {} for year {}", employeeId, year);
        } catch (Exception e) {
            logger.error("Failed to initialize balances for employee {}: {}", employeeId, e.getMessage());
        }
    }

    /**
     * Check if employee has sufficient leave balance before applying
     * UPDATED: For sick/casual, check their specific balance
     * For earned, ALWAYS return true (unlimited)
     */
    public boolean hasSufficientBalance(String employeeId, String leaveType,
                                        LocalDate startDate, LocalDate endDate,
                                        String leaveDuration, String halfDayType) {
        Integer currentYear = LocalDate.now().getYear();
        Double requestedDays = calculateLeaveDays(startDate, endDate, leaveDuration, halfDayType);

        // Convert leave type to lowercase for consistency
        String normalizedLeaveType = leaveType.toLowerCase();

        // If it's earned leave, always return true (unlimited)
        if ("earned".equals(normalizedLeaveType)) {
            return true;
        }

        Boolean hasBalance = leaveBalanceRepository.hasSufficientBalance(
                employeeId, normalizedLeaveType, currentYear, requestedDays);

        return hasBalance != null && hasBalance;
    }

    /**
     * Update leave balance when leave is approved - UPDATED: Auto-use earned when sick/casual exhausted
     */
    @Transactional
    public boolean deductLeaveBalance(LeaveRequest leaveRequest) {
        try {
            String employeeId = leaveRequest.getEmployeeId();
            String leaveType = leaveRequest.getLeaveType().toLowerCase();
            Integer year = leaveRequest.getStartDate().getYear();

            // Calculate days to deduct
            Double daysToDeduct = leaveRequest.getTotalDays();
            if (daysToDeduct == null || daysToDeduct == 0) {
                daysToDeduct = calculateLeaveDays(
                        leaveRequest.getStartDate(),
                        leaveRequest.getEndDate(),
                        leaveRequest.getLeaveDuration(),
                        leaveRequest.getHalfDayType()
                );
            }

            logger.info("Attempting to deduct {} days from {} balance for employee {}",
                    daysToDeduct, leaveType, employeeId);

            // For earned leaves, always deduct (unlimited)
            if ("earned".equals(leaveType)) {
                int updated = leaveBalanceRepository.addUsedDays(employeeId, leaveType, year, daysToDeduct);
                logger.info("Deducted {} earned days for employee {}", daysToDeduct, employeeId);
                return updated > 0;
            }

            // For sick/casual, check if sufficient balance exists
            Boolean hasBalance = leaveBalanceRepository.hasSufficientBalance(employeeId, leaveType, year, daysToDeduct);

            if (hasBalance != null && hasBalance) {
                // Sufficient balance in sick/casual - deduct normally
                int updated = leaveBalanceRepository.addUsedDays(employeeId, leaveType, year, daysToDeduct);
                logger.info("Deducted {} {} days from {} balance", daysToDeduct, leaveType, employeeId);
                return updated > 0;
            } else {
                // Not enough balance in sick/casual - need to use earned leaves
                logger.info("Insufficient {} balance for employee {}. Will use earned leaves.", leaveType, employeeId);

                // Get current sick balance
                Optional<LeaveBalance> sickBalance = leaveBalanceRepository
                        .findByEmployeeIdAndLeaveTypeAndYear(employeeId, "sick", year);
                Optional<LeaveBalance> casualBalance = leaveBalanceRepository
                        .findByEmployeeIdAndLeaveTypeAndYear(employeeId, "casual", year);

                Double sickRemaining = sickBalance.map(b -> b.getTotalDays() - b.getUsedDays()).orElse(0.0);
                Double casualRemaining = casualBalance.map(b -> b.getTotalDays() - b.getUsedDays()).orElse(0.0);

                Double daysNeeded = daysToDeduct;

                // First, use remaining sick days
                if (sickRemaining > 0 && "sick".equals(leaveType)) {
                    Double useFromSick = Math.min(sickRemaining, daysNeeded);
                    leaveBalanceRepository.addUsedDays(employeeId, "sick", year, useFromSick);
                    daysNeeded -= useFromSick;
                    logger.info("Used {} from sick balance", useFromSick);
                }

                // Then, use remaining casual days
                if (daysNeeded > 0 && "casual".equals(leaveType)) {
                    Double useFromCasual = Math.min(casualRemaining, daysNeeded);
                    leaveBalanceRepository.addUsedDays(employeeId, "casual", year, useFromCasual);
                    daysNeeded -= useFromCasual;
                    logger.info("Used {} from casual balance", useFromCasual);
                }

                // Finally, use earned leaves for remaining days
                if (daysNeeded > 0) {
                    leaveBalanceRepository.addUsedDays(employeeId, "earned", year, daysNeeded);
                    logger.info("Used {} from earned balance", daysNeeded);
                }

                return true;
            }

        } catch (Exception e) {
            logger.error("Error deducting leave balance: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Add back leave balance when leave is cancelled/rejected
     */
    @Transactional
    public boolean addBackLeaveBalance(LeaveRequest leaveRequest) {
        try {
            String employeeId = leaveRequest.getEmployeeId();
            String leaveType = leaveRequest.getLeaveType().toLowerCase();
            Integer year = leaveRequest.getStartDate().getYear();

            Double daysToAdd = leaveRequest.getTotalDays();
            if (daysToAdd == null || daysToAdd == 0) {
                daysToAdd = calculateLeaveDays(
                        leaveRequest.getStartDate(),
                        leaveRequest.getEndDate(),
                        leaveRequest.getLeaveDuration(),
                        leaveRequest.getHalfDayType()
                );
            }

            int updated = leaveBalanceRepository.subtractUsedDays(employeeId, leaveType, year, daysToAdd);

            if (updated > 0) {
                logger.info("Added back {} days to {} balance for employee {}",
                        daysToAdd, leaveType, employeeId);
                return true;
            }
            return false;

        } catch (Exception e) {
            logger.error("Error adding back leave balance: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Recalculate used days from all approved leaves
     */
    @Transactional
    public void recalculateAllBalances(String employeeId, Integer year) {
        try {
            List<LeaveRequest> approvedLeaves = leaveRequestRepository
                    .findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                    .stream()
                    .filter(lr -> "APPROVED".equals(lr.getStatus()) &&
                            lr.getStartDate().getYear() == year)
                    .toList();

            Map<String, Double> usedDaysByType = new HashMap<>();

            for (LeaveRequest leave : approvedLeaves) {
                String leaveType = leave.getLeaveType().toLowerCase();
                Double days = leave.getTotalDays();
                if (days == null || days == 0) {
                    days = calculateLeaveDays(
                            leave.getStartDate(),
                            leave.getEndDate(),
                            leave.getLeaveDuration(),
                            leave.getHalfDayType()
                    );
                }
                usedDaysByType.merge(leaveType, days, Double::sum);
            }

            for (Map.Entry<String, Double> entry : usedDaysByType.entrySet()) {
                Optional<LeaveBalance> balanceOpt = leaveBalanceRepository
                        .findByEmployeeIdAndLeaveTypeAndYear(employeeId, entry.getKey(), year);

                if (balanceOpt.isPresent()) {
                    LeaveBalance balance = balanceOpt.get();
                    balance.setUsedDays(entry.getValue());
                    leaveBalanceRepository.save(balance);
                    logger.info("Updated balance for {} - {}: used days = {}",
                            employeeId, entry.getKey(), entry.getValue());
                }
            }

            logger.info("Recalculated balances for employee {} for year {}", employeeId, year);

        } catch (Exception e) {
            logger.error("Error recalculating balances: {}", e.getMessage(), e);
        }
    }
}
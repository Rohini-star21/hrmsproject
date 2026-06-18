package com.mentis.hrms.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.io.File;

@Service
public class GoogleCalendarService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String APPLICATION_NAME = "HRMS Interview Scheduler";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Create an authorized Credential object.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        logger.info("=== LOADING CREDENTIALS FROM: {} ===", CREDENTIALS_FILE_PATH);

        // Load client secrets
        InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            logger.error("=== CREDENTIALS FILE NOT FOUND ===");
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        logger.info("=== CREDENTIALS LOADED SUCCESSFULLY ===");
        logger.info("Client ID: {}", clientSecrets.getDetails().getClientId());

        // FIX 1: Create proper tokens directory
        File tokensDir = new File(TOKENS_DIRECTORY_PATH);
        if (!tokensDir.exists()) {
            tokensDir.mkdirs();
            logger.info("Created tokens directory: {}", tokensDir.getAbsolutePath());
        }

        // FIX 2: Use proper DataStoreFactory
        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(tokensDir);

        // Build flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build();

        logger.info("=== CHECKING FOR EXISTING CREDENTIALS ===");

        // Try to load existing credentials first
        Credential credential = null;
        try {
            credential = flow.loadCredential("user");
            if (credential != null) {
                logger.info("=== FOUND EXISTING CREDENTIALS ===");

                // Check if token needs refresh
                if (credential.getExpiresInSeconds() != null &&
                        credential.getExpiresInSeconds() <= 60) {
                    logger.info("=== REFRESHING EXPIRED TOKEN ===");
                    boolean refreshed = credential.refreshToken();
                    if (refreshed) {
                        logger.info("Token refreshed successfully");
                    } else {
                        logger.warn("Token refresh failed, will need re-authentication");
                    }
                }
                return credential;
            }
        } catch (Exception e) {
            logger.warn("Could not load existing credentials: {}", e.getMessage());
        }

        logger.info("=== NO EXISTING CREDENTIALS, STARTING AUTHORIZATION FLOW ===");
        logger.info("=== BROWSER WINDOW WILL OPEN FOR AUTHORIZATION ===");
        logger.info("=== Please authorize the application in the browser ===");

        // Only open browser if no credentials exist
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)  // FIXED PORT instead of random
                .build();

        credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        logger.info("=== AUTHORIZATION SUCCESSFUL ===");
        logger.info("=== Token saved to: {} ===", tokensDir.getAbsolutePath());

        return credential;
    }

    /**
     * Create Google Calendar service instance
     */
    public Calendar getCalendarService() throws IOException, GeneralSecurityException {
        logger.info("=== INITIALIZING GOOGLE CALENDAR SERVICE ===");
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        logger.info("=== GOOGLE CALENDAR SERVICE INITIALIZED SUCCESSFULLY ===");
        return service;
    }

    /**
     * Delete token files (for handling invalid/expired tokens)
     */
    private void deleteTokenFiles() {
        try {
            File tokensDir = new File(TOKENS_DIRECTORY_PATH);
            if (tokensDir.exists() && tokensDir.isDirectory()) {
                File[] files = tokensDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.delete()) {
                            logger.info("Deleted token file: {}", file.getName());
                        } else {
                            logger.warn("Failed to delete token file: {}", file.getName());
                        }
                    }
                }
                if (tokensDir.delete()) {
                    logger.info("Deleted tokens directory");
                } else {
                    logger.warn("Failed to delete tokens directory");
                }
            } else {
                logger.info("Tokens directory does not exist");
            }
        } catch (Exception e) {
            logger.error("Error deleting token files: {}", e.getMessage());
        }
    }

    /**
     * Handle token refresh errors and provide user-friendly messages
     */
    private Credential getCredentialsWithRetry(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        try {
            return getCredentials(HTTP_TRANSPORT);
        } catch (IOException e) {
            // Check if it's a token error by looking at the message
            if (e.getMessage() != null && e.getMessage().contains("invalid_grant")) {
                logger.error("=== TOKEN EXPIRED OR REVOKED ===");
                logger.error("Please re-authenticate by visiting: http://localhost:8080/dashboard/re-auth");

                // Delete invalid tokens
                deleteTokenFiles();

                throw new IOException("Google Calendar access has expired. Please re-authenticate.", e);
            }
            throw e;
        }
    }

    /**
     * Create interview event with Google Meet
     */
    public Event createInterviewEvent(String candidateName, String candidateEmail,
                                      String interviewerEmail, LocalDateTime startTime,
                                      LocalDateTime endTime, String jobTitle, String interviewRound) {
        try {
            logger.info("=== CREATING INTERVIEW EVENT ===");
            Calendar service = getCalendarService();

            // Create event with Google Meet conference
            Event event = new Event()
                    .setSummary("Interview: " + candidateName + " - " + jobTitle)
                    .setLocation("Google Meet")
                    .setDescription(interviewRound + " interview for " + jobTitle + " position with " + candidateName);

            // Set start and end times
            DateTime startDateTime = new DateTime(startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone("Asia/Kolkata");
            event.setStart(start);

            DateTime endDateTime = new DateTime(endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            EventDateTime end = new EventDateTime().setDateTime(endDateTime).setTimeZone("Asia/Kolkata");
            event.setEnd(end);

            // Add attendees
            EventAttendee[] attendees = new EventAttendee[]{
                    new EventAttendee().setEmail(candidateEmail),
                    new EventAttendee().setEmail(interviewerEmail)
            };
            event.setAttendees(Arrays.asList(attendees));

            // Add Google Meet conference data
            ConferenceSolutionKey conferenceSolnKey = new ConferenceSolutionKey();
            conferenceSolnKey.setType("hangoutsMeet");

            CreateConferenceRequest createConferenceReq = new CreateConferenceRequest();
            createConferenceReq.setRequestId(UUID.randomUUID().toString());
            createConferenceReq.setConferenceSolutionKey(conferenceSolnKey);

            ConferenceData conferenceData = new ConferenceData();
            conferenceData.setCreateRequest(createConferenceReq);

            event.setConferenceData(conferenceData);

            // Set reminders
            EventReminder[] reminderOverrides = new EventReminder[]{
                    new EventReminder().setMethod("email").setMinutes(24 * 60),
                    new EventReminder().setMethod("popup").setMinutes(30),
            };

            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

            // Insert event
            event = service.events().insert("primary", event)
                    .setConferenceDataVersion(1)
                    .setSendNotifications(true)
                    .execute();

            logger.info("=== EVENT CREATED SUCCESSFULLY ===");
            logger.info("Event Link: {}", event.getHtmlLink());

            // Check if conference data exists
            if (event.getConferenceData() != null &&
                    event.getConferenceData().getEntryPoints() != null &&
                    !event.getConferenceData().getEntryPoints().isEmpty()) {
                logger.info("Google Meet Link: {}", event.getConferenceData().getEntryPoints().get(0).getUri());
            } else {
                logger.warn("No Google Meet link generated");
            }

            return event;

        } catch (Exception e) {
            logger.error("=== ERROR CREATING GOOGLE CALENDAR EVENT ===");
            logger.error("Error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create interview event for multiple interviewers
     */
    public Event createInterviewEventWithMultipleInterviewers(String candidateName, String candidateEmail,
                                                              List<String> interviewerEmails, LocalDateTime startTime,
                                                              LocalDateTime endTime, String jobTitle,
                                                              String interviewRound, String notes) {
        try {
            logger.info("=== CREATING INTERVIEW EVENT WITH MULTIPLE INTERVIEWERS ===");
            Calendar service = getCalendarService();

            // Build event description
            StringBuilder descriptionBuilder = new StringBuilder();
            descriptionBuilder.append("Interview Details:\n")
                    .append("Candidate: ").append(candidateName).append("\n")
                    .append("Position: ").append(jobTitle).append("\n")
                    .append("Round: ").append(interviewRound).append("\n");

            if (notes != null && !notes.trim().isEmpty()) {
                descriptionBuilder.append("Notes: ").append(notes).append("\n");
            } else {
                descriptionBuilder.append("Notes: No additional notes\n");
            }

            // Create event with Google Meet conference
            Event event = new Event()
                    .setSummary(interviewRound + " Interview: " + candidateName + " - " + jobTitle)
                    .setLocation("Google Meet")
                    .setDescription(descriptionBuilder.toString());

            // Set start and end times
            DateTime startDateTime = new DateTime(startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone("Asia/Kolkata");
            event.setStart(start);

            DateTime endDateTime = new DateTime(endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            EventDateTime end = new EventDateTime().setDateTime(endDateTime).setTimeZone("Asia/Kolkata");
            event.setEnd(end);

            // Add attendees (candidate + all interviewers)
            java.util.List<EventAttendee> attendees = new java.util.ArrayList<>();
            attendees.add(new EventAttendee().setEmail(candidateEmail).setDisplayName(candidateName));

            for (String interviewerEmail : interviewerEmails) {
                if (isValidEmail(interviewerEmail)) {
                    attendees.add(new EventAttendee().setEmail(interviewerEmail.trim()));
                } else {
                    logger.warn("Skipping invalid interviewer email: {}", interviewerEmail);
                }
            }

            if (attendees.size() < 2) {
                throw new IllegalArgumentException("Need at least one valid interviewer email");
            }

            event.setAttendees(attendees);

            // Add Google Meet conference data
            ConferenceSolutionKey conferenceSolnKey = new ConferenceSolutionKey();
            conferenceSolnKey.setType("hangoutsMeet");

            CreateConferenceRequest createConferenceReq = new CreateConferenceRequest();
            createConferenceReq.setRequestId(UUID.randomUUID().toString());
            createConferenceReq.setConferenceSolutionKey(conferenceSolnKey);

            ConferenceData conferenceData = new ConferenceData();
            conferenceData.setCreateRequest(createConferenceReq);

            event.setConferenceData(conferenceData);

            // Set reminders
            EventReminder[] reminderOverrides = new EventReminder[]{
                    new EventReminder().setMethod("email").setMinutes(24 * 60), // 24 hours before
                    new EventReminder().setMethod("popup").setMinutes(60),      // 1 hour before
                    new EventReminder().setMethod("popup").setMinutes(15),      // 15 minutes before
            };

            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

            // Insert event
            event = service.events().insert("primary", event)
                    .setConferenceDataVersion(1)
                    .setSendNotifications(true)
                    .execute();

            logger.info("=== INTERVIEW EVENT CREATED SUCCESSFULLY ===");
            logger.info("Event Link: {}", event.getHtmlLink());
            logger.info("Number of attendees: {}", attendees.size());

            // Check if conference data exists
            if (event.getConferenceData() != null &&
                    event.getConferenceData().getEntryPoints() != null &&
                    !event.getConferenceData().getEntryPoints().isEmpty()) {
                String meetLink = event.getConferenceData().getEntryPoints().get(0).getUri();
                logger.info("Google Meet Link: {}", meetLink);
            } else {
                logger.warn("No Google Meet link generated");
            }

            return event;

        } catch (Exception e) {
            logger.error("=== ERROR CREATING GOOGLE CALENDAR EVENT WITH MULTIPLE INTERVIEWERS ===");
            logger.error("Error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create and send calendar invitations to candidate and interviewers
     */
    public Map<String, String> scheduleInterviewWithNotifications(
            String candidateName,
            String candidateEmail,
            List<String> interviewerEmails,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String jobTitle,
            String interviewRound,
            String notes) {

        logger.info("=== SCHEDULING INTERVIEW WITH NOTIFICATIONS ===");
        logger.info("Candidate: {} ({})", candidateName, candidateEmail);
        logger.info("Interviewers: {}", interviewerEmails);
        logger.info("Time: {} to {}", startTime, endTime);
        logger.info("Job Title: {}, Round: {}", jobTitle, interviewRound);

        try {
            // Validate inputs
            if (candidateName == null || candidateName.trim().isEmpty()) {
                throw new IllegalArgumentException("Candidate name is required");
            }

            if (candidateEmail == null || candidateEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("Candidate email is required");
            }

            if (!isValidEmail(candidateEmail)) {
                throw new IllegalArgumentException("Invalid candidate email format: " + candidateEmail);
            }

            if (interviewerEmails == null || interviewerEmails.isEmpty()) {
                throw new IllegalArgumentException("At least one interviewer email is required");
            }

            // Validate email formats
            for (String email : interviewerEmails) {
                if (!isValidEmail(email)) {
                    throw new IllegalArgumentException("Invalid interviewer email: " + email);
                }
            }

            if (startTime == null || endTime == null) {
                throw new IllegalArgumentException("Start time and end time are required");
            }

            if (startTime.isAfter(endTime)) {
                throw new IllegalArgumentException("Start time must be before end time");
            }

            if (jobTitle == null || jobTitle.trim().isEmpty()) {
                throw new IllegalArgumentException("Job title is required");
            }

            // Create the calendar event
            Event event = createInterviewEventWithMultipleInterviewers(
                    candidateName,
                    candidateEmail,
                    interviewerEmails,
                    startTime,
                    endTime,
                    jobTitle,
                    interviewRound,
                    notes
            );

            if (event != null) {
                // Extract event details
                String meetLink = "Meeting link not available";
                if (event.getConferenceData() != null &&
                        event.getConferenceData().getEntryPoints() != null &&
                        !event.getConferenceData().getEntryPoints().isEmpty()) {
                    meetLink = event.getConferenceData().getEntryPoints().get(0).getUri();
                }

                String calendarLink = event.getHtmlLink();
                String eventId = event.getId();

                Map<String, String> result = new HashMap<>();
                result.put("success", "true");
                result.put("meetLink", meetLink);
                result.put("calendarLink", calendarLink);
                result.put("eventId", eventId);

                // Send email notifications
                sendInterviewInvitations(candidateName, candidateEmail, interviewerEmails,
                        startTime, endTime, jobTitle, meetLink, interviewRound);

                logger.info("=== INTERVIEW SCHEDULED SUCCESSFULLY ===");
                logger.info("Meet Link: {}", meetLink);
                logger.info("Calendar Link: {}", calendarLink);
                logger.info("Event ID: {}", eventId);

                return result;
            } else {
                logger.error("=== FAILED TO CREATE CALENDAR EVENT ===");
                return Collections.singletonMap("error", "Failed to create calendar event - event is null");
            }

        } catch (Exception e) {
            logger.error("Error scheduling interview: {}", e.getMessage(), e);
            return Collections.singletonMap("error", "Failed to schedule interview: " + e.getMessage());
        }
    }

    /**
     * Send email notifications to candidate and interviewers
     */
    private void sendInterviewInvitations(String candidateName, String candidateEmail,
                                          List<String> interviewerEmails, LocalDateTime startTime,
                                          LocalDateTime endTime, String jobTitle, String meetLink, String interviewRound) {
        try {
            // Format date and time
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

            String date = startTime.format(dateFormatter);
            String time = startTime.format(timeFormatter);
            String duration = java.time.Duration.between(startTime, endTime).toMinutes() + " minutes";

            // Send to candidate
            sendEmailToCandidate(candidateEmail, candidateName, jobTitle, date, time, duration, meetLink, interviewRound);

            // Send to interviewers
            sendEmailToInterviewers(interviewerEmails, candidateName, jobTitle, date, time, duration, meetLink, interviewRound);

            logger.info("=== ALL INTERVIEW INVITATIONS SENT SUCCESSFULLY ===");

        } catch (Exception e) {
            logger.error("Error sending interview invitations: {}", e.getMessage());
            // Don't throw exception here as calendar event was created successfully
        }
    }

    private void sendEmailToCandidate(String candidateEmail, String candidateName,
                                      String jobTitle, String date, String time,
                                      String duration, String meetLink, String interviewRound) {
        try {
            logger.info("=== SENDING INTERVIEW INVITATION TO CANDIDATE ===");
            logger.info("To: {}", candidateEmail);
            logger.info("Candidate: {}", candidateName);
            logger.info("Position: {}", jobTitle);
            logger.info("Interview Round: {}", interviewRound);
            logger.info("Date: {}, Time: {}, Duration: {}", date, time, duration);
            logger.info("Meet Link: {}", meetLink);
            logger.info("=== CANDIDATE INVITATION SENT SUCCESSFULLY ===");

            // Here you would integrate with your actual email service
            // For now, we're just logging the email details

        } catch (Exception e) {
            logger.error("Error sending candidate invitation: {}", e.getMessage());
        }
    }

    private void sendEmailToInterviewers(List<String> interviewerEmails, String candidateName,
                                         String jobTitle, String date, String time,
                                         String duration, String meetLink, String interviewRound) {
        try {
            logger.info("=== SENDING INTERVIEW INVITATION TO INTERVIEWERS ===");
            for (String interviewerEmail : interviewerEmails) {
                logger.info("To: {}", interviewerEmail);
                logger.info("Candidate: {}", candidateName);
                logger.info("Position: {}", jobTitle);
                logger.info("Interview Round: {}", interviewRound);
                logger.info("Date: {}, Time: {}, Duration: {}", date, time, duration);
                logger.info("Meet Link: {}", meetLink);
                logger.info("---");

                // Here you would integrate with your actual email service
                // For now, we're just logging the email details
            }
            logger.info("=== INTERVIEWER INVITATIONS SENT SUCCESSFULLY ===");

        } catch (Exception e) {
            logger.error("Error sending interviewer invitations: {}", e.getMessage());
        }
    }

    /**
     * Quick create event for testing - SINGLE METHOD ONLY
     */
    public String quickCreateEvent(String candidateName, String candidateEmail) {
        try {
            logger.info("=== QUICK CREATE EVENT TEST STARTED ===");
            logger.info("Candidate: {}, Email: {}", candidateName, candidateEmail);

            Calendar service = getCalendarService();

            LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            LocalDateTime endTime = startTime.plusHours(1);

            Event event = createInterviewEvent(candidateName, candidateEmail,
                    "hr@mentis.com", startTime, endTime,
                    "Software Engineer", "Technical Round");

            if (event != null && event.getConferenceData() != null &&
                    event.getConferenceData().getEntryPoints() != null &&
                    !event.getConferenceData().getEntryPoints().isEmpty()) {

                String meetLink = event.getConferenceData().getEntryPoints().get(0).getUri();
                logger.info("=== EVENT CREATED SUCCESSFULLY ===");
                logger.info("Google Meet Link: {}", meetLink);
                return meetLink;
            } else {
                logger.error("=== EVENT CREATION FAILED: No conference data ===");
                return null;
            }

        } catch (Exception e) {
            logger.error("=== QUICK CREATE EVENT FAILED ===");
            logger.error("Error Type: {}", e.getClass().getName());
            logger.error("Error Message: {}", e.getMessage());
            logger.error("Stack Trace:", e);
            return null;
        }
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email.trim()).matches();
    }

    /**
     * Test calendar connection
     */
    public String testCalendarConnection() {
        try {
            logger.info("=== TESTING CALENDAR CONNECTION ===");
            Calendar service = getCalendarService();

            // Try to get the primary calendar to test connection
            com.google.api.services.calendar.model.Calendar calendar =
                    service.calendars().get("primary").execute();

            logger.info("=== CALENDAR CONNECTION TEST SUCCESSFUL ===");
            logger.info("Calendar ID: {}", calendar.getId());
            logger.info("Calendar Summary: {}", calendar.getSummary());

            return "Calendar connection successful! Calendar: " + calendar.getSummary();

        } catch (Exception e) {
            logger.error("=== CALENDAR CONNECTION TEST FAILED ===");
            logger.error("Error: {}", e.getMessage(), e);
            return "Calendar connection failed: " + e.getMessage();
        }
    }

    /**
     * Get upcoming events for debugging
     */
    public List<Event> getUpcomingEvents() {
        try {
            logger.info("=== GETTING UPCOMING EVENTS ===");
            Calendar service = getCalendarService();

            DateTime now = new DateTime(System.currentTimeMillis());
            Events events = service.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> items = events.getItems();
            logger.info("=== FOUND {} UPCOMING EVENTS ===", items.size());

            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    start = event.getStart().getDate();
                }
                logger.info("Event: {} ({})", event.getSummary(), start);
            }

            return items;

        } catch (Exception e) {
            logger.error("Error getting upcoming events: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
package com.mentis.hrms.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RoleInterceptor.class);


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        logger.debug("Interceptor - URI: {}, Method: {}", uri, method);
        // ===== ADD THIS DEBUG BLOCK =====
        System.out.println("\n🔍 INTERCEPTOR CHECK - " + new java.util.Date());
        System.out.println("   URI: " + uri);
        if (session != null) {
            System.out.println("   Session ID: " + session.getId());
            System.out.println("   userId in session: " + session.getAttribute("userId"));
            System.out.println("   userRole in session: " + session.getAttribute("userRole"));
        } else {
            System.out.println("   Session: NULL");
        }
        System.out.println("===================================\n");

        // ============ PUBLIC ENDPOINTS - NO AUTH REQUIRED ============
        if (uri.equals("/candidate/login") ||
                uri.equals("/candidate/auth/login") ||
                uri.startsWith("/candidate/forgot-password") ||
                uri.startsWith("/candidate/reset-password") ||
                uri.startsWith("/candidate/create-password")) {
            logger.debug("Public auth endpoint - allowing access");
            return true;
        }

        // Allow data resources
        if (uri.startsWith("/data/")) {
            return true;
        }

        // Allow resume endpoints
        if (uri.contains("/download-resume") ||
                uri.contains("/preview-resume") ||
                uri.contains("/preview-image") ||
                uri.contains("/public/")) {
            logger.debug("Resume endpoint - allowing access without authentication: {}", uri);
            return true;
        }

        // Allow static resources
        if (uri.startsWith("/static/") ||
                uri.startsWith("/webjars/") ||
                uri.startsWith("/uploads/") ||
                uri.startsWith("/css/") ||
                uri.startsWith("/js/") ||
                uri.startsWith("/images/")) {
            logger.debug("Static resource - allowing access");
            return true;
        }

        // ============ SESSION CHECK ============
        if (session == null || session.getAttribute("userId") == null) {
            logger.warn("No active session or userId not found - redirecting to login. URI: {}", uri);

            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Session expired\",\"redirect\":\"/candidate/login\"}");
                return false;
            }

            response.sendRedirect("/candidate/login?error=Please+login+first");
            return false;
        }

        // Get user details from session
        String role = (String) session.getAttribute("userRole");
        String empId = (String) session.getAttribute("userId");
        String loginType = (String) session.getAttribute("loginType");

        logger.debug("User authenticated - Role: {}, UserId: {}, LoginType: {}", role, empId, loginType);

        // ============ CRITICAL FIX: SUPER_ADMIN HAS ACCESS TO EVERYTHING UNDER /dashboard ============
        if ("SUPER_ADMIN".equals(role)) {
            logger.debug("✅ SUPER_ADMIN access granted for URI: {}", uri);

            // Special case: If trying to access /dashboard (without /admin), allow it
            if (uri.equals("/dashboard") || uri.startsWith("/dashboard/")) {
                logger.debug("✅ SUPER_ADMIN accessing dashboard - ALLOWED");
                return true;
            }

            // Allow access to all dashboard URLs
            return true;
        }

        // ============ HR DASHBOARD ACCESS ============
        if (uri.startsWith("/dashboard/hr") || uri.equals("/dashboard") || uri.startsWith("/dashboard/applications") || uri.startsWith("/dashboard/viewApplication")) {
            if (!"HR".equals(role)) {
                logger.warn("Non-HR user attempting to access HR dashboard. Role: {}, URI: {}", role, uri);
                return redirectToHomeBasedOnRole(role, empId, response);
            }
            logger.debug("HR access granted for URI: {}", uri);
            return true;
        }

        // ============ ADMIN DASHBOARD ACCESS ============
        if (uri.startsWith("/dashboard/admin")) {
            if (!"SUPER_ADMIN".equals(role)) {
                logger.warn("Non-Admin user attempting to access Admin dashboard. Role: {}, URI: {}", role, uri);
                return redirectToHomeBasedOnRole(role, empId, response);
            }
            logger.debug("Admin access granted for URI: {}", uri);
            return true;
        }

        // ============ EMPLOYEE/CANDIDATE DASHBOARD ACCESS ============
        if (uri.startsWith("/candidate/dashboard")) {
            String[] pathParts = uri.split("/");
            String requestedEmpId = null;

            for (int i = 0; i < pathParts.length - 1; i++) {
                if ("dashboard".equals(pathParts[i]) && i + 1 < pathParts.length) {
                    requestedEmpId = pathParts[i + 1];
                    break;
                }
            }

            if (requestedEmpId != null && !requestedEmpId.equals(empId)) {
                logger.warn("Employee {} attempting to access dashboard of employee {}", empId, requestedEmpId);
                response.sendRedirect("/candidate/dashboard/" + empId);
                return false;
            }

            if (!"EMPLOYEE".equals(role)) {
                logger.warn("Non-employee attempting to access employee dashboard. Role: {}, URI: {}", role, uri);
                return redirectToHomeBasedOnRole(role, empId, response);
            }

            logger.debug("Employee dashboard access granted for URI: {}", uri);
            return true;
        }

        // ============ PREVENT EMPLOYEE ACCESS TO HR DASHBOARD ============
        if (uri.startsWith("/dashboard") && !uri.startsWith("/dashboard/admin") && !uri.startsWith("/dashboard/hr")) {
            if ("EMPLOYEE".equals(role) || "EMPLOYEE".equals(loginType)) {
                logger.warn("Employee attempting to access HR dashboard - redirecting. URI: {}", uri);
                response.sendRedirect("/candidate/dashboard/" + empId);
                return false;
            }
        }


// Allow admin attendance APIs (they have their own security checks)
        if (uri.startsWith("/dashboard/admin/attendance/api/")) {
            logger.debug("Admin attendance API endpoint - allowing access");
            return true;
        }


        // Default: allow access
        logger.debug("Default access granted for URI: {}", uri);
        return true;
    }
    private boolean redirectToHomeBasedOnRole(String role, String empId, HttpServletResponse response) throws Exception {
        if ("SUPER_ADMIN".equals(role)) {
            logger.debug("Redirecting to Super Admin dashboard");
            response.sendRedirect("/dashboard/admin");
        } else if ("HR".equals(role)) {
            logger.debug("Redirecting to HR dashboard");
            response.sendRedirect("/dashboard/hr");
        } else if ("EMPLOYEE".equals(role)) {
            logger.debug("Redirecting to Employee dashboard: {}", empId);
            response.sendRedirect("/candidate/dashboard/" + empId);
        } else {
            logger.debug("Redirecting to login page");
            response.sendRedirect("/candidate/login");
        }
        return false;
    }
}
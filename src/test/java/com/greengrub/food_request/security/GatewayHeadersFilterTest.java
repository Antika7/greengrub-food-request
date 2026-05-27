package com.greengrub.food_request.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GatewayHeadersFilterTest {

    private GatewayHeadersFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() throws Exception {
        filter = new GatewayHeadersFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    void withUserId_setsAllRequestAttributes() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("user-001");
        when(request.getHeader("X-User-Email")).thenReturn("user@example.com");
        when(request.getHeader("X-User-Role")).thenReturn("CUSTOMER");

        filter.doFilter(request, response, chain);

        verify(request).setAttribute("userId", "user-001");
        verify(request).setAttribute("userEmail", "user@example.com");
        verify(request).setAttribute("userRole", "CUSTOMER");
        verify(chain).doFilter(request, response);
    }

    @Test
    void withoutUserId_doesNotSetAttributes() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(request, never()).setAttribute(eq("userId"), any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void blankUserId_doesNotSetAttributes() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("   ");

        filter.doFilter(request, response, chain);

        verify(request, never()).setAttribute(eq("userId"), any());
    }

    @Test
    void filterAlwaysChainsRegardlessOfHeaders() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}

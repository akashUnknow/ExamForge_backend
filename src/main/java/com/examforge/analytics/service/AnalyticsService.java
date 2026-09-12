package com.examforge.analytics.service;

import com.examforge.analytics.dto.PopularExamResponse;
import com.examforge.analytics.dto.PopularTestResponse;
import com.examforge.analytics.dto.RegistrationTrendPoint;
import com.examforge.analytics.dto.TestAnalyticsResponse;

import java.util.List;
import java.util.UUID;

public interface AnalyticsService {

    TestAnalyticsResponse getTestAnalytics(UUID testId);

    List<PopularTestResponse> getPopularTests(int limit);

    List<PopularExamResponse> getPopularExams(int limit);

    List<RegistrationTrendPoint> getRegistrationTrend(int days);
}

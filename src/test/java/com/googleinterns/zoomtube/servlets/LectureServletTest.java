// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googleinterns.zoomtube.servlets;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.googleinterns.zoomtube.data.Lecture;
import com.googleinterns.zoomtube.utils.LectureUtil;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public final class LectureServletTest {
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private final LocalServiceTestHelper testServices =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private DatastoreService datastoreService;
  private LectureServlet servlet;

  /* Writer where response is written. */
  private StringWriter content;

  private static final String TEST_NAME = "TestName";
  private static final String TEST_LINK = "https://www.youtube.com/watch?v=3ymwOvzhwHs";
  private static final String TEST_ID = "3ymwOvzhwHs";

  @Before
  public void setUp() throws ServletException, IOException {
    testServices.setUp();
    datastoreService = DatastoreServiceFactory.getDatastoreService();
    servlet = new LectureServlet();
    servlet.init();
    content = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(content));
  }

  @After
  public void tearDown() {
    testServices.tearDown();
  }

  @Test
  public void doPost_missingName_badRequest() throws IOException, ServletException {
    when(request.getParameter(LectureServlet.PARAM_LINK)).thenReturn(TEST_LINK);

    servlet.doPost(request, response);

    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing name parameter.");
  }

  @Test
  public void doPost_missingLink_badRequest() throws IOException, ServletException {
    when(request.getParameter(LectureServlet.PARAM_NAME)).thenReturn(TEST_NAME);

    servlet.doPost(request, response);

    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing link parameter.");
  }

  @Test
  public void doPost_invalidLink_badRequest() throws IOException, ServletException {
    when(request.getParameter(LectureServlet.PARAM_NAME)).thenReturn(TEST_NAME);
    when(request.getParameter(LectureServlet.PARAM_LINK)).thenReturn("this is not a link");

    servlet.doPost(request, response);

    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid video link.");
  }

  @Test
  public void doPost_urlAlreadyInDatabase_shouldReturnLecture()
      throws IOException, ServletException {
    when(request.getParameter(LectureServlet.PARAM_NAME)).thenReturn(TEST_NAME);
    when(request.getParameter(LectureServlet.PARAM_LINK)).thenReturn(TEST_LINK);
    datastoreService.put(LectureUtil.createEntity(TEST_NAME, TEST_LINK, TEST_ID));

    servlet.doPost(request, response);

    assertThat(datastoreService.prepare(new Query(LectureUtil.KIND)).countEntities()).isEqualTo(1);
    verify(response).sendRedirect("/view?id=1");
  }

  @Test
  public void doPost_urlNotInDatabase_shouldAddToDatabaseAndReturnRedirect()
      throws IOException, ServletException {
    when(request.getParameter(LectureServlet.PARAM_LINK)).thenReturn(TEST_LINK);
    when(request.getParameter(LectureServlet.PARAM_NAME)).thenReturn(TEST_NAME);

    // No lecture in datastoreService.
    servlet.doPost(request, response);

    assertThat(datastoreService.prepare(new Query(LectureUtil.KIND)).countEntities()).isEqualTo(1);
    verify(response).sendRedirect("/view?id=1");
  }

  @Test
  public void doGet_missingLecture_badRequest() throws IOException {
    servlet.doGet(request, response);

    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id parameter.");
  }

  @Test
  public void doGet_lectureInDatabase_shouldWriteLecture() throws IOException {
    Entity lectureEntity =
        LectureUtil.createEntity(/* lectureName= */ TEST_NAME, TEST_LINK, TEST_ID);
    datastoreService.put(lectureEntity);
    Long entityId = lectureEntity.getKey().getId();
    when(request.getParameter(LectureUtil.ID)).thenReturn(Long.toString(entityId));

    servlet.doGet(request, response);

    String json = content.toString();
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY).create();
    Lecture lecture = gson.fromJson(json, Lecture.class);
    assertThat(lecture.key().getId()).isEqualTo(entityId);
    assertThat(lecture.lectureName()).isEqualTo(/* lectureName= */ TEST_NAME);
    assertThat(lecture.videoUrl()).isEqualTo(TEST_LINK);
    assertThat(lecture.videoId()).isEqualTo(TEST_ID);
  }

  @Test
  public void doGet_noLectureInDatabase_shouldWriteNoLecture() throws IOException {
    when(request.getParameter(LectureUtil.ID)).thenReturn(/* lectureId= */ "1");

    servlet.doGet(request, response);

    String json = content.toString();
    assertThat(json).isEmpty();
    verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Lecture not found in database.");
  }

  @Test
  public void getVideoId_shouldFindAllIds() {
    String video1 = "http://www.youtube.com/watch?v=dQw4w9WgXcQ&a=GxdCwVVULXctT2lYDEPllDR0LRTutYfW";
    String video2 = "http://www.youtube.com/watch?v=dQw4w9WgXcQ";
    String video3 = "http://youtu.be/dQw4w9WgXcQ";
    String video4 = "http://www.youtube.com/embed/dQw4w9WgXcQ";
    String video5 = "http://www.youtube.com/v/dQw4w9WgXcQ";
    String video6 = "http://www.youtube.com/watch?v=dQw4w9WgXcQ";
    String video7 = "http://www.youtube.com/watch?feature=player_embedded&v=dQw4w9WgXcQ";
    String video8 = "http://www.youtube-nocookie.com/v/dQw4w9WgXcQ?version=3&hl=en_US&rel=0";
    String id = "dQw4w9WgXcQ";

    assertThat(servlet.getVideoId(video1).get()).isEqualTo(id);
    assertThat(servlet.getVideoId(video2).get()).isEqualTo(id);
    assertThat(servlet.getVideoId(video3).get()).isEqualTo(id);
    assertThat(servlet.getVideoId(video4).get()).isEqualTo(id);
    assertThat(servlet.getVideoId(video5).get()).isEqualTo(id);
    assertThat(servlet.getVideoId(video6).get()).isEqualTo(id);
    assertThat(servlet.getVideoId(video7).get()).isEqualTo(id);
    assertThat(servlet.getVideoId(video8).get()).isEqualTo(id);
  }
}

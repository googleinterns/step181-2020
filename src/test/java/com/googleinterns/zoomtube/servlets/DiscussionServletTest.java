package com.googleinterns.zoomtube.servlets;

import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.User;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalUserServiceTestConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.googleinterns.zoomtube.data.Comment;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class DiscussionServletTest {
  private static final int LECTURE_A = 1;
  private static final int LECTURE_B = 2;
  private static final int LECTURE_C = 3;
  private static final String LECTURE_A_STR = "1";
  private static final String LECTURE_B_STR = "2";
  private static final String LECTURE_C_STR = "3";
  private static final int PARENT_ID = 32;
  private static final String PARENT_ID_STR = "32";
  private static final String CONTENT = "Test content";
  private static final String EMAIL = "test@example.com";
  private static final String AUTH_DOMAIN = "example.com";

  @Rule public final MockitoRule mockito = MockitoJUnit.rule();
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  private DiscussionServlet servlet;
  private LocalServiceTestHelper testServices = new LocalServiceTestHelper(
      new LocalUserServiceTestConfig(), new LocalDatastoreServiceTestConfig());
  private DatastoreService datastore;

  @Before
  public void setUp() throws ServletException {
    testServices.setUp();
    testServices.setEnvEmail(EMAIL);
    testServices.setEnvAuthDomain(AUTH_DOMAIN);
    servlet = new DiscussionServlet();
    servlet.init();
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @After
  public void cleanUp() {
    testServices.tearDown();
  }

  @Test
  public void doPost_loggedOut_postForbidden() throws ServletException, IOException {
    testServices.setEnvIsLoggedIn(false);
    when(request.getParameter(DiscussionServlet.PARAM_LECTURE)).thenReturn(LECTURE_A_STR);

    servlet.doPost(request, response);

    verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    assertThat(datastore.prepare(new Query(Comment.ENTITY_KIND)).countEntities(withLimit(1)))
        .isEqualTo(0);
  }

  @Test
  public void doPost_storesCommentWithProperties_noParent() throws ServletException, IOException {
    testServices.setEnvIsLoggedIn(true);
    when(request.getParameter(DiscussionServlet.PARAM_LECTURE)).thenReturn(LECTURE_A_STR);
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(CONTENT)));

    servlet.doPost(request, response);

    verify(response).setStatus(HttpServletResponse.SC_ACCEPTED);
    PreparedQuery query = datastore.prepare(new Query(Comment.ENTITY_KIND));
    assertThat(query.countEntities(withLimit(2))).isEqualTo(1);
    Comment comment = Comment.fromEntity(query.asSingleEntity());
    assertThat(comment.lecture().getId()).isEqualTo(LECTURE_A);
    assertThat(comment.author().getEmail()).isEqualTo(EMAIL);
    assertThat(comment.content()).isEqualTo(CONTENT);
    assertThat(comment.parent().isPresent()).isFalse();
  }

  @Test
  public void doPost_storesCommentParent() throws ServletException, IOException {
    testServices.setEnvIsLoggedIn(true);
    when(request.getParameter(DiscussionServlet.PARAM_LECTURE)).thenReturn(LECTURE_A_STR);
    when(request.getParameter(DiscussionServlet.PARAM_PARENT)).thenReturn(PARENT_ID_STR);
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(CONTENT)));

    servlet.doPost(request, response);

    verify(response).setStatus(HttpServletResponse.SC_ACCEPTED);
    PreparedQuery query = datastore.prepare(new Query(Comment.ENTITY_KIND));
    Comment comment = Comment.fromEntity(query.asSingleEntity());
    assertThat(comment.parent().isPresent()).isTrue();
    assertThat(comment.parent().get().getId()).isEqualTo(PARENT_ID);
  }

  @Test
  public void doGet_returnsNothingForUnknownLecture() throws ServletException, IOException {
    when(request.getParameter(DiscussionServlet.PARAM_LECTURE)).thenReturn(LECTURE_A_STR);
    StringWriter content = new StringWriter();
    PrintWriter writer = new PrintWriter(content);
    when(response.getWriter()).thenReturn(writer);

    servlet.doGet(request, response);

    verify(response).setContentType("application/json;");
    List<Comment> comments = getCommentsFromJson(content.toString());
    assertThat(comments.isEmpty()).isTrue();
  }

  @Test
  public void doGet_returnsComments() throws ServletException, IOException {
    when(request.getParameter(DiscussionServlet.PARAM_LECTURE)).thenReturn(LECTURE_A_STR);
    datastore.put(createTestCommentEntity(LECTURE_A));
    datastore.put(createTestCommentEntity(LECTURE_A));
    datastore.put(createTestCommentEntity(LECTURE_A));
    StringWriter content = new StringWriter();
    PrintWriter writer = new PrintWriter(content);
    when(response.getWriter()).thenReturn(writer);

    servlet.doGet(request, response);

    verify(response).setContentType("application/json;");
    List<Comment> comments = getCommentsFromJson(content.toString());
    assertThat(comments.size()).isEqualTo(3);
  }

  @Test
  public void doGet_returnsCommentsForSpecificLecture() throws ServletException, IOException {
    // Looking for two comments under LECTURE_B.
    when(request.getParameter(DiscussionServlet.PARAM_LECTURE)).thenReturn(LECTURE_B_STR);
    datastore.put(createTestCommentEntity(LECTURE_A));
    datastore.put(createTestCommentEntity(LECTURE_C));
    datastore.put(createTestCommentEntity(LECTURE_B)); // Add the first.
    datastore.put(createTestCommentEntity(LECTURE_A));
    datastore.put(createTestCommentEntity(LECTURE_A));
    datastore.put(createTestCommentEntity(LECTURE_C));
    datastore.put(createTestCommentEntity(LECTURE_B)); // Add the second.
    datastore.put(createTestCommentEntity(LECTURE_C));
    StringWriter content = new StringWriter();
    PrintWriter writer = new PrintWriter(content);
    when(response.getWriter()).thenReturn(writer);

    servlet.doGet(request, response);

    verify(response).setContentType("application/json;");
    List<Comment> comments = getCommentsFromJson(content.toString());
    assertThat(comments.size()).isEqualTo(2);
  }

  private Entity createTestCommentEntity(int lectureId) {
    Entity commentEntity = new Entity(Comment.ENTITY_KIND);
    Key lecture = KeyFactory.createKey(/* kind= */ "Lecture", lectureId);
    commentEntity.setProperty(Comment.PROP_LECTURE, lecture);
    commentEntity.setProperty(Comment.PROP_PARENT, null);
    commentEntity.setProperty(Comment.PROP_TIMESTAMP, new Date(0));
    commentEntity.setProperty(Comment.PROP_AUTHOR, new User("test@example.com", "example.com"));
    commentEntity.setProperty(Comment.PROP_CONTENT, "Test Content");
    commentEntity.setProperty(Comment.PROP_CREATED, new Date(Clock.systemUTC().millis()));

    return commentEntity;
  }

  private List<Comment> getCommentsFromJson(String json) {
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY).create();
    Type listType = new TypeToken<ArrayList<Comment>>() {}.getType();
    List<Comment> comments = gson.fromJson(json, listType);
    return comments;
  }
}

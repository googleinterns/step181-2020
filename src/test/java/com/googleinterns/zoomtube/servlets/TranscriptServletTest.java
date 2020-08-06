// Copyright 2019 Google LLC
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

import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googleinterns.zoomtube.data.TranscriptLine;
import com.googleinterns.zoomtube.utils.LectureUtil;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public final class TranscriptServletTest {
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private TranscriptServlet transcriptServlet;
  private DatastoreService datastore;
  private StringWriter lectureTranscript;

  private static final LocalDatastoreServiceTestConfig datastoreConfig =
      (new LocalDatastoreServiceTestConfig()).setNoStorage(true);
  private static final LocalServiceTestHelper localServiceHelper =
      new LocalServiceTestHelper(datastoreConfig);
  private static final String LECTURE_ID_A = "123";
  private static final String LECTURE_ID_B = "345";
  private static final String LECTURE_ID_C = "234";
  private static final String SHORT_VIDEO_ID = "Obgnr9pc820";
  private static final String LONG_VIDEO_ID = "jNQXAC9IVRw";
  // TODO: Find a way to reprsent this differently.
  private static final String SHORT_VIDEO_JSON =
      "[{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":1},"
      + "\"lectureKey\":{\"kind\":\"Lecture\",\"id\":123},"
      + "\"start\":\"0.4\",\"duration\":\"1\",\"content\":\" \"},"
      + "{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":2},\"lectureKey\":"
      + "{\"kind\":\"Lecture\",\"id\":123},\"start\":\"2.28\",\"duration\":\"1\",\"content\""
      + ":\"Hi\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":3},\"lectureKey\":"
      + "{\"kind\":\"Lecture\",\"id\":123},\"start\":\"5.04\",\"duration\":\"1.6\","
      + "\"content\":\"Okay\"}]";
  private static final String LONG_VIDEO_JSON =
      "[{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":1},\"lectureKey\":"
      + "{\"kind\":\"Lecture\",\"id\":123},\"start\":\"1.3\",\"duration\":"
      + "\"3.1\",\"content\":\"All right, so here we are\\nin front of the "
      + "elephants,\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":4},\"lectureKey\":"
      + "{\"kind\":\"Lecture\",\"id\":123},\"start\":\"12.7\",\"duration\":\"4.3\",\"content\":"
      + "\"and that&#39;s, that&#39;s cool.\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":5},"
      + "\"lectureKey\":{\"kind\":\"Lecture\",\"id\":123},\"start\":\"17\",\"duration\":\"1.767\",\""
      + "content\":\"And that&#39;s pretty much all there is to say.\"},{\"transcriptKey\":{\"kind\""
      + ":\"TranscriptLine\",\"id\":2},\"lectureKey\":{\"kind\":\"Lecture\",\"id\":123},\"start\":"
      + "\"4.4\",\"duration\":\"4.766\",\"content\":\"the cool thing about these guys\\nis that "
      + "they have really,\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":3},\"lectureKey\":"
      + "{\"kind\":\"Lecture\",\"id\":123},\"start\":\"9.166\",\"duration\":\"3.534\","
      + "\"content\":\"really, really long trunks,\"}]";

  private static List<TranscriptLine> shortVideoTranscriptLines;
  private static List<TranscriptLine> longVideoTranscriptLines;

  @BeforeClass
  public static void createTranscriptLineLists() {
    shortVideoTranscriptLines = transcriptLines(SHORT_VIDEO_JSON);
    longVideoTranscriptLines = transcriptLines(LONG_VIDEO_JSON);
  }

  @Before
  public void setUp() throws ServletException, IOException {
    localServiceHelper.setUp();
    transcriptServlet = new TranscriptServlet();
    datastore = DatastoreServiceFactory.getDatastoreService();
    transcriptServlet.init(datastore);
    lectureTranscript = new StringWriter();
    PrintWriter writer = new PrintWriter(lectureTranscript);
    when(response.getWriter()).thenReturn(writer);
  }

  @After
  public void tearDown() {
    localServiceHelper.tearDown();
  }

  @Test
  public void doGet_getDataInDatastoreForShortVideo() throws ServletException, IOException {
    putTranscriptLinesInDatastore(shortVideoTranscriptLines, LECTURE_ID_A);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_A);

    transcriptServlet.doGet(request, response);

    List<TranscriptLine> expectedTranscriptLines = shortVideoTranscriptLines;
    List<TranscriptLine> actualTranscriptLines = transcriptLines(lectureTranscript.toString());
    assertThat(actualTranscriptLines.size()).isEqualTo(expectedTranscriptLines.size());
  }

  @Test
  public void doPost_persistDataInDatastoreForShortVideo() throws ServletException, IOException {
    when(request.getParameter(LectureUtil.VIDEO_ID)).thenReturn(SHORT_VIDEO_ID);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_B);

    transcriptServlet.doPost(request, response);

    int actualQuery = entitiesInDatastoreCount(LECTURE_ID_B);
    int expectedQuery = (shortVideoTranscriptLines).size();
    assertThat(actualQuery).isEqualTo(expectedQuery);
  }

  @Test
  public void doGet_doPost_storeAndRetrieveShortVideo() throws ServletException, IOException {
    when(request.getParameter(LectureUtil.VIDEO_ID)).thenReturn(SHORT_VIDEO_ID);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_A);

    transcriptServlet.doPost(request, response);
    transcriptServlet.doGet(request, response);

    List<TranscriptLine> expectedTranscriptLines = shortVideoTranscriptLines;
    List<TranscriptLine> actualTranscriptLines = transcriptLines(lectureTranscript.toString());
    assertThat(actualTranscriptLines).isEqualTo(expectedTranscriptLines);
  }

  @Test
  public void doGet_doPost_storeAndRetrieveLongVideo() throws ServletException, IOException {
    when(request.getParameter(LectureUtil.VIDEO_ID)).thenReturn(LONG_VIDEO_ID);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_A);

    transcriptServlet.doPost(request, response);
    transcriptServlet.doGet(request, response);

    List<TranscriptLine> expectedTranscriptLines = longVideoTranscriptLines;
    List<TranscriptLine> actualTranscriptLines = transcriptLines(lectureTranscript.toString());
    assertThat(actualTranscriptLines).isEqualTo(expectedTranscriptLines);
  }

  @Test
  public void doGet_returnsLectureForLongVideoFromDatastore() throws ServletException, IOException {
    putTranscriptLinesInDatastore(longVideoTranscriptLines, LECTURE_ID_A);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_A);

    transcriptServlet.doGet(request, response);

    List<TranscriptLine> expectedTranscriptLines = longVideoTranscriptLines;
    List<TranscriptLine> actualTranscriptLines = transcriptLines(lectureTranscript.toString());
    assertThat(actualTranscriptLines.size()).isEqualTo(expectedTranscriptLines.size());
  }

  @Test
  public void doPost_persistDataInDatastoreForLongVideo() throws ServletException, IOException {
    when(request.getParameter(LectureUtil.VIDEO_ID)).thenReturn(LONG_VIDEO_ID);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_C);

    transcriptServlet.doPost(request, response);

    int actualQueryCount = entitiesInDatastoreCount(LECTURE_ID_C);
    int expectedQueryCount = (transcriptLines(LONG_VIDEO_JSON)).size();
    assertThat(actualQueryCount).isEqualTo(expectedQueryCount);
  }

  @Test
  public void doGet_onlyOtherLecturesInDatastore_GetNoLectures()
      throws ServletException, IOException {
    putTranscriptLinesInDatastore(shortVideoTranscriptLines, LECTURE_ID_B);
    putTranscriptLinesInDatastore(longVideoTranscriptLines, LECTURE_ID_A);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_C);

    transcriptServlet.doGet(request, response);

    List<TranscriptLine> actualTranscriptLines = transcriptLines(lectureTranscript.toString());
    assertThat(actualTranscriptLines.size()).isEqualTo(0);
  }

  @Test
  public void doGet_twoLecturesInDatastore_returnsOneLecture()
      throws ServletException, IOException {
    putTranscriptLinesInDatastore(shortVideoTranscriptLines, LECTURE_ID_B);
    putTranscriptLinesInDatastore(longVideoTranscriptLines, LECTURE_ID_A);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_A);

    transcriptServlet.doGet(request, response);

    List<TranscriptLine> expectedTranscriptLines = longVideoTranscriptLines;
    List<TranscriptLine> actualTranscriptLines = transcriptLines(lectureTranscript.toString());
    assertThat(actualTranscriptLines.size()).isEqualTo(expectedTranscriptLines.size());
  }

  private static List<TranscriptLine> transcriptLines(String transcriptLinesJson) {
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY).create();
    return (ArrayList<TranscriptLine>) gson.fromJson(
        transcriptLinesJson, (new ArrayList<List<TranscriptLine>>().getClass()));
  }

  private void putTranscriptLinesInDatastore(
      List<TranscriptLine> transcriptLines, String lectureId) {
    Key lectureKey = KeyFactory.createKey(LectureUtil.KIND, Long.parseLong(lectureId));
    for (int i = 0; i < transcriptLines.size(); i++) {
      Entity lineEntity = new Entity(TranscriptLine.ENTITY_KIND);
      lineEntity.setProperty(TranscriptLine.PROP_LECTURE, lectureKey);
      // Set dummy values because AutoValue needs all the values to create a TranscriptLine object.
      lineEntity.setProperty(TranscriptLine.PROP_START, /* start= */ "");
      lineEntity.setProperty(TranscriptLine.PROP_DURATION, /* duration= */ "");
      lineEntity.setProperty(TranscriptLine.PROP_CONTENT, /* content= */ "");
      datastore.put(lineEntity);
    }
  }

  private int entitiesInDatastoreCount(String lectureId) {
    // A limit of 100 for the maximum number of entities counted is used because
    // we can assume that for this test datastore, there won't be more than 100 entities
    // for a lecture key.
    return datastore.prepare(filteredQueryOfTranscriptLinesByLectureId(lectureId))
        .countEntities(withLimit(100));
  }

  private Query filteredQueryOfTranscriptLinesByLectureId(String lectureId) {
    Key lectureKey = KeyFactory.createKey(LectureUtil.KIND, Long.parseLong(lectureId));
    Filter lectureKeyFilter =
        new FilterPredicate(TranscriptLine.PROP_LECTURE, FilterOperator.EQUAL, lectureKey);
    return new Query(TranscriptLine.ENTITY_KIND).setFilter(lectureKeyFilter);
  }
}

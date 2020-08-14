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

package com.googleinterns.zoomtube.transcriptParser;

import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
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
import com.googleinterns.zoomtube.utils.TranscriptLineUtil;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
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
public final class TranscriptParserTest {
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private DatastoreService datastore;
  private StringWriter lectureTranscript;

  private static final LocalDatastoreServiceTestConfig datastoreConfig =
      (new LocalDatastoreServiceTestConfig()).setNoStorage(true);
  private static final LocalServiceTestHelper localServiceHelper =
      new LocalServiceTestHelper(datastoreConfig);
  private static final String LECTURE_ID_B = "345";
  private static final String LECTURE_ID_C = "234";
  private static final String SHORT_VIDEO_ID = "Obgnr9pc820";
  private static final String LONG_VIDEO_ID = "jNQXAC9IVRw";
  // TODO: Find a way to reprsent this differently.
  private static final String SHORT_VIDEO_JSON =
      "[{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":"
      + "1},\"lectureKey\":{\"kind\":\"Lecture\",\"id\":123},\"start\":\"Jan 1, 1970 12:00:00 AM\","
      + "\"duration\":\"Jan 1, 1970 12:00:01 AM\",\"end\":\"Jan 1, 1970 12:00:01 AM\",\"content\":"
      + "\" \"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":2},\"lectureKey\":{\"kind\":\"Lecture\","
      + "\"id\":123},\"start\":\"Jan 1, 1970 12:00:02 AM\",\"duration\":\"Jan 1, 1970 12:00:01 AM\","
      + "\"end\":\"Jan 1, 1970 12:00:03 AM\",\"content\":\"Hi\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\","
      + "\"id\":3},\"lectureKey\":{\"kind\":\"Lecture\",\"id\":123},\"start\":\"Jan 1, 1970 12:00:05 AM\","
      + "\"duration\":\"Jan 1, 1970 12:00:01 AM\",\"end\":\"Jan 1, 1970 12:00:06 AM\",\"content\":\"Okay\"}]";
  private static final String LONG_VIDEO_JSON =
      "[{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":1}"
      + ",\"lectureKey\":{\"kind\":\"Lecture\",\"id\":123},\"start\":\"Jan 1, 1970 12:00:01 AM\","
      + "\"duration\":\"Jan 1, 1970 12:00:03 AM\",\"end\":\"Jan 1, 1970 12:00:04 AM\",\"content\""
      + ":\"All right, so here we are\\nin front of the elephants,\"},{\"transcriptKey\":{\"kind\":"
      + "\"TranscriptLine\",\"id\":2},\"lectureKey\":{\"kind\":\"Lecture\",\"id\":123}"
      + ",\"start\":\"Jan 1, 1970 12:00:04 AM\",\"duration\":\"Jan 1, 1970 12:00:04 AM\","
      + "\"end\":\"Jan 1, 1970 12:00:09 AM\",\"content\":\"the cool thing about these "
      + "guys\\nis that they have really,\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":3},"
      + "\"lectureKey\":{\"kind\":\"Lecture\",\"id\":123},\"start\":\"Jan 1, 1970 12:00:09 AM\","
      + "\"duration\":\"Jan 1, 1970 12:00:03 AM\",\"end\":\"Jan 1, 1970 12:00:12 AM\",\"content\":"
      + "\"really, really long trunks,\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":4},\"lectureKey\""
      + ":{\"kind\":\"Lecture\",\"id\":123},\"start\":\"Jan 1, 1970 12:00:12 AM\",\"duration\":\"Jan "
      + "1, 1970 12:00:04 AM\",\"end\":\"Jan 1, 1970 12:00:17 AM\",\"content\":\"and that&#39;s, "
      + "that&#39;s cool.\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":5},\"lectureKey\""
      + ":{\"kind\":\"Lecture\",\"id\":123},\"start\":\"Jan 1, 1970 12:00:17 AM\",\"duration\":"
      + "\"Jan 1, 1970 12:00:01 AM\",\"end\":\"Jan 1, 1970 12:00:18 AM\",\"content\""
      + ":\"And that&#39;s pretty much all there is to say.\"}]";

  private static List<TranscriptLine> shortVideoTranscriptLines;
  private static List<TranscriptLine> longVideoTranscriptLines;

  @BeforeClass
  public static void createTranscriptLineLists() {
    shortVideoTranscriptLines = transcriptLines(SHORT_VIDEO_JSON);
    longVideoTranscriptLines = transcriptLines(LONG_VIDEO_JSON);
  }

  @Before
  public void setUp() throws IOException {
    localServiceHelper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();
    lectureTranscript = new StringWriter();
    PrintWriter writer = new PrintWriter(lectureTranscript);
    when(response.getWriter()).thenReturn(writer);
  }

  @After
  public void tearDown() {
    localServiceHelper.tearDown();
  }

  @Test
  public void parseAndStoreTranscript_persistDataInDatastoreForShortVideo() throws IOException {
    when(request.getParameter(LectureUtil.VIDEO_ID)).thenReturn(SHORT_VIDEO_ID);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_B);
    Key lectureKeyB = KeyFactory.createKey(LectureUtil.KIND, Long.parseLong(LECTURE_ID_B));

    TranscriptParser.getParser().parseAndStoreTranscript(SHORT_VIDEO_ID, lectureKeyB);

    int actualQueryCount = entitiesInDatastoreCount(lectureKeyB);
    int expectedQueryCount = (shortVideoTranscriptLines).size();
    assertThat(actualQueryCount).isEqualTo(expectedQueryCount);
  }

  @Test
  public void parseAndStoreTranscript_persistDataInDatastoreForLongVideo() throws IOException {
    when(request.getParameter(LectureUtil.VIDEO_ID)).thenReturn(LONG_VIDEO_ID);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_C);
    Key lectureKeyC = KeyFactory.createKey(LectureUtil.KIND, Long.parseLong(LECTURE_ID_C));

    TranscriptParser.getParser().parseAndStoreTranscript(LONG_VIDEO_ID, lectureKeyC);

    int actualQueryCount = entitiesInDatastoreCount(lectureKeyC);
    int expectedQueryCount = (longVideoTranscriptLines).size();
    assertThat(actualQueryCount).isEqualTo(expectedQueryCount);
  }

  private static List<TranscriptLine> transcriptLines(String transcriptLinesJson) {
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY).create();
    return (ArrayList<TranscriptLine>) gson.fromJson(
        transcriptLinesJson, (new ArrayList<List<TranscriptLine>>().getClass()));
  }

  private int entitiesInDatastoreCount(Key lectureKey) {
    // A limit of 100 for the maximum number of entities counted is used because
    // we can assume that for this test datastore, there won't be more than 100 entities
    // for a lecture key.
    return datastore.prepare(filteredQueryOfTranscriptLinesByLectureId(lectureKey))
        .countEntities(withLimit(100));
  }

  private Query filteredQueryOfTranscriptLinesByLectureId(Key lectureKey) {
    Filter lectureKeyFilter =
        new FilterPredicate(TranscriptLineUtil.LECTURE, FilterOperator.EQUAL, lectureKey);
    return new Query(TranscriptLineUtil.KIND).setFilter(lectureKeyFilter);
  }
}

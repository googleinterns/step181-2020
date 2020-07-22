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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.googleinterns.zoomtube.data.TranscriptLine;
import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Provides the transcript for a given lecture.
 */
@WebServlet("/transcript")
public class TranscriptServlet extends HttpServlet {
  private static final String TRANSCRIPT_XML_URL_TEMPLATE =
      "http://video.google.com/timedtext?lang=en&v=";
  private static final String START_ATTRIBUTE = "start";
  private static final String DURATION_ATTRIBUTE = "dur";
  private static final String TEXT_TAG = "text";
  private static final String PARAM_LECTURE = "lecture";
  private static final String PARAM_LECTURE_ID = "id";
  private static final String PARAM_VIDEO_ID = "video";

  private static DatastoreService datastore;

  @Override
  public void init() throws ServletException {
    this.datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    long lectureId = Long.parseLong(request.getParameter(PARAM_LECTURE_ID));
    String videoId = request.getParameter(PARAM_VIDEO_ID);
    String transcriptXMLUrl = TRANSCRIPT_XML_URL_TEMPLATE + videoId;
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final DocumentBuilder db;
    final Document doc;

    try {
      db = dbf.newDocumentBuilder();
      doc = db.parse(new URL(transcriptXMLUrl).openStream());
      doc.getDocumentElement().normalize();
    } catch (ParserConfigurationException | SAXException e) {
      // TODO: alert the user.
      System.out.println("XML parsing error");
      return;
    }

    NodeList nodeList = doc.getElementsByTagName(TEXT_TAG);
    for (int nodeIndex = 0; nodeIndex < nodeList.getLength(); nodeIndex++) {
      Node node = nodeList.item(nodeIndex);
      Element element = (Element) node;
      String lineStart = element.getAttribute(START_ATTRIBUTE);
      String lineDuration = element.getAttribute(DURATION_ATTRIBUTE);
      String lineContent = node.getTextContent();
      // TODO: Remove print statement. It is currently here for display purposes.
      System.out.println(lineStart + " " + lineDuration + " "
          + " " + lineContent);
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    long lectureId = Long.parseLong(request.getParameter(PARAM_LECTURE_ID));
    Key lecture = KeyFactory.createKey(PARAM_LECTURE, lectureId);

    Filter lectureFilter =
        new FilterPredicate(TranscriptLine.PROP_LECTURE, FilterOperator.EQUAL, lecture);

    Query query = new Query(TranscriptLine.ENTITY_KIND)
                      .setFilter(lectureFilter)
                      .addSort(TranscriptLine.PROP_START, SortDirection.ASCENDING);
    PreparedQuery pq = datastore.prepare(query);

    ImmutableList.Builder<TranscriptLine> lineBuilder = new ImmutableList.Builder<>();
    for (Entity entity : pq.asQueryResultIterable()) {
      lineBuilder.add(TranscriptLine.fromEntity(entity));
    }
    ImmutableList<TranscriptLine> lines = lineBuilder.build();

    Gson gson = new Gson();
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(lines));
  }

  /**
   * Creates a line entity using the attributes from {@code node} and {@code lectureId}.
   */
  private Entity createLineEntity(Node node, long lectureId) {
    Element element = (Element) node;
    String lineContent = node.getTextContent();
    String lineStart = element.getAttribute(START_ATTRIBUTE);
    String lineDuration = element.getAttribute(DURATION_ATTRIBUTE);

    Entity lineEntity = new Entity(TranscriptLine.ENTITY_KIND);
    lineEntity.setProperty(
        TranscriptLine.PROP_LECTURE, KeyFactory.createKey(PARAM_LECTURE, lectureId));
    lineEntity.setProperty(TranscriptLine.PROP_CONTENT, lineContent);
    lineEntity.setProperty(TranscriptLine.PROP_START, lineStart);
    lineEntity.setProperty(TranscriptLine.PROP_DURATION, lineDuration);
    return lineEntity;
  }
}

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
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
  private static final String ATTRIBUTE_START = "start";
  private static final String ATTRIBUTE_DURATION = "dur";
  private static final String TEXT_TAG = "text";
  private static final String PARAM_LECTURE = "lecture";
  private static final String PARAM_LECTURE_ID = "id";
  private static final String PARAM_VIDEO_ID = "video";

  private static DatastoreService datastore;

  @Override
  public void init() throws ServletException {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // TODO: Decompose method. (getting parameters, getting doc to parse, adding to datastore)
    long lectureId = Long.parseLong(request.getParameter(PARAM_LECTURE_ID));
    String videoId = request.getParameter(PARAM_VIDEO_ID);
    String transcriptXMLUrl = TRANSCRIPT_XML_URL_TEMPLATE + videoId;

    Document document = getXmlAsDocument(transcriptXMLUrl).get();

    NodeList nodeList = document.getElementsByTagName(TEXT_TAG);
    for (int nodeIndex = 0; nodeIndex < nodeList.getLength(); nodeIndex++) {
      Node node = nodeList.item(nodeIndex);
      datastore.put(createLineEntity(node, lectureId));
    }
  }

  // private Document getXmlAsDocument(aram string)
  // private void put in datstore (param tag)

  private Optional<Document> getXmlAsDocument(String xmlUrl) throws IOException {
    final Document document;
    try {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      document = documentBuilder.parse(new URL(xmlUrl).openStream());
      document.getDocumentElement().normalize();
    } catch (ParserConfigurationException | SAXException e) {
      // TODO: Alert the user.
      System.out.println("XML parsing error");
      return Optional.empty();
    }
    return Optional.of(document);
  }

  private void putInDatastore()

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // TODO: Decompose method.
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
      lineBuilder.add(TranscriptLine.fromLineEntity(entity));
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
    String lineStart = element.getAttribute(ATTRIBUTE_START);
    String lineDuration = element.getAttribute(ATTRIBUTE_DURATION);

    Entity lineEntity = new Entity(TranscriptLine.ENTITY_KIND);
    // TODO: Change PARAM_LECTURE to Lecture.ENTITY_KIND once lectureServlet is
    // merged to this branch.
    lineEntity.setProperty(
        TranscriptLine.PROP_LECTURE, KeyFactory.createKey(PARAM_LECTURE, lectureId));
    lineEntity.setProperty(TranscriptLine.PROP_CONTENT, lineContent);
    lineEntity.setProperty(TranscriptLine.PROP_START, lineStart);
    lineEntity.setProperty(TranscriptLine.PROP_DURATION, lineDuration);
    return lineEntity;
  }
}

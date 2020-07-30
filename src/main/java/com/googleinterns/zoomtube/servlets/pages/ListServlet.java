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

package com.googleinterns.zoomtube.servlets.pages;

import javax.servlet.annotation.WebServlet;

/**
 * Serves the lecture-list page to logged in users.
 */
@WebServlet("/list/*")
public class ListServlet extends PageServlet {
  private static final String PAGE = "lecture-list.html";
  private static final String REDIRECT_URL = "/";
  public ListServlet() {
    super(PAGE, REDIRECT_URL);
  }
}